package ru.inovus.ms.mdm.service;

import ru.inovus.ms.mdm.model.DictionaryCreateRequest;
import ru.inovus.ms.mdm.model.Dictionary;
import ru.inovus.ms.mdm.model.DictionaryCriteria;

import java.util.List;

public interface DictionaryService {

    List<Dictionary> search(DictionaryCriteria dictionaryCriteria);

    Dictionary create(DictionaryCreateRequest dictionaryCreateRequest);

}
