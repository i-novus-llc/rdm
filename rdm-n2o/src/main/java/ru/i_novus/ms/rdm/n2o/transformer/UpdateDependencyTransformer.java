package ru.i_novus.ms.rdm.n2o.transformer;

import net.n2oapp.framework.api.metadata.Compiled;
import net.n2oapp.framework.api.metadata.ReduxModel;
import net.n2oapp.framework.api.metadata.aware.CompiledClassAware;
import net.n2oapp.framework.api.metadata.compile.CompileContext;
import net.n2oapp.framework.api.metadata.compile.CompileProcessor;
import net.n2oapp.framework.api.metadata.compile.CompileTransformer;
import net.n2oapp.framework.api.metadata.meta.ModelLink;
import net.n2oapp.framework.api.metadata.meta.control.ValidationType;
import net.n2oapp.framework.api.metadata.meta.toolbar.Toolbar;
import net.n2oapp.framework.api.metadata.meta.widget.table.Table;
import net.n2oapp.framework.api.metadata.meta.widget.toolbar.AbstractButton;
import net.n2oapp.framework.api.metadata.meta.widget.toolbar.ButtonCondition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Блокировка кнопки "Изменить" в данных справочника
 * в зависимости от того, есть ли данные в таблице или нет.
 */
@Component
public class UpdateDependencyTransformer
        implements CompileTransformer<Table, CompileContext<?, ?>>, CompiledClassAware {

    // Список суффиксов таблиц, для которых нужно выполнить transform.
    private static final List<String> UPDATED_TABLE_SUFFIXES = asList("_dataTable", "_dataTableWithConflicts");

    // Аффиксы кнопок, для которых нужно выполнить transform.
    private static final String BUTTON_PREFIX = "update";
    private static final String BUTTON_SUFFIX = "Record_r";
    // Выражение для условий кнопок.
    private static final String BUTTON_CONDITION_EXPRESSION = "!_.isEmpty(this)";

    @Override
    public Class<? extends Compiled> getCompiledClass() {
        return Table.class;
    }

    @Override
    public Table transform(Table table, CompileContext<?, ?> compileContext, CompileProcessor compileProcessor) {

        String tableId = table.getId();
        if (UPDATED_TABLE_SUFFIXES.stream().noneMatch(tableId::endsWith)) {
            return table;
        }

        Toolbar toolbar = table.getToolbar();
        if (toolbar == null || toolbar.get("topRight") == null) {
            return table;
        }

        List<AbstractButton> buttons = toolbar.get("topRight").get(0).getButtons();
        buttons.stream()
                .filter(button ->
                        button.getId().startsWith(BUTTON_PREFIX)
                                && button.getId().endsWith(BUTTON_SUFFIX))
                .forEach(button -> transformButton(tableId, button));

        return table;
    }

    private void transformButton(String tableId, AbstractButton button) {

        List<ButtonCondition> conditions = button.getConditions().get(ValidationType.enabled);
        if (conditions == null) {
            conditions = new ArrayList<>();
            button.getConditions().putIfAbsent(ValidationType.enabled, conditions);
        }

        ButtonCondition condition = conditions.stream()
                .filter(c -> BUTTON_CONDITION_EXPRESSION.equals(c.getExpression()))
                .findAny().orElse(null);

        if (condition != null) {
            condition.setModelLink(new ModelLink(ReduxModel.RESOLVE, tableId).getBindLink());

        } else {
            condition = new ButtonCondition();
            condition.setExpression(BUTTON_CONDITION_EXPRESSION);
            condition.setModelLink(new ModelLink(ReduxModel.RESOLVE, tableId).getBindLink());
            conditions.add(condition);
        }
    }
}
