package store.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    // valores exatos da role (secao 4 do claude.md): String, sem enum
    public static final String ROLE_USER = "USER";

    private static final int PASSWORD_MIN_LENGTH = 6;

    @Autowired
    private AccountRepository accountRepository;

    public Account create(Account account) {
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is required");
        }
        if (account.name() == null || account.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        if (account.email() == null || account.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (account.password() == null || account.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        final String pass = account.password().trim();
        if (pass.length() < PASSWORD_MIN_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Password must have at least " + PASSWORD_MIN_LENGTH + " characters");
        }
        final String email = account.email().trim();
        if (accountRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        account.id(null);                   // o id e gerado pelo JPA
        account.name(account.name().trim());
        account.email(email);
        account.hashPassword(calcHash(pass));
        account.password(null);             // descarta a senha em texto puro
        account.role(ROLE_USER);            // o cliente nunca escolhe a role

        try {
            return accountRepository.save(new AccountModel(account)).to();
        } catch (DataIntegrityViolationException e) {
            // corrida: dois POSTs com o mesmo email passaram juntos pelo existsByEmail;
            // a constraint UNIQUE barra o segundo e devolvemos 409 em vez de 500
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
    }

    // hands-on 3: usado pelo Auth no login
    public Account findByEmailAndPassword(String email, String password) {
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        // mesmo tratamento do create (trim), senao o hash nunca bate
        final String hash = calcHash(password.trim());
        return accountRepository.findByEmailAndHashPassword(email.trim(), hash)
            .map(AccountModel::to)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    }

    public Account findById(String id) {
        return accountRepository.findById(id)
            .map(AccountModel::to)
            .orElse(null);
    }

    public List<Account> findAll() {
        return accountRepository.findAll().stream()
            .map(AccountModel::to)
            .toList();
    }

    public void delete(String id) {
        accountRepository.deleteById(id);
    }

    // hands-on 1: SHA-256 em UTF-8, codificado em Base64 padrao
    private static String calcHash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(text.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
