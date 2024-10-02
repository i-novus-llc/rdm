package ru.i_novus.ms.rdm.impl.queryprovider;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.model.version.VersionCriteria;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.ms.rdm.impl.entity.QPassportValueEntity;
import ru.i_novus.ms.rdm.impl.entity.QRefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.predicate.PassportPredicateProducer;
import ru.i_novus.ms.rdm.impl.predicate.RefBookVersionPredicates;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.isNull;

@Component
@SuppressWarnings("java:S3740")
public class RefBookVersionQueryProvider {

    private static final String CANNOT_ORDER_BY_EXCEPTION_CODE = "cannot.order.by \"{0}\"";

    private static final String PASSPORT_SORT_PREFIX = "passport";
    private static final String VERSION_ID_SORT_PROPERTY = "id";
    private static final String REF_BOOK_ID_SORT_PROPERTY = "refBookId";
    private static final String REF_BOOK_CODE_SORT_PROPERTY = "code";
    private static final String REF_BOOK_DISPLAY_CODE_SORT_PROPERTY = "displayCode";
    private static final String REF_BOOK_LAST_PUBLISH_DATE_SORT_PROPERTY = "lastPublishedDate";
    public static final String REF_BOOK_FROM_DATE_SORT_PROPERTY = "fromDate";
    private static final String REF_BOOK_CATEGORY_SORT_PROPERTY = "category";

    private final PassportPredicateProducer passportPredicateProducer;

    private final EntityManager entityManager;

    @Autowired
    public RefBookVersionQueryProvider(PassportPredicateProducer passportPredicateProducer,
                                       EntityManager entityManager) {
        this.passportPredicateProducer = passportPredicateProducer;

        this.entityManager = entityManager;
    }

    /**
     * Поиск сущностей версий по критерию.
     *
     * @param criteria критерий поиска
     * @return Список сущностей
     */
    public Page<RefBookVersionEntity> search(RefBookCriteria criteria) {

        JPAQuery<RefBookVersionEntity> jpaQuery =
                new JPAQuery<>(entityManager)
                        .select(QRefBookVersionEntity.refBookVersionEntity)
                        .from(QRefBookVersionEntity.refBookVersionEntity)
                        .where(toPredicate(criteria));

        long count = jpaQuery.fetchCount();

        sortQuery(jpaQuery, criteria);
        List<RefBookVersionEntity> refBookVersionEntityList = jpaQuery
                .offset(criteria.getOffset())
                .limit(criteria.getPageSize())
                .fetch();

        return new PageImpl<>(refBookVersionEntityList, criteria, count);
    }

    /**
     * Формирование предиката на основе критерия поиска.
     *
     * @param criteria критерий поиска
     * @return Предикат для запроса поиска
     */
    public Predicate toPredicate(RefBookCriteria criteria) {

        BooleanBuilder where = new BooleanBuilder();

        fillRefBookPredicate(criteria, where);
        fillRefBookVersionPredicate(criteria, where);

        return where.getValue();
    }

    /**
     * Заполнение предиката по параметрам для справочника на основе критерия поиска.
     *
     * @param criteria критерий поиска
     * @param where    предикат для запроса поиска
     */
    private void fillRefBookPredicate(RefBookCriteria criteria, BooleanBuilder where) {

        if (!CollectionUtils.isEmpty(criteria.getRefBookIds())) {
            where.and(RefBookVersionPredicates.isVersionOfRefBooks(criteria.getRefBookIds()));
        }

        if (!StringUtils.isEmpty(criteria.getCode())) {
            where.and(RefBookVersionPredicates.isCodeContains(criteria.getCode()));

        } else if (!StringUtils.isEmpty(criteria.getCodeExact())) {
            where.and(RefBookVersionPredicates.isCodeEquals(criteria.getCodeExact()));
        }

        if (criteria.getVersionId() != null) {
            where.and(RefBookVersionPredicates.refBookHasVersion(criteria.getVersionId()));
        }

        if (criteria.getExcludeByVersionId() != null) {
            where.andNot(RefBookVersionPredicates.refBookHasVersion(criteria.getExcludeByVersionId()));
        }

        if (!StringUtils.isEmpty(criteria.getCategory())) {
            where.and(RefBookVersionPredicates.refBookHasCategory(criteria.getCategory()));
        }

        if (criteria.getIsArchived()) {
            where.and(RefBookVersionPredicates.isArchived());
        }

        else if (criteria.getNonArchived()) {
            where.andNot(RefBookVersionPredicates.isArchived());
        }

        if (!StringUtils.isEmpty(criteria.getDisplayCode())) {
            where.and(RefBookVersionPredicates.isDisplayCodeContains(criteria.getDisplayCode()));
        }

        if (!CollectionUtils.isEmpty(criteria.getPassport())) {
            where.and(passportPredicateProducer.toPredicate(criteria.getPassport()));
        }
    }

