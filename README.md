# Account-service

Implementação do microserviço **Account** da store (contrato em `store:account`).

## Rodando localmente

```bash
mvn clean package
cp .env.example .env   # e preencha (ver abaixo)
docker compose up --build
```

## Primeiro ADMIN (seed)

Toda conta criada por `POST /accounts` nasce `USER`. O primeiro `ADMIN` é criado pela
migration `V2026.11.09.003__seed_admin.sql`, com email e hash vindos das variáveis
`ACCOUNT_ADMIN_EMAIL` e `ACCOUNT_ADMIN_HASH`. Se alguma estiver vazia, o serviço não sobe.

O hash precisa estar no mesmo formato do `calcHash` (Base64 de SHA-256 sobre a senha em UTF-8):

```bash
printf '%s' 'suaSenha' | openssl dgst -sha256 -binary | base64
```

Use `printf '%s'` (e não `echo`), para não incluir a quebra de linha no hash.

## Autorização (headers do gateway)

O gateway valida o JWT e injeta `id-account` e `id-role` (`USER`/`ADMIN`) em toda rota
protegida, apagando os que o cliente tenta enviar. O Account usa esses headers assim:

| Endpoint | Com `id-account` (via gateway) | Sem `id-account` (interno: Auth) |
|---|---|---|
| `POST /accounts`, `POST /accounts/login` | 403 | permitido |
| `GET /accounts` | só `ADMIN` | permitido |
| `GET /accounts/{id}`, `DELETE /accounts/{id}` | dono ou `ADMIN` | permitido |
| `GET /accounts/health-check` | permitido | permitido |

Negado = **403**. `id-account` sem `id-role` conta como `USER`.

**Premissa de segurança:** "sem header = chamada interna" só é seguro porque apenas o
gateway publica porta. O `compose.yaml` deste repo publica a 8080 só para testes locais;
em qualquer ambiente compartilhado o Account deve ficar na rede interna.

## Kubernetes (Minikube)

Manifestos em `k8s/k8s.yaml` (Deployment + Service `ClusterIP` 8080, nome `account`).
Pré-requisito: o Postgres do repo root já aplicado (`postgres-configmap`, `postgres-secrets`
e Service `postgres`).

O seed do ADMIN lê `ACCOUNT_ADMIN_EMAIL`/`ACCOUNT_ADMIN_HASH` do Secret `account-secrets`,
criado a partir do `.env` local (nada disso vai para o Git; `k8s/secrets.example.yaml`
mostra só o formato):

```bash
kubectl create secret generic account-secrets --from-env-file=.env
```

Imagem (o cluster puxa do Docker Hub; a tag é o commit do código):

```bash
mvn package
docker build -t pedrohmm1/account:<tag> -t pedrohmm1/account:latest .
docker push pedrohmm1/account:<tag>
docker push pedrohmm1/account:latest
```

Deploy e teste (o Service é interno; para testar de fora, use port-forward):

```bash
kubectl apply -f k8s/k8s.yaml
kubectl get pods,svc
kubectl logs deploy/account
kubectl port-forward svc/account 8081:8080   # http://localhost:8081/accounts/health-check
```

A JVM usa até 50% do limite de memória do container como heap
(`JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=50`), para não estourar os 512Mi.

**Readiness probe (TCP na 8080):** sem probe, o Kubernetes considera o pod pronto assim que o
container sobe e, no rollout, derruba o pod antigo na hora; como o Spring leva ~40 s para
inicializar, a API dava 503 durante o deploy (visto no build #2 do Jenkins). O Tomcat só abre a
porta 8080 no fim da inicialização, então um `tcpSocket` basta para indicar "pronto", sem
actuator nem mudança de código. O teste começa aos 20 s e se repete a cada 5 s; enquanto não
passa, o pod novo fica fora do Service e o antigo segue atendendo. Readiness não reinicia o
container: com o pod já pronto, 6 falhas seguidas só o tiram do Service até voltar a responder.

## CI/CD (Jenkins)

Estágios do `Jenkinsfile`:

1. `Dependencies`: dispara o job `account` (contrato) e espera.
2. `Build`: `mvn -B -DskipTests clean package`.
3. `Build & Push Image`: publica `pedrohmm1/account:latest` e `pedrohmm1/account:<BUILD_ID>`
   (linux/amd64 e linux/arm64) no Docker Hub.
4. `Deploy to EKS`: atualiza o Deployment `account` no cluster `eks-store` (us-east-2) para a
   imagem `pedrohmm1/account:<BUILD_ID>` que o estágio anterior acabou de publicar.

O deploy usa a credencial `aws-credential` do Jenkins (usuário IAM `store-jenkins`, que só pode
`eks:DescribeCluster` e, dentro do cluster, editar o namespace `default`). O estágio gera um
kubeconfig temporário no workspace (apagado ao sair, mesmo em caso de erro) e roda:

```bash
kubectl set image deployment/account account=pedrohmm1/account:<BUILD_ID>
kubectl rollout status deployment/account --timeout=300s
```

- Cluster desligado (`ResourceNotFoundException` no `describe-cluster`): avisa e pula o deploy,
  sem quebrar o build.
- Qualquer outro erro (credencial, permissão, Deployment inexistente, rollout que não termina
  em 5 min): o build falha.

**Tags:** depois de um deploy pelo Jenkins, o cluster roda a tag `<BUILD_ID>`. O `k8s/k8s.yaml`
deste repo continua com a tag do commit (`51f0491`), que é a usada pelo `aws/up.ps1` do
store-pma ao recriar o cluster. Ou seja, um cluster recém-criado volta para a imagem do
manifesto até o próximo build no Jenkins.
