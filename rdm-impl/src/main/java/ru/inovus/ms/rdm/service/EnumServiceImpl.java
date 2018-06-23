package ru.inovus.ms.rdm.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import ru.kirkazan.common.exception.CodifiedException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Qualifier("impl")
public class EnumServiceImpl implements EnumService {

    @Override
    public Page<Identifiable> search(Integer id, String enumClass, String name) throws ClassNotFoundException {
        Class<?> aClass = Class.forName(enumClass);
        if (!Identifiable.class.isAssignableFrom(aClass)) {
            throw new CodifiedException("Enum " + enumClass + "doesn't implement Identifiable");
        }
        List<Identifiable> collection = Arrays.stream(aClass.getEnumConstants())
                .map(Identifiable.class::cast)
                .filter(v -> id == null || v.getId().equals(id))
                .filter(v -> StringUtils.isBlank(name) || v.getName().trim().toLowerCase().startsWith(name.toLowerCase()))
                .collect(Collectors.toList());
        return new PageImpl<>(collection);

    }
}
