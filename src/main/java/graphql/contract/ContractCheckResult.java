package graphql.contract;

import java.util.List;

public class ContractCheckResult {

    private final List<String> errors;


    public ContractCheckResult(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
