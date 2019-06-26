package ru.inovus.ms.rdm.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.model.refdata.Row;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

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
        inputRow.getData().forEach((name, value) ->
                inputRow.getData().put(name,
                        castValue(structure.getAttribute(name), value != null ? String.valueOf(value) : null))
        );
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
