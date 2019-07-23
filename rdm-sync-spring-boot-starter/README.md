
# НСИ. Инструкция по разворачиванию клиента для синхронизации версий справочников  

##1. Добавить в pom зависимость
```
    <dependency>
        <groupId>ru.inovus.ms.rdm</groupId>
        <artifactId>rdm-sync-spring-boot-starter</artifactId>
        <version>${rdm.version}</version>
    </dependency>
```

##2. Запустить клиентское приложение
Нужно, чтобы отработал liquibase.

В базе данных должна создаться схема rdm_sync с таблицами:  
 * version - список справочников которые необходимо синхронизировать с НСИ;  
 * field_mapping - маппинг полей;  
 * log - журнал обновления.  

**Важно**: liquibase rdm sync запускается ПОСЛЕ общего liquibase, сконфигурированного по умолчанию. Поэтому, если нужно добавить в общем liquibase что-то, 
что производит изменения в схеме rdm_sync, то нужно это добавлять в директорию **/rdm-sync-db/changelog**.

Заполнить таблицы в соответствии с требованиями системы.
 > Внимание! Поле, указанное в качестве первичного ключа таблицы должно быть уникально. 

 Допустимые типы данных при указании маппинга полей:  
  * строковые: varchar, text, character varying  
  * целочисленные: smallint, integer, bigint, serial, bigserial  
  * дата: date  
  * логический: boolean  
  * с плавающей точкой: numeric, decimal  
  * для полей, имеющих ссылку на другой справочник:  jsonb  
  
##3. Необязательно. 
Чтобы методы сервиса отображались в swagger клиента, в application.properties добавить к настройкe swagger пакет синхронизатора через запятую:
```  
jaxrs.swagger.resource-package=..., ru.inovus.ms.rdm.service  
```  
  
##4. Обновление всех справочников, которые ведутся в системе клиента:
```  
{CLIENT_SERVICE_URL}/rdm/update  
```  
  
Обновление конкретного справочника:  
  
```  
{CLIENT_SERVICE_URL}/rdm/update?refbookCode=A001  
```