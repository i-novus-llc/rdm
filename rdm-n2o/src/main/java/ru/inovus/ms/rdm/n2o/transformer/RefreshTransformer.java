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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Обновление master-виджета (version_select) вместо detail-виджета.
 * Это необходимо для того, чтобы был переход на черновик после изменения версии.
 */
@Component
public class RefreshTransformer
        implements CompileTransformer<InvokeAction, CompileContext<?, ?>>, CompiledClassAware {

    private static final String REFRESHED_ID_SUFFIX = "_r";
    private static final String DEFAULT_WIDGET_ID = "main_edit_version_select";
    private static final String WIDGET_ID_REGEX = "^.+/:(.*?" + DEFAULT_WIDGET_ID + ")_id/.+" + REFRESHED_ID_SUFFIX + "$";
    private static final Pattern WIDGET_ID_PATTERN = Pattern.compile(WIDGET_ID_REGEX);

    @Override
    public Class<? extends Compiled> getCompiledClass() {
        return InvokeAction.class;
    }

    @Override
    public InvokeAction transform(InvokeAction invokeAction, CompileContext<?, ?> compileContext, CompileProcessor compileProcessor) {

        String route = compileContext.getRoute((N2oCompileProcessor) compileProcessor);
        setRefreshSaga(invokeAction, route);
        return invokeAction;
    }

    private void setRefreshSaga(InvokeAction invokeAction, String route) {

        if (!route.endsWith(REFRESHED_ID_SUFFIX))
            return;

        RefreshSaga refresh = new RefreshSaga();
        refresh.setType(RefreshSaga.Type.widget);

        Matcher matcher = WIDGET_ID_PATTERN.matcher(route);
        String refreshedWidgetId = matcher.matches() ? matcher.group(1) : DEFAULT_WIDGET_ID;
        refresh.getOptions().setWidgetId(refreshedWidgetId);

        invokeAction.getMeta().getSuccess().setRefresh(refresh);
    }
}
