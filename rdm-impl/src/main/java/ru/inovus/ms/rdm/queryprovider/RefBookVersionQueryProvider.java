package ru.inovus.ms.rdm.queryprovider;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.inovus.ms.rdm.entity.QPassportValueEntity;
import ru.inovus.ms.rdm.entity.QRefBookVersionEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookSourceType;
import ru.inovus.ms.rdm.model.refbook.RefBookCriteria;
import ru.inovus.ms.rdm.model.version.VersionCriteria;
import ru.inovus.ms.rdm.predicate.PassportPredicateProducer;

import javax.persistence.EntityManager;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.inovus.ms.rdm.predicate.RefBookVersionPredicates.*;

@Component
public class RefBookVersionQueryProvider {

    private static final String CANNOT_ORDER_BY_EXCEPTION_CODE = "cannot.order.by \"{0}\"";

    private static final String PASSPORT_SORT_PREFIX = "passport";
    private static final String VERSION_ID_SORT_PROPERTY = "id";
    private static final String REF_BOOK_ID_SORT_PROPERTY = "refBookId";
    private static final String REF_BOOK_CODE_SORT_PROPERTY = "code";
    private static final String REF_BOOK_DISPLAY_CODE_SORT_PROPERTY = "displayCode";
    private static final String REF_BOOK_LAST_PUBLISH_DATE_SORT_PROPERTY = "lastPublishedVersionFromDate";
    public static final String REF_BOOK_FROM_DATE_SORT_PROPERTY = "fromDate";
    private static final String REF_BOOK_CATEGORY_SORT_PROPERTY = "category";

    private PassportPredicateProducer passportPredicateProducer;

    private EntityManager entityManager;

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

        if (!isEmpty(criteria.getCode()))
            where.and(isCodeContains(criteria.getCode()));

        if (nonNull(criteria.getExcludeByVersionId()))
            where.andNot(refBookHasVersion(criteria.getExcludeByVersionId()));

        if (!CollectionUtils.isEmpty(criteria.getPassport()))
            where.and(passportPredicateProducer.toPredicate(criteria.getPassport()));

        if (!isEmpty(criteria.getCategory()))
            where.and(refBookHasCategory(criteria.getCategory()));

        if (!CollectionUtils.isEmpty(criteria.getRefBookIds()))
            where.and(isVersionOfRefBooks(criteria.getRefBookIds()));

        if (criteria.getIsArchived())
            where.and(isArchived());

        else if (criteria.getNonArchived())
            where.andNot(isArchived());
    }

    /**
     * Заполнение предиката по параметрам для версии справочника на основе критерия поиска.
     *
     * @param criteria критерий поиска
     * @param where    предикат для запроса поиска
     */
    private void fillRefBookVersionPredicate(RefBookCriteria criteria, BooleanBuilder where) {

        where.and(isSourceType(getSourceType(criteria)));

        if (nonNull(criteria.getFromDateBegin()))
            where.and(isMaxFromDateEqOrAfter(criteria.getFromDateBegin()));

        if (nonNull(criteria.getFromDateEnd()))
            where.and(isMaxFromDateEqOrBefore(criteria.getFromDateEnd()));

        if (criteria.getHasPublished())
            where.andNot(isArchived()).and(isAnyPublished());

        if (criteria.getHasDraft())
            where.andNot(isArchived()).and(refBookHasDraft());

        if (criteria.getHasPublishedVersion())
            where.andNot(isArchived()).and(hasLastPublishedVersion());

        if (criteria.getHasPrimaryAttribute())
            where.and(hasStructure()).and(hasPrimaryAttribute());
    }

    private RefBookSourceType getSourceType(RefBookCriteria criteria) {

        RefBookSourceType sourceType = criteria.getSourceType();
        if (criteria.getIncludeVersions()) {
            // Если ищется последняя версия справочника, то выбирается требуемая последняя версия.
            if (criteria.getExcludeDraft()
                    && (isNull(sourceType) || RefBookSourceType.LAST_VERSION.equals(sourceType)))
                sourceType = RefBookSourceType.LAST_PUBLISHED;

        } else {
            // Если ищется список справочников, то выбираются только требуемые последние версии.
            if (isNull(sourceType))
                sourceType = RefBookSourceType.LAST_VERSION;

            if (criteria.getExcludeDraft()
                    && RefBookSourceType.LAST_VERSION.equals(sourceType))
                sourceType = RefBookSourceType.LAST_PUBLISHED;
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
            jpaQuery.orderBy(getOrderByLastPublishDateExpression(jpaQuery).asc());

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
            sortExpression = getOrderByLastPublishDateExpression(jpaQuery);
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
            case VERSION_ID_SORT_PROPERTY:
                return QRefBookVersionEntity.refBookVersionEntity.id;

            case REF_BOOK_ID_SORT_PROPERTY:
                return QRefBookVersionEntity.refBookVersionEntity.refBook.id;

            case REF_BOOK_CODE_SORT_PROPERTY:
            case REF_BOOK_DISPLAY_CODE_SORT_PROPERTY:
                return QRefBookVersionEntity.refBookVersionEntity.refBook.code;

            case REF_BOOK_FROM_DATE_SORT_PROPERTY:
                return QRefBookVersionEntity.refBookVersionEntity.fromDate;

            case REF_BOOK_CATEGORY_SORT_PROPERTY:
                return QRefBookVersionEntity.refBookVersionEntity.refBook.category;

            default:
                throw new UserException(new Message(CANNOT_ORDER_BY_EXCEPTION_CODE, orderProperty));
        }
    }

    private ComparableExpressionBase getOrderByLastPublishDateExpression(JPAQuery<RefBookVersionEntity> jpaQuery) {
        QRefBookVersionEntity qSortFromDateVersion = new QRefBookVersionEntity("sort_from_date");
        QRefBookVersionEntity whereVersion = new QRefBookVersionEntity("sort_max_version");

        jpaQuery.leftJoin(qSortFromDateVersion)
                .on(QRefBookVersionEntity.refBookVersionEntity.refBook.eq(qSortFromDateVersion.refBook)
                        .and(qSortFromDateVersion.fromDate.eq(JPAExpressions
                                .select(whereVersion.fromDate.max()).from(whereVersion)
                                .where(whereVersion.refBook.eq(QRefBookVersionEntity.refBookVersionEntity.refBook)))));
        return qSortFromDateVersion.fromDate;
    }

    /**
     * Формирование предиката на основе критерия поиска.
     *
     * @param criteria критерий поиска
     * @return Предикат для запроса поиска
     */
    public static Predicate toVersionPredicate(VersionCriteria criteria) {
        BooleanBuilder where = new BooleanBuilder();

        where.and(isVersionOfRefBook(criteria.getRefBookId()));

        if (criteria.getExcludeDraft())
            where.andNot(isDraft());

        if (nonNull(criteria.getVersion()))
            where.and(isVersionNumberContains(criteria.getVersion()));

        return where.getValue();
    }
}
