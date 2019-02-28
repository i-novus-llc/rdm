# НСИ. Инструкция по разворачиванию клиента для синхронизации версий справочников

1. В application.properties добавить к настройку swagger пакет синхронизатора:
```
 ключу jaxrs.swagger.resource-package добавить через запятую пакет ru.inovus.ms.rdm.service
jaxrs.swagger.resource-package=..., ru.inovus.ms.rdm.service
```

2. Добавить в application.properties настройки клиента:
```
rdm.client.sync.url=${rdm.rest.url}
cxf.jaxrs.client.address=${rdm.rest.url}
cxf.jaxrs.client.classes-scan=true
cxf.jaxrs.client.classes-scan-packages=ru.inovus.ms.rdm.service.api
```

3. Добавить в base-changelog.xml:
 ```
   <includeAll path="classpath*:/rdm-sync-db/changelog"/>
```