package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.jaxrs.RestPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflict;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.validation.*;
import ru.i_novus.ms.rdm.api.model.version.CreateAttributeRequest;
import ru.i_novus.ms.rdm.api.model.version.DeleteAttributeRequest;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.model.version.UpdateAttributeRequest;
import ru.i_novus.ms.rdm.api.util.ConflictUtils;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.ms.rdm.api.util.StructureUtils;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.ms.rdm.n2o.model.AttributeCriteria;
import ru.i_novus.ms.rdm.n2o.model.FormAttribute;
import ru.i_novus.ms.rdm.n2o.model.ReadAttribute;
import ru.i_novus.ms.rdm.rest.client.impl.ConflictServiceRestClient;
import ru.i_novus.ms.rdm.rest.client.impl.DraftRestServiceRestClient;
import ru.i_novus.ms.rdm.rest.client.impl.RefBookServiceRestClient;
import ru.i_novus.ms.rdm.rest.client.impl.VersionRestServiceRestClient;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.model.version.UpdateAttributeRequest.setUpdateValueIfExists;

@Controller
@SuppressWarnings("WeakerAccess")
public class StructureController extends BaseController {

    private static final String ATTRIBUTE_TYPE_PREFIX = "attribute.type.";

    private static final int DISPLAY_TYPE_ATTRIBUTE = 1;
    private static final int DISPLAY_TYPE_EXPRESSION = 2;

    private final RefBookServiceRestClient refBookService;

    private final VersionRestServiceRestClient versionService;

    private final DraftRestServiceRestClient draftService;

    private final ConflictServiceRestClient conflictService;

    @Autowired
    public StructureController(RefBookServiceRestClient refBookService,
                               VersionRestServiceRestClient versionService,
                               DraftRestServiceRestClient draftService,
                               ConflictServiceRestClient conflictService,
                               Messages messages) {
        super(messages);

        this.refBookService = refBookService;
        this.versionService = versionService;
        this.draftService = draftService;

        this.conflictService = conflictService;
    }

    // used in: attribute.query.xml
    public RestPage<ReadAttribute> getPage(AttributeCriteria criteria) {

        Integer versionId = criteria.getVersionId();
        final RefBookVersion version = versionService.getById(versionId);
        if (version.hasEmptyStructure()) {
            return new RestPage<>(emptyList(), Pageable.unpaged(), 0);
        }

        if (criteria.getOptLockValue() != null) {
            version.setOptLockValue(criteria.getOptLockValue());
        }

        final List<Structure.Attribute> attributes = version.getStructure().getAttributes();
        final List<AttributeValidation> validations = draftService
                .getAttributeValidations(versionId, criteria.getCode());

        List<ReadAttribute> list;
        if (!isEmpty(attributes)) {
            list = toPageAttributes(attributes, criteria).stream()
                    .map(attribute -> toReadAttribute(attribute, version, validations))
                    .collect(toList());
        } else {
            list = emptyList();
        }

        return new RestPage<>(list, PageRequest.of(criteria.getPage(), criteria.getSize()), attributes.size());
    }

    // used in: attributeDefault.query.xml
    public ReadAttribute getDefault(AttributeCriteria criteria) {

        final Integer versionId = criteria.getVersionId();

        final ReadAttribute readAttribute = new ReadAttribute();
        readAttribute.setVersionId(versionId);
        readAttribute.setOptLockValue(criteria.getOptLockValue());
        readAttribute.setCode(criteria.getCode());

        enrichByRefBook(versionId, readAttribute);

        return readAttribute;
    }

    /** Отбор атрибутов, отображаемых на текущей странице. */
    private List<Structure.Attribute> toPageAttributes(List<Structure.Attribute> attributes, AttributeCriteria criteria) {
        return attributes.stream()
                .filter(attribute -> isCriteriaAttribute(attribute, criteria))
                .skip((long) (criteria.getPage() - 1) * criteria.getSize())
                .limit(criteria.getSize())
                .collect(toList());
    }

