package ru.inovus.ms.rdm.rest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.junit4.SpringRunner;
import ru.inovus.ms.rdm.enumeration.RefBookStatus;
import ru.inovus.ms.rdm.service.EnumService;
import ru.inovus.ms.rdm.service.Identifiable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
        "cxf.jaxrs.client.classes-scan=true",
        "cxf.jaxrs.client.classes-scan-packages=ru.inovus.ms.rdm.service",
        "cxf.jaxrs.client.address=http://localhost:${server.port}/rdm/api",
        "server.port=8899"
})
public class EnumServiceTest {

    @Autowired
    private EnumService enumService;

    @Test
    public void testSearchAll() throws ClassNotFoundException {

        Page<Identifiable> search = enumService.search(null, RefBookStatus.class.getName(), null);
        assertEquals(RefBookStatus.values().length, search.getTotalElements());
        search.getContent().forEach(identifiable ->
                assertEquals(RefBookStatus.valueOf(identifiable.getId()).getName(), identifiable.getName()));
    }

    @Test
    public void testSearchById() throws ClassNotFoundException {

        RefBookStatus status = RefBookStatus.DRAFT;
        Page<Identifiable> search = enumService.search(status.getId(), RefBookStatus.class.getName(), null);
        assertEquals(1, search.getTotalElements());
        assertEquals(status.getName(), search.getContent().get(0).getName());
    }

    @Test
    public void testSearchByName() throws ClassNotFoundException {

        RefBookStatus status = RefBookStatus.PUBLISHED;
        String nameSearchStr = status.getName().toLowerCase().substring(0, 3);

        Page<Identifiable> search = enumService.search(null, RefBookStatus.class.getName(), nameSearchStr);
        assertEquals(1, search.getTotalElements());
        Identifiable foundValue = search.getContent().get(0);
        assertEquals(status.getId(), foundValue.getId());
        assertTrue(foundValue.getName().toLowerCase().startsWith(nameSearchStr));
    }
}
