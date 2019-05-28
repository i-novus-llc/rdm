package ru.inovus.ms.rdm.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConflictServiceTest {

    private static final String REFERRER_REF_BOOK_CODE = "TEST_REFERRER_BOOK";

    private static final String PUBLISHED_REF_BOOK_CODE = "TEST_PUBLISHED_BOOK";

    @Mock
    private VersionService versionService;
    @Mock
    private DraftService draftService;
    @Mock
    private DraftDataService draftDataService;

    @Mock
    private RefBookVersionRepository versionRepository;

    private RefBookVersion referrerVersion;
    private RefBookVersion publishedVersion;

    @Before
    public void setUp() {
        referrerVersion = new RefBookVersion();
        referrerVersion.setId(-1);
        referrerVersion.setCode(REFERRER_REF_BOOK_CODE);

        Structure referrerStructure = new Structure(
                asList(
                        Structure.Attribute.buildPrimary("str", "string", FieldType.STRING, "строка"),
                        Structure.Attribute.build("ref", "reference", FieldType.REFERENCE, "ссылка")
                ),
                singletonList(
                        new Structure.Reference("ref", PUBLISHED_REF_BOOK_CODE, "${name}: ${amount}")
                )
            );
        referrerVersion.setStructure(referrerStructure);

        publishedVersion = new RefBookVersion();
        publishedVersion.setId(-2);
        publishedVersion.setCode(PUBLISHED_REF_BOOK_CODE);

        Structure publishedStructure = new Structure(
                asList(
                        Structure.Attribute.buildPrimary("code", "Код", FieldType.STRING, "строковый код"),
                        Structure.Attribute.build("name", "Название", FieldType.STRING, "наименование"),
                        Structure.Attribute.build("amount", "Количество", FieldType.INTEGER, "количество единиц")
                ),
                emptyList()
            );
        publishedVersion.setStructure(publishedStructure);
    }

    @Test
    public void calculateConflicts() {
    }

    @Test
    public void updateReferenceValues() {
    }
}