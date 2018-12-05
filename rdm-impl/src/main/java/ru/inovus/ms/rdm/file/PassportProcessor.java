package ru.inovus.ms.rdm.file;

import ru.inovus.ms.rdm.model.Result;

import java.util.Map;

public interface PassportProcessor {

    Result process(Map<String, String> passport);

}
