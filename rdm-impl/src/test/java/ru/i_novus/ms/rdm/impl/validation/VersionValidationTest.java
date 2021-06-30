package ru.i_novus.ms.rdm.impl.validation;

import com.querydsl.core.types.dsl.BooleanExpression;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookStatusType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.impl.BaseTest;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.impl.util.StructureTestConstants.*;

@RunWith(MockitoJUnitRunner.class)
public class VersionValidationTest extends BaseTest {

    private static final Integer REFBOOK_ID = -10;
    private static final String REFBOOK_CODE = "test";
    private static final Integer VERSION_ID = 2;
    private static final Integer DRAFT_ID = 6;
    private static final Structure STRUCTURE = new Structure(DEFAULT_STRUCTURE);

    private static final Integer REFERRED_BOOK_ID = -20;
    private static final Integer SELF_REFERRED_BOOK_ID = -30;

    @InjectMocks
    private VersionValidationImpl versionValidation;

    @Mock
    private RefBookRepository refBookRepository;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Test
    public void testValidateRefBook() {

        when(refBookRepository.existsById(REFBOOK_ID)).thenReturn(true);
        when(versionRepository.exists(any(BooleanExpression.class))).thenReturn(false);
        validateSuccess(
                () -> versionValidation.validateRefBook(REFBOOK_ID)
        );

        verify(refBookRepository).existsById(REFBOOK_ID);
        verify(versionRepository).exists(any(BooleanExpression.class));
        verifyNoMore();
    }

    @Test
    public void testValidateRefBookFail() {

        when(refBookRepository.existsById(REFBOOK_ID)).thenReturn(true);
        when(versionRepository.exists(any(BooleanExpression.class))).thenReturn(true);
        validateFailure(
                () -> versionValidation.validateRefBook(REFBOOK_ID),
                UserException.class,
                "refbook.is.archived"
        );

        verify(refBookRepository).existsById(REFBOOK_ID);
        verify(versionRepository).exists(any(BooleanExpression.class));
        verifyNoMore();
    }

    @Test
    public void testValidateRefBookCode() {

        validateSuccess(
                () -> versionValidation.validateRefBookCode("aBook-0")
        );
    }

    @Test
    public void testValidateRefBookCodeFail() {

        testValidateRefBookCodeFail(null);
        testValidateRefBookCodeFail("Реестр");
        testValidateRefBookCodeFail("001");
        testValidateRefBookCodeFail("Book/2");
    }

    private void testValidateRefBookCodeFail(String refBookCode) {

        validateFailure(
                () -> versionValidation.validateRefBookCode(refBookCode),
                UserException.class,
                List.of("refbook.code.is.invalid", "code.is.invalid")
        );
    }

    @Test
    public void testValidateRefBookExists() {

        when(refBookRepository.existsById(REFBOOK_ID)).thenReturn(true);
        validateSuccess(
                () -> versionValidation.validateRefBookExists(REFBOOK_ID)
        );
    }

    @Test
    public void testValidateRefBookExistsFail() {

        when(refBookRepository.existsById(REFBOOK_ID)).thenReturn(false);
        validateFailure(
                () -> versionValidation.validateRefBookExists(REFBOOK_ID),
                NotFoundException.class,
                "refbook.not.found"
        );
    }

    @Test
    public void testValidateRefBookCodeExists() {

        when(refBookRepository.existsByCode(REFBOOK_CODE)).thenReturn(true);
        validateSuccess(
                () -> versionValidation.validateRefBookCodeExists(REFBOOK_CODE)
        );
    }

    @Test
    public void testValidateRefBookCodeExistsFail() {

        when(refBookRepository.existsByCode(REFBOOK_CODE)).thenReturn(false);
        validateFailure(
                () -> versionValidation.validateRefBookCodeExists(REFBOOK_CODE),
                NotFoundException.class,
                "refbook.with.code.not.found"
        );
    }

    @Test
    public void testValidateRefBookCodeNotExists() {

        when(refBookRepository.existsByCode(REFBOOK_CODE)).thenReturn(false);
        validateSuccess(
                () -> versionValidation.validateRefBookCodeNotExists(REFBOOK_CODE)
        );
    }

    @Test
    public void testValidateRefBookCodeNotExistsFail() {

        when(refBookRepository.existsByCode(REFBOOK_CODE)).thenReturn(true);
        validateFailure(
                () -> versionValidation.validateRefBookCodeNotExists(REFBOOK_CODE),
                UserException.class,
                "refbook.with.code.already.exists"
        );
    }

    @Test
    public void testHasReferrerVersions() {

        when(versionRepository.existsReferrerVersions(REFBOOK_CODE, RefBookStatusType.ALL.name(), RefBookSourceType.ALL.name()))
                .thenReturn(true);

        boolean hasReferrers = versionValidation.hasReferrerVersions(REFBOOK_CODE);
        assertTrue(hasReferrers);
    }

