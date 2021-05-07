package ru.i_novus.ms.rdm.impl.strategy.file;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.impl.file.FileStorage;

import java.io.InputStream;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultSupplyPathFileContentStrategyTest {

    private static final String FILE_PATH = "/refBook_1.0.xml";

    @InjectMocks
    private DefaultSupplyPathFileContentStrategy strategy;

    @Mock
    private FileStorage fileStorage;

    @Test
    public void testSupply() {
        
        InputStream is = mock(InputStream.class);
        when(fileStorage.getContent(FILE_PATH)).thenReturn(is);

        Supplier<InputStream> supplier = strategy.supply(FILE_PATH);
        assertNotNull(supplier);
        assertEquals(is,supplier.get());
    }
}