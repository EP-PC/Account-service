package store.account;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Builder
@Data
@Accessors(fluent = true)
public class Account {

    private String id;
    private String name;
    private String email;
    // senha em texto puro: so existe em memoria, na entrada (nunca vai para o banco nem para a resposta)
    private String password;
    private String hashPassword;
    private String role;

}