    @Test
    public void testValidateVersionExists() {

        when(versionRepository.existsById(VERSION_ID)).thenReturn(true);
        validateSuccess(
                () -> versionValidation.validateVersionExists(VERSION_ID)
        );
    }

    @Test
    public void testValidateVersionExistsFail() {

        when(versionRepository.existsById(VERSION_ID)).thenReturn(false);
        validateFailure(
                () -> versionValidation.validateVersionExists(VERSION_ID),
                NotFoundException.class,
                "version.not.found"
        );
    }

    @Test
    public void testValidateOptLockValue() {

        validateSuccess(
                () -> versionValidation.validateOptLockValue(DRAFT_ID, 3, 3)
        );
    }

    @Test
    public void testValidateOptLockValueFail() {

        validateFailure(
                () -> versionValidation.validateOptLockValue(DRAFT_ID, 3, 2),
                UserException.class,
                "draft.was.changed"
        );
    }

    @Test
    public void testValidateDraftNotArchived() {

        when(versionRepository.exists(any(BooleanExpression.class))).thenReturn(false);
        validateSuccess(
                () -> versionValidation.validateDraftNotArchived(DRAFT_ID)
        );
    }

    @Test
    public void testValidateDraftNotArchivedFail() {

        when(versionRepository.exists(any(BooleanExpression.class))).thenReturn(true);
        validateFailure(
                () -> versionValidation.validateDraftNotArchived(DRAFT_ID),
                UserException.class,
                "refbook.is.archived"
        );
    }

    @Test
    public void testValidateAttributeExists() {

        validateSuccess(
                () -> versionValidation.validateAttributeExists(VERSION_ID, STRUCTURE, NAME_ATTRIBUTE_CODE)
        );
    }

    @Test
    public void testValidateAttributeExistsFail() {

        validateFailure(
                () -> versionValidation.validateAttributeExists(VERSION_ID, STRUCTURE, UNKNOWN_ATTRIBUTE_CODE),
                NotFoundException.class,
                "version.attribute.not.found"
        );
    }

    @Test
    public void testValidateDraftAttributeExists() {

        validateSuccess(
                () -> versionValidation.validateDraftAttributeExists(DRAFT_ID, STRUCTURE, NAME_ATTRIBUTE_CODE)
        );
    }

    @Test
    public void testValidateDraftAttributeExistsFail() {

        validateFailure(
                () -> versionValidation.validateDraftAttributeExists(DRAFT_ID, STRUCTURE, UNKNOWN_ATTRIBUTE_CODE),
                NotFoundException.class,
                "draft.attribute.not.found"
        );
    }

    @Test
    public void testValidateStructure() {

        mockReferredEntity();
        mockSelfReferredEntity();

        validateSuccess(
                () -> versionValidation.validateStructure(STRUCTURE)
        );
    }

    @Test
    public void testValidateReferenceAbility() {

        mockReferredEntity();

        validateSuccess(
                () -> versionValidation.validateReferenceAbility(REFERENCE)
        );
    }

    @Test
    public void testValidateReferenceAbilityWhenEmpty() {

        Structure.Reference reference = new Structure.Reference(
                REFERENCE.getAttribute(),
                REFERENCE.getReferenceCode(),
                ""
        );

        validateFailure(
                () -> versionValidation.validateReferenceAbility(reference),
                UserException.class,
                "reference.display.expression.is.empty"
        );
    }

    @Test
    public void testValidateReferenceAbilityWhenAbsent() {

        RefBookVersionEntity referredEntity = createReferredEntity();

        when(versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(
                REFERRED_BOOK_CODE, RefBookVersionStatus.PUBLISHED
        ))
                .thenReturn(referredEntity);

        Structure.Reference reference = new Structure.Reference(
                REFERENCE.getAttribute(),
                REFERENCE.getReferenceCode(),
                DisplayExpression.toPlaceholder(UNKNOWN_ATTRIBUTE_CODE)
        );

        validateFailure(
                () -> versionValidation.validateReferenceAbility(reference),
                NotFoundException.class,
                "reference.referred.attribute.not.found"
        );

        reference.setDisplayExpression(
                reference.getDisplayExpression() +
                        DisplayExpression.toPlaceholder(UNKNOWN_ATTRIBUTE_CODE + "2")
        );

        validateFailure(
                () -> versionValidation.validateReferenceAbility(reference),
                NotFoundException.class,
                "reference.referred.attributes.not.found"
        );
    }

