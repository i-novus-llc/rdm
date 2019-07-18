package ru.inovus.ms.rdm.queryprovider;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAQuery;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.entity.QRefBookConflictEntity;
import ru.inovus.ms.rdm.entity.RefBookConflictEntity;
import ru.inovus.ms.rdm.model.conflict.DeleteRefBookConflictCriteria;
import ru.inovus.ms.rdm.model.conflict.RefBookConflictCriteria;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.predicate.RefBookConflictPredicates.*;

/**
 * Провайдер запросов на основе критериев.
 */
@Component
public class RefBookConflictQueryProvider {

    public static final int REF_BOOK_CONFLICT_PAGE_SIZE = 100;
    public static final int REF_BOOK_DIFF_CONFLICT_PAGE_SIZE = 100;

    private static final String CONFLICT_REFERRER_VERSION_ID_SORT_PROPERTY = "referrerVersionId";
    private static final String CONFLICT_PUBLISHED_VERSION_ID_SORT_PROPERTY = "publishedVersionId";
    private static final String CONFLICT_REF_RECORD_ID_SORT_PROPERTY = "refRecordId";
    private static final String CONFLICT_REF_FIELD_CODE_SORT_PROPERTY = "refFieldCode";

    private static final List<Sort.Order> SORT_REF_BOOK_CONFLICTS = asList(
            new Sort.Order(Sort.Direction.ASC, CONFLICT_REF_RECORD_ID_SORT_PROPERTY),
            new Sort.Order(Sort.Direction.ASC, CONFLICT_REF_FIELD_CODE_SORT_PROPERTY)
    );

    private static final String CANNOT_ORDER_BY_EXCEPTION_CODE = "cannot.order.by \"{0}\"";

    private EntityManager entityManager;

