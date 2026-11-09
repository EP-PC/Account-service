package store.account;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class AccountResource implements AccountController {

    @Autowired
    private AccountService accountService;

    @Override
    public ResponseEntity<AccountOut> create(AccountIn in) {
        Account created = accountService.create(AccountParser.to(in));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();
        return ResponseEntity.created(location).body(AccountParser.to(created));
    }

    @Override
    public ResponseEntity<AccountOut> login(AccountIn in) {
        if (in == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credentials are required");
        }
        Account account = accountService.findByEmailAndPassword(in.email(), in.password());
        return ResponseEntity.ok(AccountParser.to(account));
    }

    @Override
    public ResponseEntity<List<AccountOut>> findAll() {
        return ResponseEntity.ok(AccountParser.to(accountService.findAll()));
    }

    @Override
    public ResponseEntity<AccountOut> findById(String id) {
        Account account = accountService.findById(id);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }
        return ResponseEntity.ok(AccountParser.to(account));
    }

    @Override
    public ResponseEntity<Void> delete(String id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> healthCheck() {
        return ResponseEntity.ok().build();
    }

}
