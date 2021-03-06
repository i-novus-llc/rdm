# Создание справочников через механизм лоадеров
  
RDM поддерживает загрузку справочников из xml-файлов с использованием механизма `n2o-platform-loader`.
Файл справочника в формате xml можно получить в результате выгрузки этого справочника в рамках RDM.

При выполнении загрузки справочников выполняется их создание и публикация.
Если справочник уже существует, то загрузка не выполняется.

## Подключение
* Добавьте зависимость:
```xml
<dependency>
  <groupId>ru.i-novus.ms.rdm</groupId>
  <artifactId>rdm-loader-client</artifactId>
</dependency>
```

* Создайте файл конфигурации (например, `rdm.json`) в формате:
```json
[
  {
    "code" : "код справочника",
    "file" : "путь к файлу справочника в ресурсах"
  },
  ...
]  
```
Файлы справочников должны находиться в ресурсах клиента.

* Настройте параметры конфигурации в `application.properties` клиента:
  - `rdm.loader.client.url` -- адрес REST-сервиса RDM. По умолчанию "http://docker.one:8807/rdm/api".
  - `rdm.loader.client.subject` -- владелец справочников. По умолчанию "client".
  - `rdm.loader.client.file.path` -- путь к файлу конфигурации. По умолчанию "rdm.json".
  - `rdm.loader.client.enabled` -- разрешает загрузку справочников через механизм лоадеров. По умолчанию "true".

Параметры `rdm.loader.client.url` и `rdm.loader.client.subject` обязательны для корректной работы.
Параметр `rdm.loader.client.file.path` не обязателен, если путь к файлу конфигурации соответствует "rdm.json".
Параметр `rdm.loader.client.enabled` позволяет при необходимости отключать загрузку справочников.