    @Autowired
    public RefBookConflictQueryProvider(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public static List<Sort.Order> getSortRefBookConflicts() {
        return SORT_REF_BOOK_CONFLICTS;
    }

    public Page<RefBookConflictEntity> search(RefBookConflictCriteria criteria) {
        JPAQuery<RefBookConflictEntity> jpaQuery =
                new JPAQuery<>(entityManager)
                        .select(QRefBookConflictEntity.refBookConflictEntity)
                        .from(QRefBookConflictEntity.refBookConflictEntity)
                        .where(toPredicate(criteria));

        long count = jpaQuery.fetchCount();

        sortQuery(jpaQuery, criteria);
        List<RefBookConflictEntity> entities = jpaQuery
                .offset(criteria.getOffset())
                .limit(criteria.getPageSize())
                .fetch();

        return new PageImpl<>(entities, criteria, count);
    }

    /**
     * Получение количества строк с конфликтами на основании уникальных идентификаторов строк
     *
     * @param criteria критерий поиска
     * @return Количество конфликтных строк
     */
    public Long countConflictedRowIds(RefBookConflictCriteria criteria) {
        return getConflictedRowIdsQuery(criteria).fetchCount();
    }

    /**
     * Поиск идентификаторов строк с конфликтами.
     *
     * @param criteria критерий поиска
     * @return Страница идентификаторов конфликтных строк
     */
    public Page<Long> searchConflictedRowIds(RefBookConflictCriteria criteria) {

        JPAQuery<Long> jpaQuery = getConflictedRowIdsQuery(criteria);

        long count = jpaQuery.fetchCount();

        jpaQuery.orderBy(QRefBookConflictEntity.refBookConflictEntity.refRecordId.asc());

        List<Long> entities = jpaQuery
                .offset(criteria.getOffset())
                .limit(criteria.getPageSize())
                .fetch();

        return new PageImpl<>(entities, criteria, count);
    }

    private JPAQuery<Long> getConflictedRowIdsQuery(RefBookConflictCriteria criteria) {
        return new JPAQuery<>(entityManager)
                .select(QRefBookConflictEntity.refBookConflictEntity.refRecordId)
                .from(QRefBookConflictEntity.refBookConflictEntity)
                .where(toPredicate(criteria))
                .distinct();
    }

    /**
     * Формирование предиката на основе критерия поиска.
     *
     * @param criteria критерий поиска
     * @return Предикат для запроса поиска
     */
    private static Predicate toPredicate(RefBookConflictCriteria criteria) {
        BooleanBuilder where = new BooleanBuilder();

        if (nonNull(criteria.getReferrerVersionId()))
            where.and(isReferrerVersionId(criteria.getReferrerVersionId()));

        if (nonNull(criteria.getReferrerVersionRefBookId()))
            where.and(isReferrerVersionRefBookId(criteria.getReferrerVersionRefBookId()));

        if (nonNull(criteria.getPublishedVersionId()))
            where.and(isPublishedVersionId(criteria.getPublishedVersionId()));

        if (nonNull(criteria.getPublishedVersionRefBookId()))
            where.and(isPublishedVersionRefBookId(criteria.getPublishedVersionRefBookId()));

        if (nonNull(criteria.getRefRecordId()))
            where.and(isRefRecordId(criteria.getRefRecordId()));

        if (!isEmpty(criteria.getRefRecordIds()))
            where.and(isRefRecordIdIn(criteria.getRefRecordIds()));

        if (nonNull(criteria.getRefFieldCode()))
            where.and(isRefFieldCode(criteria.getRefFieldCode()));

        if (nonNull(criteria.getConflictType()))
            where.and(isConflictType(criteria.getConflictType()));

        return where.getValue();
    }

    /**
     * Добавление сортировки в запрос на основе критерия.
     *
     * @param jpaQuery запрос
     * @param criteria критерий поиска
     */
    private static void sortQuery(JPAQuery<RefBookConflictEntity> jpaQuery, RefBookConflictCriteria criteria) {

        List<Sort.Order> orders = criteria.getOrders();

        if (!isEmpty(orders)) {
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
    private static void addSortOrder(JPAQuery<RefBookConflictEntity> jpaQuery, Sort.Order order) {

        ComparableExpressionBase sortExpression;

        switch (order.getProperty()) {
            case CONFLICT_REFERRER_VERSION_ID_SORT_PROPERTY:
                sortExpression = QRefBookConflictEntity.refBookConflictEntity.referrerVersion.id;
                break;

            case CONFLICT_PUBLISHED_VERSION_ID_SORT_PROPERTY:
                sortExpression = QRefBookConflictEntity.refBookConflictEntity.publishedVersion.id;
                break;

            case CONFLICT_REF_RECORD_ID_SORT_PROPERTY:
                sortExpression = QRefBookConflictEntity.refBookConflictEntity.refRecordId;
                break;

            case CONFLICT_REF_FIELD_CODE_SORT_PROPERTY:
                sortExpression = QRefBookConflictEntity.refBookConflictEntity.refFieldCode;
                break;

            default:
                throw new UserException(new Message(CANNOT_ORDER_BY_EXCEPTION_CODE, order.getProperty()));
        }

        jpaQuery.orderBy(order.isAscending() ? sortExpression.asc() : sortExpression.desc());
    }

    /**
     * Удаление конфликтов по заданному критерию.
     *
     * @param criteria критерий удаления
     */
    public void delete(DeleteRefBookConflictCriteria criteria) {

        JPADeleteClause jpaDelete =
                new JPADeleteClause(entityManager, QRefBookConflictEntity.refBookConflictEntity)
                        .where(QRefBookConflictEntity.refBookConflictEntity.id.in(
                                JPAExpressions.select(QRefBookConflictEntity.refBookConflictEntity.id)
                                        .from(QRefBookConflictEntity.refBookConflictEntity)
                                        .where(toDeletionPredicate(criteria))
                        ));
        jpaDelete.execute();
    }

    /**
     * Формирование предиката на основе критерия удаления.
     *
     * @param criteria критерий удаления
     * @return Предикат для удаления
     */
    private static Predicate toDeletionPredicate(DeleteRefBookConflictCriteria criteria) {

        BooleanBuilder where = new BooleanBuilder();

        if (nonNull(criteria.getReferrerVersionId()))
            where.and(isReferrerVersionId(criteria.getReferrerVersionId()));

        if (nonNull(criteria.getReferrerVersionRefBookId()))
            where.and(isReferrerVersionRefBookId(criteria.getReferrerVersionRefBookId()));

        if (nonNull(criteria.getPublishedVersionId()))
            where.and(isPublishedVersionId(criteria.getPublishedVersionId()));

        if (nonNull(criteria.getPublishedVersionRefBookId()))
            where.and(isPublishedVersionRefBookId(criteria.getPublishedVersionRefBookId()));

        if (nonNull(criteria.getExcludedPublishedVersionId()))
            where.andNot(isPublishedVersionId(criteria.getExcludedPublishedVersionId()));

        if (nonNull(criteria.getRefFieldCode()))
            where.and(isRefFieldCode(criteria.getRefFieldCode()));

        if (nonNull(criteria.getConflictType()))
            where.and(isConflictType(criteria.getConflictType()));

        return where.getValue();
    }

}
