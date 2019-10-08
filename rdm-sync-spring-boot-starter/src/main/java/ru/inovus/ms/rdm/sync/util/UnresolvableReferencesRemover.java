package ru.inovus.ms.rdm.sync.util;

import ru.inovus.ms.rdm.api.model.refbook.RefBook;
import ru.inovus.ms.rdm.sync.model.VersionMapping;

import java.util.List;

public class UnresolvableReferencesRemover {

    private UnresolvableReferencesRemover() {}

    public static void removeUnresolvable(List<RefBook> refBooks, List<VersionMapping> versionMappings) {
        int n = versionMappings.size();
        for (int i = 0; i < n; i++) {
            for (VersionMapping versionMapping : versionMappings) {
                String code = versionMapping.getCode();
                if (refBooks.stream().noneMatch(refBook -> refBook.getCode().equals(code))) {
                    refBooks.removeIf(
                        refBook -> refBook.getStructure().getReferences().stream().anyMatch(
                            reference -> reference.getReferenceCode().equals(code)
                        )
                    );
                }
            }
        }
    }

}
