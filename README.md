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
