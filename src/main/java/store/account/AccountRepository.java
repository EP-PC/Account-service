package store.account;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends CrudRepository<AccountModel, String> {

    List<AccountModel> findAll();

    // login (hands-on 3): o Spring Data gera "WHERE email = ? AND hash_password = ?"
    Optional<AccountModel> findByEmailAndHashPassword(String email, String hashPassword);

    // usado no create para devolver 409 antes de bater na constraint UNIQUE
    boolean existsByEmail(String email);

}
