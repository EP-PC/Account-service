package store.account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Regras de autorizacao do Account (Fase 2), a partir dos headers que o Gateway injeta.
 *
 * Premissa (ver README): so o Gateway e exposto para fora, e ele
 *  - apaga id-account/id-role enviados pelo cliente;
 *  - injeta id-account (e id-role) em toda rota protegida, depois de validar o JWT.
 * Logo: com id-account = chamada externa (usuario logado, via Gateway);
 *       sem id-account = chamada interna (Auth via Feign, na rede do compose).
 *
 * Os headers sao lidos aqui, e nao na interface AccountController, porque o contrato
 * store:account esta congelado (o Auth compila contra ele).
 */
@Component
public class AccountAuthorization {

    public static final String HEADER_ID_ACCOUNT = "id-account";
    public static final String HEADER_ID_ROLE = "id-role";
    public static final String ROLE_ADMIN = "ADMIN";

    // o Spring injeta um proxy que aponta para o request da thread atual,
    // entao e seguro usar num bean singleton
    @Autowired
    private HttpServletRequest request;

    // sem id-account: veio da rede interna (Auth), nao do Gateway
    public boolean isInternal() {
        return idAccount() == null;
    }

    // create e login: so o Auth (interno) pode; usuario logado nao chama o Account direto
    public void requireInternal() {
        if (!isInternal()) {
            throw forbidden();
        }
    }

    // lista: interna libera; externa so ADMIN
    public void requireAdmin() {
        if (!isInternal() && !isAdmin()) {
            throw forbidden();
        }
    }

    // ler/apagar uma conta: interna libera; externa so o dono ou ADMIN
    public void requireOwnerOrAdmin(String id) {
        if (isInternal() || isAdmin()) {
            return;
        }
        if (!idAccount().equals(id)) {
            throw forbidden();
        }
    }

    // id-account presente sem id-role: trata como USER (menor privilegio)
    private boolean isAdmin() {
        return ROLE_ADMIN.equals(header(HEADER_ID_ROLE));
    }

    private String idAccount() {
        return header(HEADER_ID_ACCOUNT);
    }

    // header vazio conta como ausente
    private String header(String name) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

}