    /** Проверка на соответствие атрибута критерию поиска. */
    private boolean isCriteriaAttribute(Structure.Attribute attribute, AttributeCriteria criteria) {

        final String code = criteria.getCode();
        final String name = criteria.getName();
        return (StringUtils.isEmpty(code) || code.equals(attribute.getCode())) &&
                (StringUtils.isEmpty(name) || containsIgnoreCase(attribute.getName(), name));
    }

    /** Преобразование атрибута в атрибут для отображения на форме. */
    private ReadAttribute toReadAttribute(Structure.Attribute attribute, RefBookVersion version,
                                          List<AttributeValidation> validations) {

        final Structure.Reference reference = attribute.isReferenceType()
                ? version.getStructure().getReference(attribute.getCode())
                : null;
        final ReadAttribute readAttribute = toReadAttribute(attribute, reference);
        enrichAttribute(readAttribute, getValidations(validations, attribute.getCode()));

        readAttribute.setVersionId(version.getId());
        readAttribute.setOptLockValue(version.getOptLockValue());

        readAttribute.setIsReferrer(!isEmpty(version.getStructure().getReferences()));
        readAttribute.setCodeExpression(DisplayExpression.toPlaceholder(attribute.getCode()));

        enrichReference(readAttribute, reference);

        enrichByRefBook(version.getId(), readAttribute);

        return readAttribute;
    }

    private List<AttributeValidation> getValidations(List<AttributeValidation> validations, String attribute) {

        if (isEmpty(validations))
            return emptyList();

        return validations.stream().filter(v -> Objects.equals(attribute, v.getAttribute())).collect(toList());
    }

    /** Заполнение атрибута для отображения с учётом его представления. */
    private void enrichAttribute(ReadAttribute attribute, List<AttributeValidation> validations) {

        for (AttributeValidation validation : validations) {
            switch (validation.getType()) {
                case REQUIRED: attribute.setRequired(true); break;
                case UNIQUE: attribute.setUnique(true); break;
                case PLAIN_SIZE: enrichPlainSize(attribute, (PlainSizeAttributeValidation) validation); break;
                case FLOAT_SIZE: enrichFloatSize(attribute, (FloatSizeAttributeValidation) validation); break;
                case INT_RANGE: enrichIntRange(attribute, (IntRangeAttributeValidation) validation); break;
                case FLOAT_RANGE: enrichFloatRange(attribute, (FloatRangeAttributeValidation) validation); break;
                case DATE_RANGE: enrichDateRange(attribute, (DateRangeAttributeValidation) validation); break;
                case REG_EXP: attribute.setRegExp(((RegExpAttributeValidation) validation).getRegExp()); break;
            }
        }
    }

    private static void enrichPlainSize(ReadAttribute attribute, PlainSizeAttributeValidation validation) {

        attribute.setPlainSize(validation.getSize());
    }

    private static void enrichFloatSize(ReadAttribute attribute, FloatSizeAttributeValidation validation) {

        attribute.setIntPartSize(validation.getIntPartSize());
        attribute.setFracPartSize(validation.getFracPartSize());
    }

    private static void enrichIntRange(ReadAttribute attribute, IntRangeAttributeValidation validation) {

        attribute.setMinInteger(validation.getMin());
        attribute.setMaxInteger(validation.getMax());
    }

    private static void enrichFloatRange(ReadAttribute attribute, FloatRangeAttributeValidation validation) {

        attribute.setMinFloat(validation.getMin());
        attribute.setMaxFloat(validation.getMax());
    }

    private void enrichDateRange(ReadAttribute attribute, DateRangeAttributeValidation validation) {

        attribute.setMinDate(validation.getMin());
        attribute.setMaxDate(validation.getMax());
    }

