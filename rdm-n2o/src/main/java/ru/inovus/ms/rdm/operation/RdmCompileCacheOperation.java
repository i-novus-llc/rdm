package ru.inovus.ms.rdm.operation;

import net.n2oapp.cache.template.CacheTemplate;
import net.n2oapp.criteria.dataset.DataSet;
import net.n2oapp.framework.api.metadata.compile.CompileContext;
import net.n2oapp.framework.api.metadata.compile.CompileProcessor;
import net.n2oapp.framework.config.compile.pipeline.operation.CompileCacheOperation;

import java.util.function.Supplier;

/**
 * Переопределение CompileCacheOperation, чтобы не кэшировать динамические метаданные dataRecord
 */
public class RdmCompileCacheOperation<S> extends CompileCacheOperation<S> {

    public RdmCompileCacheOperation() {
    }

    public RdmCompileCacheOperation(CacheTemplate cacheTemplate) {
        super(cacheTemplate);
    }

    @Override
    public S execute(CompileContext<?,?> context, DataSet data, Supplier<S> supplier, CompileProcessor processor) {
        String sourceId = context.getSourceId(processor);
        if (sourceId.startsWith("dataRecord")) {
            return supplier.get();
        }
        return super.execute(context, data, supplier, processor);
    }
}
