package ru.inovus.ms.rdm.esnsi;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.mockito.internal.util.reflection.FieldSetter.setField;

@RunWith(MockitoJUnitRunner.class)
public class EsnsiSyncTest {

    private EsnsiIntegrationService integrationService = new EsnsiIntegrationService();

    @Test
    public void main() {
        setField(integrationService, getField(integrationService, "codes"), List.of("01-519"));
        integrationService.runIntegration();
    }

    private Field getField(Object from, String name) {
        try {
            return from.getClass().getField(name);
        } catch (NoSuchFieldException e) {
            fail();
            return null;
        }
    }

}
