package ru.inovus.ms.rdm.impl.validation;

import java.util.*;

/**
 * Created by znurgaliev on 14.08.2018.
 */
public abstract class ErrorAttributeHolderValidation implements RdmValidation {

    private Set<String> errorAttributes;

    public Set<String> getErrorAttributes() {
        return errorAttributes == null ? Collections.emptySet(): errorAttributes;
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