    /**
     * Заполнение предиката по параметрам для версии справочника на основе критерия поиска.
     *
     * @param criteria критерий поиска
     * @param where    предикат для запроса поиска
     */
    private void fillRefBookVersionPredicate(RefBookCriteria criteria, BooleanBuilder where) {

        if (criteria.getFromDateBegin() != null) {
            where.and(RefBookVersionPredicates.isMaxFromDateEqOrAfter(criteria.getFromDateBegin()));
        }

        if (criteria.getFromDateEnd() != null) {
            where.and(RefBookVersionPredicates.isMaxFromDateEqOrBefore(criteria.getFromDateEnd()));
        }

        where.and(RefBookVersionPredicates.isSourceType(getSourceType(criteria)));

        if (criteria.getHasDraft()) {
            where.andNot(RefBookVersionPredicates.isArchived()).and(RefBookVersionPredicates.refBookHasDraft());
        }

        if (criteria.getHasPublished()) {
            where.andNot(RefBookVersionPredicates.isArchived()).and(RefBookVersionPredicates.refBookHasPublished());
        }

        if (criteria.getHasPrimaryAttribute()) {
            where.and(RefBookVersionPredicates.hasStructure()).and(RefBookVersionPredicates.hasPrimaryAttribute());
        }
    }

    private RefBookSourceType getSourceType(RefBookCriteria criteria) {

        RefBookSourceType sourceType = criteria.getSourceType();
        if (criteria.getIncludeVersions()) {
            // Если ищется последняя версия справочника, то выбирается требуемая последняя версия.
            if (criteria.getExcludeDraft()
                    && (isNull(sourceType) || RefBookSourceType.LAST_VERSION.equals(sourceType))) {
                sourceType = RefBookSourceType.LAST_PUBLISHED;
            }

        } else {
            // Если ищется список справочников, то выбираются только требуемые последние версии.
            if (isNull(sourceType)) {
                sourceType = RefBookSourceType.LAST_VERSION;
            }

            if (criteria.getExcludeDraft()
                    && RefBookSourceType.LAST_VERSION.equals(sourceType)) {
                sourceType = RefBookSourceType.LAST_PUBLISHED;
            }
        }

        return sourceType;
    }

    /**
     * Добавление сортировки в запрос на основе критерия.
     *
     * @param jpaQuery запрос
     * @param criteria критерий поиска
     */
    private void sortQuery(JPAQuery<RefBookVersionEntity> jpaQuery, RefBookCriteria criteria) {

        List<Sort.Order> orders = criteria.getOrders();

        if (CollectionUtils.isEmpty(orders)) {
            jpaQuery.orderBy(getLastPublishDateOrder(jpaQuery).asc());

        } else {
            criteria.getOrders().stream()
                    .filter(Objects::nonNull)
                    .forEach(order -> addSortOrder(jpaQuery, order));
        }
    }