    @Test
    public void testValidateDraftStructure() {

        mockReferredEntity();
        mockSelfReferredEntity();

        when(versionRepository.existsReferrerVersions(REFBOOK_CODE, RefBookStatusType.ALL.name(), RefBookSourceType.ALL.name()))
                .thenReturn(true);

        RefBookVersionEntity draftEntity = new RefBookVersionEntity();
        draftEntity.setStructure(STRUCTURE);

        when(versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(
                REFBOOK_CODE, RefBookVersionStatus.PUBLISHED
        ))
                .thenReturn(draftEntity);

        validateSuccess(
                () -> versionValidation.validateDraftStructure(REFBOOK_CODE, draftEntity.getStructure())
        );
    }

    @Test
    public void testValidateReferrerStructure() {

        mockReferredEntity();
        mockSelfReferredEntity();

        validateSuccess(
                () -> versionValidation.validateReferrerStructure(STRUCTURE)
        );
    }

    @Test
    public void testValidateNewAttribute() {

        Structure oldStructure = new Structure(STRUCTURE);
        oldStructure.remove(NAME_ATTRIBUTE_CODE);

        Structure.Attribute newAttribute = new Structure.Attribute(NAME_ATTRIBUTE);

        validateSuccess(
                () -> versionValidation.validateNewAttribute(newAttribute, oldStructure, REFBOOK_CODE)
        );
    }

    @Test
    public void testValidateNewReference() {

        Structure oldStructure = new Structure(STRUCTURE);
        oldStructure.remove(REFERENCE_ATTRIBUTE_CODE);

        Structure.Attribute newAttribute = new Structure.Attribute(REFERENCE_ATTRIBUTE);
        Structure.Reference newReference = new Structure.Reference(REFERENCE);

        mockReferredEntity();

        validateSuccess(
                () -> versionValidation.validateNewReference(newAttribute, newReference, oldStructure, REFBOOK_CODE)
        );
    }

    @Test
    public void testValidateOldAttribute() {

        Structure oldStructure = new Structure(STRUCTURE);
        Structure.Attribute oldAttribute = new Structure.Attribute(NAME_ATTRIBUTE);

        validateSuccess(
                () -> versionValidation.validateOldAttribute(oldAttribute, oldStructure, REFBOOK_CODE)
        );
    }

    @Test
    public void testEqualsPrimaries() {

        Structure.Attribute idAttribute = new Structure.Attribute(ID_ATTRIBUTE);
        Structure.Attribute codeAttribute = new Structure.Attribute(CODE_ATTRIBUTE);

        assertFalse(versionValidation.equalsPrimaries(emptyList(), emptyList()));
        assertFalse(versionValidation.equalsPrimaries(List.of(idAttribute), emptyList()));
        assertFalse(versionValidation.equalsPrimaries(emptyList(), List.of(codeAttribute)));
        assertFalse(versionValidation.equalsPrimaries(List.of(idAttribute), List.of(codeAttribute)));
        assertTrue(versionValidation.equalsPrimaries(List.of(idAttribute), List.of(idAttribute)));
    }

    private void mockReferredEntity() {

        RefBookVersionEntity referredEntity = createReferredEntity();
        mockReferredEntity(REFERRED_BOOK_CODE, referredEntity);
    }

    private void mockSelfReferredEntity() {

        RefBookVersionEntity referredEntity = createSelfReferredEntity();
        mockReferredEntity(SELF_REFERRED_BOOK_CODE, referredEntity);
    }

    private void mockReferredEntity(String referredCode, RefBookVersionEntity referredEntity) {

        when(versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(
                referredCode, RefBookVersionStatus.PUBLISHED
        ))
                .thenReturn(referredEntity);
    }

    private void verifyNoMore() {

        verifyNoMoreInteractions(refBookRepository, versionRepository);
    }

    private RefBookVersionEntity createReferredEntity() {

        RefBookVersionEntity referredEntity = new RefBookVersionEntity();
        referredEntity.setRefBook(createRefBookEntity(REFERRED_BOOK_ID, REFERRED_BOOK_CODE));
        referredEntity.setStructure(new Structure(REFERRED_STRUCTURE));

        return referredEntity;
    }

    private RefBookVersionEntity createSelfReferredEntity() {

        RefBookVersionEntity referredEntity = new RefBookVersionEntity();
        referredEntity.setRefBook(createRefBookEntity(SELF_REFERRED_BOOK_ID, SELF_REFERRED_BOOK_CODE));
        referredEntity.setStructure(new Structure(SELF_REFERRED_STRUCTURE));

        return referredEntity;
    }

    private RefBookEntity createRefBookEntity(Integer id, String code) {

        RefBookEntity entity = new DefaultRefBookEntity();
        entity.setId(id);
        entity.setCode(code);

        return entity;
    }
}