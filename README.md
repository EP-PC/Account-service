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
