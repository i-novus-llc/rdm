package ru.inovus.ms.rdm.impl.util.mappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;

/**
 * Не строгий на тип маппер
 * Пытается привести значения полей колонки к нужному типу из структуры.
 * Если привести не удалось, оставляет значение строкой
 */
public class NonStrictOnTypeRowMapper extends StructureRowMapper {

    private static final Logger logger = LoggerFactory.getLogger(NonStrictOnTypeRowMapper.class);

    public NonStrictOnTypeRowMapper(Structure structure, RefBookVersionRepository versionRepository) {
        super(structure, versionRepository);
    }

    @Override
    public Row map(Row inputRow) {
        inputRow.getData().entrySet()
                .forEach(e -> {
                    Object value = castValue(structure.getAttribute(e.getKey()), e.getValue());
                    if (e.getValue() != value) {
                        e.setValue(value);
                    }
                });
        return inputRow;
    }

    @Override
    protected Object castValue(Structure.Attribute attribute, Object value) {
        try {
            return super.castValue(attribute, value);

        } catch (Exception e) {
            logger.error("value can not be casted to a needed type", e);
            return value;
        }
    }
}
