package store.account;

import java.util.List;

public class AccountParser {

    // a role nao vem do cliente: o AccountIn nem tem esse campo
    public static Account to(AccountIn in) {
        return in == null ? null :
            Account.builder()
                .name(in.name())
                .email(in.email())
                .password(in.password())
                .build();
    }

    // nunca expor senha nem hash na resposta
    public static AccountOut to(Account a) {
        return a == null ? null :
            AccountOut.builder()
                .id(a.id())
                .name(a.name())
                .email(a.email())
                .role(a.role())
                .build();
    }

    public static List<AccountOut> to(List<Account> accounts) {
        return accounts.stream().map(AccountParser::to).toList();
    }

}
