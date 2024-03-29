package ru.i_novus.ms.rdm;

import net.n2oapp.framework.api.metadata.io.IOProcessorAware;
import net.n2oapp.framework.api.metadata.reader.NamespaceReaderFactory;
import net.n2oapp.framework.config.N2oApplicationBuilder;
import net.n2oapp.framework.config.io.IOProcessorImpl;
import net.n2oapp.framework.config.metadata.compile.N2oCompileProcessor;
import net.n2oapp.framework.config.metadata.pack.*;
import net.n2oapp.framework.config.reader.XmlMetadataLoader;
import net.n2oapp.framework.config.register.scanner.XmlInfoScanner;
import net.n2oapp.framework.config.test.N2oTestBase;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Тестирование валидности файлов N2O
 */
public class RdmMetadataTest extends N2oTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RdmMetadataTest.class);

    private static final List<String> RDM_CUSTOM_PROPERTIES = asList(
            "server.servlet.context-path= ",
            "rdm.context-path=/#",
            "rdm.backend.path=http://localhost:8080/rdm/api",
            "rdm.user.admin.url=http://docker.one:8182/",
            "rdm.permissions.refbook.status.list= ",
            "rdm.l10n.support=false"
    );

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void configure(N2oApplicationBuilder builder) {
        super.configure(builder);

        customProperties(builder);
        builder.loaders(new XmlMetadataLoader(builder.getEnvironment().getNamespaceReaderFactory()));
        builder.packs(new N2oAllDataPack(), new N2oAllPagesPack(), new N2oAllValidatorsPack(), new N2oApplicationPack());
        builder.scanners(new XmlInfoScanner());
        builder.scan();

        new N2oCompileProcessor(builder.getEnvironment());
    }

    @Test
    public void validate() {
        builder.getEnvironment().getMetadataRegister()
                .find(i -> !"rdm".equals(i.getId()))
                .forEach(i -> {
            try {
                builder.read().validate().get(i.getId(), i.getBaseSourceClass());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                assert false : "Fail on id=" + i.getId() + ", class=" + i.getBaseSourceClass().getSimpleName();
            }
        });
    }

    private void customProperties(N2oApplicationBuilder builder) {

        RDM_CUSTOM_PROPERTIES.forEach(builder::properties);

        NamespaceReaderFactory readerFactory = builder.getEnvironment().getNamespaceReaderFactory();
        IOProcessorImpl processor = new IOProcessorImpl(readerFactory);
        ((IOProcessorAware) readerFactory).setIOProcessor(processor);
        processor.setSystemProperties(builder.getEnvironment().getSystemProperties());
    }
}
