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

2. Настройте data source любым доступным для spring boot образом.
  
3. Настройте [n2o-platform-starter-jwt](https://github.com/i-novus-llc/n2o-platform/tree/master/n2o-platform-security)

4. Настройте [n2o-platform-starter-audit](https://github.com/i-novus-llc/n2o-platform/tree/master/n2o-platform-audit)

## Список настроек стартера

| Настройка                                             | Значение по умолчанию              | Описание                                                                                           |
|-------------------------------------------------------|------------------------------------|----------------------------------------------------------------------------------------------------|
| rdm.validation-errors-count                           | 99                                 | Кол-во ошибок при загрузке данных справочника, при достижении этого значения дальше не проверяется |
| rdm.loader.enabled                                    | true                               | Использование загрузчика справочников (rdm-loader)                                                 |
| rdm.loader.max.file-size                              | 50000000                           | Максимальный размер файла (в байтах) для загрузчика                                                |
| rdm.download.passport-enable                          | true                               | Генерация pdf-файла с паспортом справочника при сохранении версии                                  |
| rdm.download.passport.head                            | fullName                           | Наименование паспортного атрибута для формирования заголовка паспорта при генерации pdf-файла      |
| rdm.compare.data.diff.max.size                        | 1000                               | Ограничение на размер разницы между данными.                                                       |
| rdm.enable.publish.topic                              | false                              | Отправка события публикации справочника в брокер                                                   |
| rdm.publish.topic                                     | publish_topic                      | Название топика, по которому вещаются события публикации справочника                               |
| rdm.audit.application.name                            | rdm                                | Наименование системы для отправки в аудит                                                          |

