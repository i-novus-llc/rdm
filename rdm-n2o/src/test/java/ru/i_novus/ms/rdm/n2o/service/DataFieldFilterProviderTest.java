package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.framework.api.MetadataEnvironment;
import net.n2oapp.framework.api.metadata.compile.CompileContext;
import net.n2oapp.framework.api.metadata.control.N2oField;
import net.n2oapp.framework.api.metadata.control.plain.N2oInputText;
import net.n2oapp.framework.api.metadata.meta.control.Control;
import net.n2oapp.framework.api.metadata.meta.control.StandardField;
import net.n2oapp.framework.api.metadata.pipeline.CompileBindTerminalPipeline;
import net.n2oapp.framework.api.metadata.pipeline.CompilePipeline;
import net.n2oapp.framework.api.metadata.pipeline.CompileTerminalPipeline;
import net.n2oapp.framework.api.metadata.pipeline.PipelineOperationFactory;
import net.n2oapp.framework.config.compile.pipeline.N2oPipelineSupport;
import net.n2oapp.framework.config.metadata.compile.IndexScope;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.FILTER_PREFIX;

@RunWith(MockitoJUnitRunner.class)
public class DataFieldFilterProviderTest {

    @InjectMocks
    private DataFieldFilterProvider provider;

    @Mock
    private MetadataEnvironment environment;

    @Mock
    private CompilePipeline pipeline;

    @Mock
    private CompileTerminalPipeline<CompileBindTerminalPipeline> bindTerminalPipeline;

    @Mock
    private PipelineOperationFactory pipelineOperationFactory;

    @Test
    public void testToFilterField() {

        try(MockedStatic<N2oPipelineSupport> n2oPipelineSupport = mockStatic(N2oPipelineSupport.class)) {

            n2oPipelineSupport.when(() -> N2oPipelineSupport.compilePipeline(environment)).thenReturn(pipeline);
            when(pipeline.compile()).thenReturn(bindTerminalPipeline);

            final N2oField n2oField = new N2oInputText();
            n2oField.setId(FILTER_PREFIX + "id");

            final StandardField<Control> standardField = new StandardField<>();
            when(bindTerminalPipeline.get(eq(n2oField), any(CompileContext.class), any(IndexScope.class))).thenReturn(standardField);

            final StandardField<Control> result = provider.toFilterField(n2oField);
            assertNotNull(result);
            assertEquals(n2oField.getId(), result.getId());
        }
    }
}