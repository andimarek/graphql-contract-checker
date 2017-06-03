package graphql.contract;

import graphql.InvalidSyntaxError;
import graphql.validation.ValidationError;

import java.util.List;

public class ContractCheckResult {

    private boolean result;

    private final List<String> compatibilityErrors;
    private InvalidSyntaxError invalidSyntaxError;
    private final List<ValidationError> validationErrors;


    public ContractCheckResult(boolean result, List<ValidationError> validationErrors, List<String> compatibilityErrors, InvalidSyntaxError invalidSyntaxError) {
        this.result = result;
        this.validationErrors = validationErrors;
        this.compatibilityErrors = compatibilityErrors;
        this.invalidSyntaxError = invalidSyntaxError;
    }


    public boolean isResult() {
        return result;
    }

    public List<String> getCompatibilityErrors() {
        return compatibilityErrors;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }


    public InvalidSyntaxError getInvalidSyntaxError() {
        return invalidSyntaxError;
    }

    @Override
    public String toString() {
        return "ContractCheckResult{" +
                "result=" + result +
                ", compatibilityErrors=" + compatibilityErrors +
                ", invalidSyntaxError=" + invalidSyntaxError +
                ", validationErrors=" + validationErrors +
                '}';
    }
}
