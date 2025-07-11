# Загрузка справочников через механизм лоадеров
  
RDM поддерживает загрузку справочников из xml-файлов с использованием механизма `n2o-platform-loader`.
Файл справочника в формате xml можно получить в результате выгрузки этого справочника в рамках RDM.

При выполнении загрузки справочников выполняется их создание/обновление и публикация.

## Подключение
* Добавьте зависимость:
```xml
<dependency>
  <groupId>ru.i-novus.ms.rdm</groupId>
  <artifactId>rdm-loader-client-starter</artifactId>
</dependency>
```

* Создайте файл конфигурации (например, `rdm.json`) в формате:
```json
[
  {
    "change_set_id": "идентификатор изменения справочника",
    "update_type": "тип изменения справочника",
    "code" : "код загружаемого справочника",
    ...
  },
  ...
]  
```

* Настройте параметры конфигурации в `application.properties` клиента:
  - `rdm.loader.client.url` -- адрес REST-сервиса RDM. По умолчанию "http://docker.one:8807/rdm/api".
  - `rdm.loader.client.subject` -- владелец справочников. По умолчанию "client".
  - `rdm.loader.client.file-path` -- путь к файлу конфигурации. По умолчанию "rdm.json".
  - `rdm.loader.client.enabled` -- разрешает загрузку справочников через механизм лоадеров. По умолчанию "true".

Параметры `rdm.loader.client.url` и `rdm.loader.client.subject` обязательны для корректной работы.
Параметр `rdm.loader.client.file-path` не обязателен, если путь к файлу конфигурации соответствует "`rdm.json`".
Параметр `rdm.loader.client.enabled` позволяет при необходимости отключать загрузку справочников.

## Формат файла конфигурации

Файл конфигурации содержит список записей о загружаемых/изменяемых справочниках.
Поддерживается один формат записи о справочнике - с указанием файла справочника, который должен быть в ресурсах клиента.

Формат записи с указанием файла:  
```json
[
  {
    "change_set_id": "идентификатор изменения справочника",
    "update_type": "тип изменения справочника",
    "code" : "код справочника",
    "file" : "путь к файлу справочника в ресурсах"
  },
  ...
]  
```

При изменении справочника необходимо изменить значение в поле `change_set_id`, чтобы справочник изменился в rdm.

Значение в поле `update_type` определяет необходимость внесения изменений в справочник:
- `create_only`: изменения будут внесены только при создании справочника (по умолчанию),
- `skip_on_draft`: если есть черновик, то изменения не будут внесены в справочник,
- `force_update`: если есть черновик, то сначала он будет удалён, затем изменения будут внесены в справочник.

Поле `code` является обязательным.

## Журналирование загрузки

При успешной загрузке справочника информация о загрузке сохраняется в отдельную таблицу `2o_rdm_management.ref_book_data_load_log`.
Также эта таблица используется для исключения повторной загрузки справочника.

Проверка уникальности загрузки обеспечивается парой полей - `code` и `change_set_id`.
Поэтому при необходимости повторной загрузки справочника необходимости изменить `change_set_id` и `update_type`.

Идентификатор загруженного справочника сохраняется в поле `ref_book_id`, а дата загрузки (и публикации) - в `executed_date`.

Также процесс загрузки справочников можно проверить по логам, которые можно включить настройкой `logging.level.ru.i_novus.ms.rdm.impl.service.loader`=`TRACE`.
