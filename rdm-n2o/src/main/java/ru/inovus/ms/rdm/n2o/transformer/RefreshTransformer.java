package ru.inovus.ms.rdm.n2o.transformer;

import net.n2oapp.framework.api.metadata.Compiled;
import net.n2oapp.framework.api.metadata.aware.CompiledClassAware;
import net.n2oapp.framework.api.metadata.compile.CompileContext;
import net.n2oapp.framework.api.metadata.compile.CompileProcessor;
import net.n2oapp.framework.api.metadata.compile.CompileTransformer;
import net.n2oapp.framework.api.metadata.meta.action.invoke.InvokeAction;
import net.n2oapp.framework.api.metadata.meta.saga.RefreshSaga;
import net.n2oapp.framework.config.metadata.compile.N2oCompileProcessor;
import org.springframework.stereotype.Component;

/**
 * Обновить master (version_select) виджет вместо detail виджетов
 * Для того чтобы был переход на черновик после изменения версии
 */
@Component
public class RefreshTransformer implements CompileTransformer<InvokeAction, CompileContext<?, ?>>, CompiledClassAware {
    @Override
    public Class<? extends Compiled> getCompiledClass() {
        return InvokeAction.class;
    }

    @Override
    public InvokeAction transform(InvokeAction invokeAction, CompileContext<?, ?> compileContext, CompileProcessor compileProcessor) {
        if (compileContext.getRoute((N2oCompileProcessor) compileProcessor).endsWith("_r")) {
            RefreshSaga refresh = new RefreshSaga();
            refresh.setType(RefreshSaga.Type.widget);
            refresh.getOptions().setWidgetId("main_edit_version_select");
            invokeAction.getMeta().getSuccess().setRefresh(refresh);
            return invokeAction;

        }
        return invokeAction;
    }
}
