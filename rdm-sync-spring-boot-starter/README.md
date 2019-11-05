# СИНХРОНИЗАЦИЯ СПРАВОЧНИКОВ

Общая информация
Синхронизация справочников предназначена для переноса данных справочников НСИ в бд клиента синхронизации. Клиент синхронизации в своей бд создает таблицы соответствующей структуры, в которые будет происходить копирование данных НСИ. Синхронизатор предоставляет api для запуска копирования. В какой момент запускать процесс синхронизации клиент реализует сам.

## Первый запуск
1. Добавить в pom зависимость.
    ```
    <dependency>
        <groupId>ru.inovus.ms.rdm</groupId>
        <artifactId>rdm-sync-spring-boot-starter</artifactId>
        <version>${rdm.version}</version>
    </dependency>
    ```

2. Запустить клиентское приложение
Нужно, чтобы отработал liquibase.
В базе данных должна создаться схема rdm_sync с таблицами:

version - список справочников которые необходимо синхронизировать с НСИ;
field_mapping - маппинг полей;
log - журнал обновления.

**Важно:** liquibase rdm sync запускается ПОСЛЕ общего liquibase, сконфигурированного по умолчанию. Поэтому, если нужно добавить в общем liquibase что-то,
что производит изменения в схеме rdm_sync, то нужно это добавлять в директорию /rdm-sync-db/changelog.

3. Необязательно.
Чтобы методы сервиса отображались в swagger клиента, в application.properties добавить к настройкe swagger пакет синхронизатора через запятую: jaxrs.swagger.resource-package=..., ru.inovus.ms.rdm.service

4. Обновление всех справочников, которые ведутся в системе клиента:
{CLIENT_SERVICE_URL}/rdm/update
Обновление конкретного справочника:
{CLIENT_SERVICE_URL}/rdm/update?refbookCode=A001


## Настройка маппинга
Маппинг - это соответствие полей справочника в системе НСИ и колонок таблицы в бд клиента. Описание маппинга производится в таблице rdm_sync.field_mapping(см комментарии к колонкам). Описание какой справочник копировать в какую таблицу производится в таблице rdm_sync.version.
Маппинг можно настроить напрямую в бд и через xml конфигурацию.
### Через бд
Добавляем запись на каждый справочник в rdm_sync.version. Заполняем колонки code - код справочника в НСИ, sys_table - название таблицы в бд клиента, unique_sys_field - заполняем значением “code”, deleted_field - заполняем значением “is_deleted ”.
Пример :
Добавляем запись на каждое поле справочника(которое нужно скопировать в бд клиента) в rdm_sync.field_mapping. Поле справочника, которое является первичным ключом справочника в НСИ, должно маппиться в колонку code
Пример :
```
insert into rdm_sync.version(code, sys_table, unique_sys_field, deleted_field)
select 'S019', 'rdm.grade_test', 'code', 'is_deleted';

insert into rdm_sync.field_mapping(code, sys_field, sys_data_type, rdm_field) select 'S019', 'code', 'varchar', 'id';
insert into rdm_sync.field_mapping(code, sys_field, sys_data_type, rdm_field) select 'S019', 'test_text', 'varchar', 'test_text';
insert into rdm_sync.field_mapping(code, sys_field, sys_data_type, rdm_field) select 'S019', 'sequence', 'integer', 'sequence';
insert into rdm_sync.field_mapping(code, sys_field, sys_data_type, rdm_field) select 'S019', 'grade_request_id', 'varchar', 'grade_request_id';
insert into rdm_sync.field_mapping(code, sys_field, sys_data_type, rdm_field) select 'S019', 'is_required', 'boolean', 'is_required';
```
### Через xml
В classpath (например в папку resources) подкладываем файл с наименование *rdm-mapping.xml*. В случае изменения маппинга меняем в файле соответвующий элемент refbook и увеличиваем mapping-version на 1.
Пример:
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<mapping>

    <refbook code="T001" sys-table="rdm.test_rb" unique-sys-field="code" deleted-field="is_deleted" mapping-version="1">
        <field sys-field="code" sys-data-type="varchar" rdm-field="id"/>
        <field sys-field="name" sys-data-type="varchar" rdm-field="short_name"/>
        <field sys-field="doc_number" sys-data-type="integer" rdm-field="doc_num"/>
    </refbook>

    <refbook code="R001" sys-table="rdm.some_table" unique-sys-field="code" deleted-field="is_deleted" mapping-version="1">
        <field sys-field="code" sys-data-type="varchar" rdm-field="id"/>
        <field sys-field="name" sys-data-type="varchar" rdm-field="short_name"/>
    </refbook>

</mapping>
```

## Из ограничений:
- не использовать ссылочные справочники.
- справочники без первичных ключей не смогут синхронизироваться.
- строковый тип в НСИ можно мапить в "varchar", "text", "character varying", "smallint", "integer", "bigint", "serial", "bigserial", boolean(true/false), "numeric", "decimal",  "date(yyyy-MM-dd)"
- дату из НСИ можно  мапить в "date", "varchar", "text", "character varying"
- дробный в НСИ можно маппить в "numeric", "decimal", "varchar", "text", "character varying"
-логический тип в НСИ можно маппить в boolean, "varchar", "text", "character varying"




## Создание таблиц для копирования данных из НСИ

Таблицы создавать в схеме rdm.
Таблица должна содержать технические колонки :
UUID id - внутрений первичный ключ таблицы, на него можно ссылаться внутри системы.
code - любой совместимый тип с типом первичного ключа справочника в нси. В эту колонку будет копироваться значение первичного ключа справочника в нси. указывается в колонке rdm_sync.version.unique_sys_field
is_deleted - признак удалена ли запись или нет. указывается в колонке rdm_sync.version.deleted_field
Таблица должна содержать для значений справочника, т.е те колонки в которые будут копироваться данные из полей справочника. Их кол-во и название не обязательно должны совпадать. Эти колонки участвуют в маппинге, т.е прописываются в rdm_sync.field_mapping



## Возможность импортировать справочник по событию публикации:
- Необходимо задать значение свойств `rdm_sync.publish.listener.enable`, `spring.activemq.broker-url`, `rdm_sync.publish.topic`.
Первое включает возможность импортировать справочник по событию (по - умолчанию выключено). Второе свойство -- это адрес брокера ActiveMQ. 
Он должен совпадать с адресом брокера, на который уходят сообщения о публикации с PublishServiceImpl.
Третье -- это название топика, по которому вещаются события публикации справочников (по - умолчанию `publish_topic`).