    /**
     * Добавление сортировки в запрос по заданному порядку.
     *
     * @param jpaQuery запрос поиска
     * @param order    порядок сортировки
     */
    private void addSortOrder(JPAQuery<RefBookVersionEntity> jpaQuery, Sort.Order order) {

        ComparableExpressionBase sortExpression;

        if (order.getProperty().startsWith(PASSPORT_SORT_PREFIX)) {
            String property = order.getProperty().replaceFirst(PASSPORT_SORT_PREFIX + "\\.", "");
            QPassportValueEntity qPassportValueEntity = new QPassportValueEntity(PASSPORT_SORT_PREFIX + "_" + property);

            jpaQuery.leftJoin(QRefBookVersionEntity.refBookVersionEntity.passportValues, qPassportValueEntity)
                    .on(qPassportValueEntity.version.eq(QRefBookVersionEntity.refBookVersionEntity)
                            .and(qPassportValueEntity.attribute.code.eq(property)));
            sortExpression = qPassportValueEntity.value;

        } else if (REF_BOOK_LAST_PUBLISH_DATE_SORT_PROPERTY.equals(order.getProperty()))
            sortExpression = getLastPublishDateOrder(jpaQuery);
        else
            sortExpression = getSortOrder(order.getProperty());

        jpaQuery.orderBy(order.isAscending() ? sortExpression.asc() : sortExpression.desc());
    }

    /**
     * Получение сортировки по заданному коду сортировки.
     *
     * @param orderProperty строковый код сортировки
     */
    private static ComparableExpressionBase getSortOrder(String orderProperty) {

        switch (orderProperty) {
            case VERSION_ID_SORT_PROPERTY: return QRefBookVersionEntity.refBookVersionEntity.id;
            case REF_BOOK_ID_SORT_PROPERTY: return QRefBookVersionEntity.refBookVersionEntity.refBook.id;
            case REF_BOOK_CODE_SORT_PROPERTY:
            case REF_BOOK_DISPLAY_CODE_SORT_PROPERTY: return QRefBookVersionEntity.refBookVersionEntity.refBook.code;
            case REF_BOOK_FROM_DATE_SORT_PROPERTY: return QRefBookVersionEntity.refBookVersionEntity.fromDate;
            case REF_BOOK_CATEGORY_SORT_PROPERTY: return QRefBookVersionEntity.refBookVersionEntity.refBook.category;
            default: throw new UserException(new Message(CANNOT_ORDER_BY_EXCEPTION_CODE, orderProperty));
        }
    }

    private ComparableExpressionBase getLastPublishDateOrder(JPAQuery<RefBookVersionEntity> jpaQuery) {

        QRefBookVersionEntity qSortPublishedDateVersion = new QRefBookVersionEntity("sort_published_date");
        QRefBookVersionEntity whereVersion = new QRefBookVersionEntity("sort_published_date_max_version");

        jpaQuery.leftJoin(qSortPublishedDateVersion)
                .on(QRefBookVersionEntity.refBookVersionEntity.refBook.eq(qSortPublishedDateVersion.refBook)
                        .and(qSortPublishedDateVersion.fromDate.eq(JPAExpressions
                                .select(whereVersion.fromDate.max()).from(whereVersion)
                                .where(whereVersion.refBook.eq(QRefBookVersionEntity.refBookVersionEntity.refBook))
                        ))
                );
        return qSortPublishedDateVersion.fromDate;
    }

    /**
     * Формирование предиката на основе критерия поиска.
     *
     * @param criteria критерий поиска
     * @return Предикат для запроса поиска
     */
    public static Predicate toVersionPredicate(VersionCriteria criteria) {

        BooleanBuilder where = new BooleanBuilder();

        if (criteria.getId() != null) {
            where.andNot(RefBookVersionPredicates.hasVersionId(criteria.getId()));
        }

        if (criteria.getRefBookId() != null) {
            where.and(RefBookVersionPredicates.isVersionOfRefBook(criteria.getRefBookId()));
        }

        if (criteria.getRefBookCode() != null) {
            where.and(RefBookVersionPredicates.isVersionOfRefBookCode(criteria.getRefBookCode()));
        }

        if (criteria.getExcludeDraft()) {
            where.andNot(RefBookVersionPredicates.isDraft());
        }

        if (criteria.getVersion() != null) {
            where.and(RefBookVersionPredicates.isVersionNumberContains(criteria.getVersion()));
        }

        return where.getValue();
    }
}
