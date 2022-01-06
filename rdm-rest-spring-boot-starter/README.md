# Стартер UI

Модуль используется при подключении RDM REST API.

## Подключение RDM REST API

1. Добавьте зависимость в `pom.xml`:
    ```xml
    <dependency>
      <groupId>ru.i-novus.ms.rdm</groupId>
        <artifactId>rdm-rest-spring-boot-starter</artifactId>
        <version>${rdm.version}</version>
    </dependency>
    ```
  Здесь `rdm.version` - подключаемая версия RDM.

2. Настроить data source любым доступным для spring boot образом
3. Настроить [n2o-platform-starter-jwt](https://github.com/i-novus-llc/n2o-platform/tree/master/n2o-platform-security)
4. Настроить [n2o-platform-starter-audit](https://github.com/i-novus-llc/n2o-platform/tree/master/n2o-platform-audit)

## Список настроек стартера

| Настройка                                                             | Значение по умолчанию                | Описание                                                                                                                             |
|-----------------------------------------------------------------------|--------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| rdm.validation-errors-count                                           | 99                                   | Кол-во ошибок при загрузке данных справочника, при достижения этого значения дальше не проверяется                                   |
| rdm.loader.enabled                                                    | true                                 | Включение/выключения лоадера справочников                                                                                            |
| rdm.loader.max.file-size                                              | 50000000                             | Размер(в байтах) файла для лоадера                                                                                                   |
| rdm.download.passport-enable                                          | true                                 | ???                                                                                                                                  |
| rdm.download.passport.head                                            | fullName                             | ???                                                                                                                                  |
| rdm.compare.data.diff.max.size                                        | 1000                                 | Ограничение на размер разницы между данными.                                                                                         |
| rdm.publish.topic                                                     | publish_topic                        | Название топика, по которому вещаются события публикации справочника.                                                                |
| rdm.enable.publish.topic                                              | false                                | Включение/выключение отправки события публикации справочника  в брокер                                                               |
| rdm.audit.application.name                                            | rdm                                  | Наименование системы для отправки в аудит                                                                                            |
| rdm.async.operation.queue                                             | RDM-INTERNAL-ASYNC-OPERATION-QUEUE   | Наименование очереди для асинхронных операций                                                                                        |
