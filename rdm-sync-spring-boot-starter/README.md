
# НСИ. Инструкция по разворачиванию клиента для синхронизации версий справочников  

1. Добавить в pom зависимость
```
    <dependency>
        <groupId>ru.inovus.ms.rdm</groupId>
        <artifactId>rdm-sync-spring-boot-starter</artifactId>
        <version>${rdm.version}</version>
    </dependency>
```

3. Необязательно. Чтобы методы сервиса отображались в swagger клиента, в application.properties добавить к настройкe swagger пакет синхронизатора через запятую:
```  
jaxrs.swagger.resource-package=..., ru.inovus.ms.rdm.service  
```  
  
4. Запустить клиентское приложение, чтобы отработал liquibase.
Если пункт 2 выполнен правильно, то в базе данных должна создаться схема rdm_sync с таблицами:  
 * version - список справочников которые необходимо синхронизировать с НСИ;  
 * field_mapping - маппинг полей;  
 * log - журнал обновления.  
  
 Заполнить таблицы в соответствии с требованиями системы.
 > Внимание! Поле, указанное в качестве первичного ключа таблицы должно быть уникально. 

 Допустимые типы данных при указании маппинга полей:  
  * строковые: varchar, text, character varying  
  * целочисленные: smallint, integer, bigint, serial, bigserial  
  * дата: date  
  * логический: boolean  
  * с плавающей точкой: numeric, decimal  
  * для полей, имеющих ссылку на другой справочник:  jsonb  
  
5. Обновление всех справочников, которые ведутся в системе клиента:
```  
{CLIENT_SERVICE_URL}/rdm/update  
```  
  
Обновление конкретного справочника:  
  
```  
{CLIENT_SERVICE_URL}/rdm/update?refbookCode=A001  
```