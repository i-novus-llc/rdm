package ru.i_novus.ms.rdm.api.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EntityUtilsTest {

    @Test
    public void testToEntityList() {

        final List<String> strings = List.of("s1", "s2", "s3");
        final List<String> actuals = strings.stream().collect(EntityUtils.toEntityList());
        assertNotNull(actuals);
        assertEquals(strings.size(), actuals.size());
        actuals.removeLast();
        assertEquals(strings.size() - 1, actuals.size());
    }
}