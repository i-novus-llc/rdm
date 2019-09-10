package ru.inovus.ms.rdm.transformer;

import net.n2oapp.framework.api.metadata.Compiled;
import net.n2oapp.framework.api.metadata.ReduxModel;
import net.n2oapp.framework.api.metadata.aware.CompiledClassAware;
import net.n2oapp.framework.api.metadata.compile.CompileContext;
import net.n2oapp.framework.api.metadata.compile.CompileProcessor;
import net.n2oapp.framework.api.metadata.compile.CompileTransformer;
import net.n2oapp.framework.api.metadata.meta.ModelLink;
import net.n2oapp.framework.api.metadata.meta.control.ValidationType;
import net.n2oapp.framework.api.metadata.meta.widget.table.Table;
import net.n2oapp.framework.api.metadata.meta.widget.toolbar.ButtonCondition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Блокировка кнопки Изменить в данных справочника, в зависимости от того, есть ли данные в таблице или нет
 */
@Component
public class UpdateDependencyTransformer implements CompileTransformer<Table, CompileContext<?, ?>>, CompiledClassAware {
    @Override
    public Class<? extends Compiled> getCompiledClass() {
        return Table.class;
    }

    @Override
    public Table transform(Table table, CompileContext<?, ?> compileContext, CompileProcessor compileProcessor) {
        if (table.getId().endsWith("_dataTable") && table.getToolbar() != null) {
            List<ButtonCondition> buttonConditions = table.getToolbar().get("topRight").get(0).getButtons().get(1)
                    .getConditions().get(ValidationType.enabled);
            if (buttonConditions == null) {
                buttonConditions = new ArrayList<>();
            }
            Optional<ButtonCondition> buttonCondition = buttonConditions.stream().filter(c -> "!_.isEmpty(this)".equals(c.getExpression())).findAny();
            if (buttonCondition.isPresent()) {
                buttonCondition.get().setModelLink(new ModelLink(ReduxModel.RESOLVE, table.getId()).getBindLink());
            } else {
                ButtonCondition condition = new ButtonCondition();
                condition.setExpression("!_.isEmpty(this)");
                condition.setModelLink(new ModelLink(ReduxModel.RESOLVE, table.getId()).getBindLink());
                buttonConditions.add(condition);
            }
        }
        return table;
    }
}