    /** Заполнение атрибута-ссылки для отображения с учётом его представления. */
    private void enrichReference(ReadAttribute attribute, Structure.Reference reference) {

        if (reference == null) return;

        final Integer referenceRefBookId = refBookService.getId(reference.getReferenceCode());
        attribute.setReferenceRefBookId(referenceRefBookId);

        int displayType = DISPLAY_TYPE_ATTRIBUTE;
        final String displayExpression = reference.getDisplayExpression();
        if (!StringUtils.isEmpty(displayExpression)) {
            attribute.setDisplayExpression(displayExpression);

            displayType = DISPLAY_TYPE_EXPRESSION;
            final String attributeCode = StructureUtils.displayExpressionToPlaceholder(displayExpression);
            if (attributeCode != null) {
                displayType = DISPLAY_TYPE_ATTRIBUTE;
                attribute.setDisplayAttribute(attributeCode);
                attribute.setDisplayAttributeName(attributeCodeToName(reference.getReferenceCode(), attributeCode));
            }
        }
        attribute.setDisplayType(displayType);
    }

    private String attributeCodeToName(String refBookCode, String attributeCode) {

        final RefBookVersion version = versionService.getLastPublishedVersion(refBookCode);
        final Structure.Attribute attribute = version.getStructure().getAttribute(attributeCode);
        return (attribute != null) ? attribute.getName() : null;
    }

    /** Заполнение атрибута (+ ссылки) для отображения по информации о справочнике. */
    private void enrichByRefBook(Integer versionId, ReadAttribute readAttribute) {

        final RefBook refBook = refBookService.getByVersionId(versionId);
        readAttribute.setHasReferrer(refBook.getHasReferrer());

        if (readAttribute.isReferenceType()) {
            readAttribute.setHasStructureConflict(getHasStructureConflict(refBook.getId(), readAttribute.getCode()));
        }
    }

    private Boolean getHasStructureConflict(Integer versionId, String fieldCode) {

        final RefBookConflictCriteria conflictCriteria = new RefBookConflictCriteria();
        conflictCriteria.setReferrerVersionId(versionId);
        conflictCriteria.setRefFieldCodes(List.of(fieldCode));
        conflictCriteria.setConflictTypes(ConflictUtils.getStructureConflictTypes());
        conflictCriteria.setIsLastPublishedVersion(true);
        conflictCriteria.setPageSize(1);

        final Page<RefBookConflict> conflicts = conflictService.search(conflictCriteria);
        return conflicts != null && !isEmpty(conflicts.getContent());
    }

    public void createAttribute(Integer versionId, Integer optLockValue, FormAttribute formAttribute) {

        final CreateAttributeRequest request = getCreateAttributeRequest(optLockValue, formAttribute);
        request.setValidations(createValidations(formAttribute));

        draftService.createAttribute(versionId, request);
    }

    public void updateAttribute(Integer versionId, Integer optLockValue, FormAttribute formAttribute) {

        final UpdateAttributeRequest request = getUpdateAttributeRequest(optLockValue, formAttribute);
        request.setValidations(createValidations(formAttribute));

        draftService.updateAttribute(versionId, request);
    }

    public void deleteAttribute(Integer versionId, Integer optLockValue, String attributeCode) {

        final DeleteAttributeRequest request = new DeleteAttributeRequest(optLockValue, attributeCode);

        draftService.deleteAttribute(versionId, request);
    }

