-- Seed do primeiro ADMIN. Email e hash vem de variaveis de ambiente
-- (ACCOUNT_ADMIN_EMAIL / ACCOUNT_ADMIN_HASH -> placeholders do Flyway), nunca do repositorio.
-- O hash deve ser Base64(SHA-256(senha em UTF-8)), o mesmo formato do calcHash (ver README).
DO $$
BEGIN
    IF btrim('${admin_email}') = '' OR btrim('${admin_hash}') = '' THEN
        RAISE EXCEPTION 'ACCOUNT_ADMIN_EMAIL e ACCOUNT_ADMIN_HASH sao obrigatorias para o seed do ADMIN';
    END IF;
    -- SHA-256 em Base64 tem sempre 44 caracteres: barra quem colou a senha em claro por engano
    IF length(btrim('${admin_hash}')) <> 44 THEN
        RAISE EXCEPTION 'ACCOUNT_ADMIN_HASH invalido: esperado Base64 de SHA-256 (44 caracteres)';
    END IF;

    INSERT INTO accounts.accounts (id, name, email, hash_password, role)
    VALUES (gen_random_uuid()::text, 'Admin', btrim('${admin_email}'), btrim('${admin_hash}'), 'ADMIN')
    ON CONFLICT (email) DO NOTHING;
END $$;
