package ru.inovus.ms.rdm.validation;

import java.util.*;

/**
 * Created by znurgaliev on 14.08.2018.
 */
public abstract class ErrorAttributeHolderValidator implements RdmValidation {

    private Set<String> errorAttributes;

    public Set<String> getErrorAttributes() {
        return errorAttributes == null ? Collections.EMPTY_SET : errorAttributes;
    }

    public void setErrorAttributes(Set<String> errorAttributes) {
        if (errorAttributes != null)
            this.errorAttributes = new HashSet<>(errorAttributes);
    }

    protected void addErrorAttribute(String errorAttribute) {
        if (errorAttributes == null)
            errorAttributes = new HashSet<>();
        errorAttributes.add(errorAttribute);
    }

}
