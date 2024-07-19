package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.framework.api.MetadataEnvironment;
import net.n2oapp.framework.api.metadata.compile.CompileContext;
import net.n2oapp.framework.api.metadata.control.N2oField;
import net.n2oapp.framework.api.metadata.meta.control.Control;
import net.n2oapp.framework.api.metadata.meta.control.StandardField;
import net.n2oapp.framework.api.metadata.pipeline.CompilePipeline;
import net.n2oapp.framework.config.compile.pipeline.N2oPipelineSupport;
import net.n2oapp.framework.config.metadata.compile.IndexScope;
import net.n2oapp.framework.config.metadata.compile.context.WidgetContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataFieldFilterProvider {

    @Autowired
    private MetadataEnvironment environment;

    public StandardField<Control> toFilterField(N2oField n2oField) {

        final CompilePipeline pipeline = N2oPipelineSupport.compilePipeline(environment);
        final CompileContext<?, ?> context = new WidgetContext("");

        final StandardField<Control> result = pipeline.compile().get(n2oField, context, new IndexScope());
        result.setId(n2oField.getId());

        return result;
    }
}
