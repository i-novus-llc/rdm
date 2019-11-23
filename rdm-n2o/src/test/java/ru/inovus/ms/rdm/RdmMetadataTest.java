package ru.inovus.ms.rdm;

import net.n2oapp.framework.config.N2oApplicationBuilder;
import net.n2oapp.framework.config.metadata.compile.N2oCompileProcessor;
import net.n2oapp.framework.config.metadata.pack.N2oAllDataPack;
import net.n2oapp.framework.config.metadata.pack.N2oAllPagesPack;
import net.n2oapp.framework.config.metadata.pack.N2oAllValidatorsPack;
import net.n2oapp.framework.config.metadata.pack.N2oHeaderPack;
import net.n2oapp.framework.config.reader.XmlMetadataLoader;
import net.n2oapp.framework.config.register.scanner.XmlInfoScanner;
import net.n2oapp.framework.config.test.N2oTestBase;
import net.n2oapp.properties.test.TestStaticProperties;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Тестирование валидности файлов N2O
 */
public class RdmMetadataTest extends N2oTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RdmMetadataTest.class);

    @Override
    @Before
    public void setUp() throws Exception {
        new TestStaticProperties().setProperties(getCustomProperties());
        super.setUp();
    }

    @Override
    protected void configure(N2oApplicationBuilder b) {
        super.configure(b);
        b.properties("server.servlet.context-path=/");
        b.loaders(new XmlMetadataLoader(b.getEnvironment().getNamespaceReaderFactory()));
        b.packs(new N2oAllDataPack(), new N2oAllPagesPack(), new N2oAllValidatorsPack(), new N2oHeaderPack());
        b.scanners(new XmlInfoScanner());
        builder.scan();
        new N2oCompileProcessor(builder.getEnvironment());
    }

    @Test
    public void validate() {
        builder.getEnvironment().getMetadataRegister().find(i -> true).forEach(i -> {
            try {
                builder.read().validate().get(i.getId(), i.getBaseSourceClass());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                assert false : "Fail on id=" + i.getId() + ", class=" + i.getBaseSourceClass().getSimpleName();
            }
        });
    }

    private Properties getCustomProperties() {

        Properties properties = new Properties();
        properties.put("server.servlet.context-path", "");
        properties.put("rdm.context-path", "/#");
        properties.put("rdm.backend.path", "http://localhost:8080/rdm/api");
        properties.put("rdm.user.admin.url", "http://docker.one:8182/");

        return properties;
    }
}
