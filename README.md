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
