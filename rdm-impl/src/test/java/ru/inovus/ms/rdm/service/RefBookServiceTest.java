package ru.inovus.ms.rdm.service;

import com.querydsl.core.BooleanBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.entity.RefBookEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RefBookServiceTest {

    private static final Integer REFERRER_ONE_REF_BOOK_ID = -1;
    private static final String REFERRER_ONE_REF_BOOK_CODE = "TEST_REFERRER_ONE";
    private static final Integer REFERRER_ONE_VERSION_ID = -11;
    private static final String REFERRER_ONE_REFER_ATTRIBUTE_CODE = "ref";
    private static final String REFERRER_ONE_REFER_DISPLAY_EXPRESSION = "${name}: ${amount}";

    private static final Integer REFERRER_TWO_REF_BOOK_ID = -2;
    private static final String REFERRER_TWO_REF_BOOK_CODE = "TEST_REFERRER_TWO";
    private static final Integer REFERRER_TWO_VERSION_ID = -22;
    private static final String REFERRER_TWO_REFER_ATTRIBUTE_CODE_1 = "ref1";
    private static final String REFERRER_TWO_REFER_DISPLAY_EXPRESSION_1 = "${name}";
    private static final String REFERRER_TWO_REFER_ATTRIBUTE_CODE_2 = "ref2";
    private static final String REFERRER_TWO_REFER_DISPLAY_EXPRESSION_2 = "Amount = ${amount}";

    private static final Integer REFERRER_DIF_REF_BOOK_ID = -3;
    private static final String REFERRER_DIF_REF_BOOK_CODE = "TEST_REFERRER_DIF";
    private static final Integer REFERRER_DIF_VERSION_ID = -33;
    private static final String REFERRER_DIF_REFER_ATTRIBUTE_CODE_1 = "valid";
    private static final String REFERRER_DIF_REFER_DISPLAY_EXPRESSION_1 = "${name} (${code})";
    private static final String REFERRER_DIF_REFER_ATTRIBUTE_CODE_2 = "other";
    private static final String REFERRER_DIF_REFER_DISPLAY_EXPRESSION_2 = "${opname} (${opcode})";

    private static final Integer REFERRER_NON_REF_BOOK_ID = -10;
    private static final String REFERRER_NON_REF_BOOK_CODE = "TEST_REFERRER_NON";
    private static final Integer REFERRER_NON_VERSION_ID = -101;

    private static final Integer CHECKING_REF_BOOK_ID = -100;
    private static final String CHECKING_REF_BOOK_CODE = "TEST_CHECKING_BOOK";
    private static final Integer CHECKING_VERSION_ID = -1000;
    private static final String CHECKING_PRIMARY_CODE = "code";

    private static final Integer UNCHECKING_REF_BOOK_ID = -101;
    private static final String UNCHECKING_REF_BOOK_CODE = "TEST_UNCHECKING_BOOK";
    private static final Integer UNCHECKING_VERSION_ID = -1001;
    private static final String UNCHECKING_PRIMARY_CODE = "opcode";

    @InjectMocks
    private RefBookServiceImpl refBookService;

    @Mock
    private VersionServiceImpl versionService;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Test
    public void testGetReferrerVersions() {
        RefBookVersion referrerOneVersion = createReferrerOneVersion();
        RefBookVersion referrerTwoVersion = createReferrerTwoVersion();
        RefBookVersion referrerDifVersion = createReferrerDifVersion();
        RefBookVersion referrerNonVersion = createReferrerNonVersion();

        ArrayList<RefBookVersion> versions = new ArrayList<>(asList(referrerOneVersion, referrerTwoVersion, referrerDifVersion, referrerNonVersion));
        List<RefBookVersionEntity> entitites = versions.stream().map(this::modelToEntity).collect(Collectors.toList());
        when(versionRepository.findAll(any(BooleanBuilder.class), any(Pageable.class))).thenReturn(new PageImpl<>(entitites));

        List<RefBookVersion> actualList = refBookService.getReferrerVersions(CHECKING_REF_BOOK_CODE);
        List<RefBookVersion> expectedList = new ArrayList<>(asList(referrerOneVersion, referrerTwoVersion, referrerDifVersion));

        Assert.assertNotNull(actualList);
        Assert.assertEquals(expectedList.size(), actualList.size());

        // Models are not override `equals`.
        IntStream.range(0, expectedList.size() - 1)
                .forEach(i -> {
                    Assert.assertEquals(modelToEntity(expectedList.get(i)), modelToEntity(actualList.get(i)));
                });
    }

    private RefBookVersion createReferrerOneVersion() {
        RefBookVersion version = new RefBookVersion();
        version.setCode(REFERRER_ONE_REF_BOOK_CODE);
        version.setId(REFERRER_ONE_VERSION_ID);
        version.setRefBookId(REFERRER_ONE_REF_BOOK_ID);
        version.setStatus(RefBookVersionStatus.DRAFT);

        Structure structure = new Structure(
                asList(
                        Structure.Attribute.buildPrimary("str", "string", FieldType.STRING, "строка"),
                        Structure.Attribute.build(REFERRER_ONE_REFER_ATTRIBUTE_CODE, "reference", FieldType.REFERENCE, "ссылка")
                ),
                singletonList(
                        new Structure.Reference(REFERRER_ONE_REFER_ATTRIBUTE_CODE, CHECKING_REF_BOOK_CODE, REFERRER_ONE_REFER_DISPLAY_EXPRESSION)
                )
        );
        version.setStructure(structure);

        return version;
    }

    private RefBookVersionEntity modelToEntity(RefBookVersion model) {
        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(model.getId());
        entity.setStructure(model.getStructure());

        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setId(model.getRefBookId());
        refBookEntity.setCode(model.getCode());
        entity.setRefBook(refBookEntity);

        return entity;
    }

    private RefBookVersion createReferrerTwoVersion() {
        RefBookVersion version = new RefBookVersion();
        version.setCode(REFERRER_TWO_REF_BOOK_CODE);
        version.setId(REFERRER_TWO_VERSION_ID);
        version.setRefBookId(REFERRER_TWO_REF_BOOK_ID);
        version.setStatus(RefBookVersionStatus.DRAFT);

        Structure structure = new Structure(
                asList(
                        Structure.Attribute.buildPrimary("str", "string", FieldType.STRING, "строка"),
                        Structure.Attribute.build(REFERRER_TWO_REFER_ATTRIBUTE_CODE_1, "reference1", FieldType.REFERENCE, "ссылка 1"),
                        Structure.Attribute.build(REFERRER_TWO_REFER_ATTRIBUTE_CODE_2, "reference2", FieldType.REFERENCE, "ссылка 2")
                ),
                asList(
                        new Structure.Reference(REFERRER_TWO_REFER_ATTRIBUTE_CODE_1, CHECKING_REF_BOOK_CODE, REFERRER_TWO_REFER_DISPLAY_EXPRESSION_1),
                        new Structure.Reference(REFERRER_TWO_REFER_ATTRIBUTE_CODE_2, CHECKING_REF_BOOK_CODE, REFERRER_TWO_REFER_DISPLAY_EXPRESSION_2)
                )
        );
        version.setStructure(structure);

        return version;
    }

    private RefBookVersion createReferrerDifVersion() {
        RefBookVersion version = new RefBookVersion();
        version.setCode(REFERRER_DIF_REF_BOOK_CODE);
        version.setId(REFERRER_DIF_VERSION_ID);
        version.setRefBookId(REFERRER_DIF_REF_BOOK_ID);
        version.setStatus(RefBookVersionStatus.DRAFT);

        Structure structure = new Structure(
                asList(
                        Structure.Attribute.buildPrimary("str", "string", FieldType.STRING, "строка"),
                        Structure.Attribute.build(REFERRER_DIF_REFER_ATTRIBUTE_CODE_1, "valid ref", FieldType.REFERENCE, "верная ссылка"),
                        Structure.Attribute.build(REFERRER_DIF_REFER_ATTRIBUTE_CODE_2, "other ref", FieldType.REFERENCE, "другая ссылка")
                ),
                asList(
                        new Structure.Reference(REFERRER_DIF_REFER_ATTRIBUTE_CODE_1, CHECKING_REF_BOOK_CODE, REFERRER_DIF_REFER_DISPLAY_EXPRESSION_1),
                        new Structure.Reference(REFERRER_DIF_REFER_ATTRIBUTE_CODE_2, UNCHECKING_REF_BOOK_CODE, REFERRER_DIF_REFER_DISPLAY_EXPRESSION_2)
                )
        );
        version.setStructure(structure);

        return version;
    }

    private RefBookVersion createReferrerNonVersion() {
        RefBookVersion version = new RefBookVersion();
        version.setCode(REFERRER_NON_REF_BOOK_CODE);
        version.setId(REFERRER_NON_VERSION_ID);
        version.setRefBookId(REFERRER_NON_REF_BOOK_ID);
        version.setStatus(RefBookVersionStatus.DRAFT);

        Structure structure = new Structure(
                singletonList(
                        Structure.Attribute.buildPrimary("str", "string", FieldType.STRING, "строка")
                ),
                emptyList()
        );
        version.setStructure(structure);

        return version;
    }

    private RefBookVersion createCheckingVersion() {
        RefBookVersion version = new RefBookVersion();
        version.setCode(CHECKING_REF_BOOK_CODE);
        version.setId(CHECKING_VERSION_ID);
        version.setRefBookId(CHECKING_REF_BOOK_ID);
        version.setStatus(RefBookVersionStatus.DRAFT);

        Structure structure = new Structure(
                asList(
                        Structure.Attribute.buildPrimary(CHECKING_PRIMARY_CODE, "Код", FieldType.STRING, "строковый код"),
                        Structure.Attribute.build("name", "Название", FieldType.STRING, "наименование"),
                        Structure.Attribute.build("amount", "Количество", FieldType.INTEGER, "количество единиц")
                ),
                emptyList()
        );
        version.setStructure(structure);

        return version;
    }

}