    /** Заполнение валидаций атрибута из атрибута формы. */
    private List<AttributeValidation> createValidations(FormAttribute formAttribute) {

        final List<AttributeValidation> validations = new ArrayList<>(8);

        if (Boolean.TRUE.equals(formAttribute.getRequired())) {
            validations.add(new RequiredAttributeValidation());
        }
        if (Boolean.TRUE.equals(formAttribute.getUnique())) {
            validations.add(new UniqueAttributeValidation());
        }

        if (formAttribute.getPlainSize() != null) {
            validations.add(new PlainSizeAttributeValidation(formAttribute.getPlainSize()));
        }
        if (formAttribute.getIntPartSize() != null || formAttribute.getFracPartSize() != null) {
            validations.add(new FloatSizeAttributeValidation(formAttribute.getIntPartSize(), formAttribute.getFracPartSize()));
        }
        if (formAttribute.getMinInteger() != null || formAttribute.getMaxInteger() != null) {
            validations.add(new IntRangeAttributeValidation(formAttribute.getMinInteger(), formAttribute.getMaxInteger()));
        }
        if (formAttribute.getMinFloat() != null || formAttribute.getMaxFloat() != null) {
            validations.add(new FloatRangeAttributeValidation(formAttribute.getMinFloat(), formAttribute.getMaxFloat()));
        }

        if (formAttribute.getMinDate() != null || formAttribute.getMaxDate() != null) {
            validations.add(new DateRangeAttributeValidation(formAttribute.getMinDate(), formAttribute.getMaxDate()));
        }

        if (formAttribute.getRegExp() != null) {
            validations.add(new RegExpAttributeValidation(formAttribute.getRegExp()));
        }

        return validations;
    }

    /** Преобразование конкретного атрибута (+ ссылки) в атрибут для отображения на форме. */
    private ReadAttribute toReadAttribute(Structure.Attribute attribute,
                                          Structure.Reference reference) {

        final ReadAttribute readAttribute = new ReadAttribute();
        readAttribute.setCode(attribute.getCode());
        readAttribute.setName(attribute.getName());
        readAttribute.setType(attribute.getType());
        readAttribute.setTypeName(toEnumLocaleName(ATTRIBUTE_TYPE_PREFIX, attribute.getType()));

        readAttribute.setIsPrimary(attribute.getIsPrimary());
        readAttribute.setLocalizable(attribute.getLocalizable());
        readAttribute.setDescription(attribute.getDescription());

        if (Objects.nonNull(reference)) {
            readAttribute.setReferenceCode(reference.getReferenceCode());
            readAttribute.setDisplayExpression(reference.getDisplayExpression());
        }

        return readAttribute;
    }

    /** Получение атрибута для добавления из атрибута формы. */
    private CreateAttributeRequest getCreateAttributeRequest(Integer optLockValue,
                                                             FormAttribute formAttribute) {
        return new CreateAttributeRequest(optLockValue,
                buildAttribute(formAttribute), buildReference(formAttribute)
        );
    }

    private Structure.Attribute buildAttribute(FormAttribute request) {

        if (request.hasIsPrimary()) {
            return Structure.Attribute.buildPrimary(request.getCode(),
                    request.getName(), request.getType(), request.getDescription());

        } else if (request.isLocalizable()) {
            return Structure.Attribute.buildLocalizable(request.getCode(),
                    request.getName(), request.getType(), request.getDescription());

        } else {
            return Structure.Attribute.build(request.getCode(), request.getName(),
                    request.getType(), request.getDescription());
        }
    }

    private Structure.Reference buildReference(FormAttribute request) {

        return new Structure.Reference(request.getCode(),
                request.getReferenceCode(),
                request.getDisplayExpression());
    }

    /** Получение атрибута для изменения из атрибута формы. */
    private UpdateAttributeRequest getUpdateAttributeRequest(Integer optLockValue,
                                                             FormAttribute formAttribute) {

        final UpdateAttributeRequest request = new UpdateAttributeRequest();
        request.setLastActionDate(TimeUtils.nowZoned());

        request.setOptLockValue(optLockValue);

        // attribute fields:
        request.setCode(formAttribute.getCode());
        request.setType(formAttribute.getType());
        request.setDescription(formAttribute.getDescription());

        setUpdateValueIfExists(formAttribute::getName, request::setName);
        setUpdateValueIfExists(formAttribute::getIsPrimary, request::setIsPrimary);
        setUpdateValueIfExists(formAttribute::getLocalizable, request::setLocalizable);

        // reference fields:
        setUpdateValueIfExists(formAttribute::getCode, request::setAttribute);
        setUpdateValueIfExists(formAttribute::getReferenceCode, request::setReferenceCode);
        setUpdateValueIfExists(formAttribute::getDisplayExpression, request::setDisplayExpression);

        return request;
    }
}
