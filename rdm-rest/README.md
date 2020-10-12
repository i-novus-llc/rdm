# RDM REST
 
## Описание свойств в файле ```application.properties```
  - `spring.activemq.broker-url` -- представляет собой адрес брокера сообщений ActiveMQ. 
    Через него проходят, например, сообщения о публикации справочника. По умолчанию "tcp://docker.one:8825".
  - `rdm.enable.publish.topic` -- если это свойство задано как `true`, при старте будут созданы экземпляры классов,
 необходимых для работы ActiveMQ, и будет включено вещание событий публикации справочника. По умолчанию `false`.
  - `rdm.publish.topic` -- название топика, по которому вещаются события публикации справочника. По умолчанию "publish_topic".
  - `rdm.loader.server.enabled` -- разрешает создание справочников через механизм лоадеров. По умолчанию `true`.
  
## Создание справочников через механизм лоадеров
RDM поддерживает создание и публикацию справочников из xml-файлов с использованием механизма `n2o-platform-loader`.

REST-запрос доступен по пути
`/loaders/{subject}/{target}`, где
`subject` -- владелец данных -- кодовое обозначение приложения, которому требуется функциональность,
`target` -- вид данных, значение должно быть `refBookData`.
Содержимое запроса должно включать прикреплённый xml-файл и иметь `content-type` = `"multipart/form-data"`.
Файл справочника в формате xml можно получить в результате выгрузки этого справочника в рамках rdm.

### Подключение
* Добавьте зависимости:
```xml
<dependency>
  <groupId>net.n2oapp.platform</groupId>
  <artifactId>n2o-platform-starter-loader-client</artifactId>
</dependency>

<dependency>
  <groupId>ru.i-novus.ms.rdm</groupId>
  <artifactId>rdm-loader-client</artifactId>
</dependency>
```

* Создайте файл конфигурации, например, `rdm.json`:
```json
[
  {
    "code" : "код справочника",
    "file" : "путь к файлу справочника в ресурсах"
  },
  ...
]  
```

* Добавьте конфигурацию для использования загрузчика справочников:
```java
import net.n2oapp.platform.loader.autoconfigure.ClientLoaderConfigurer;
import net.n2oapp.platform.loader.client.ClientLoaderRunner;
import org.springframework.context.annotation.Configuration;
import ru.i_novus.ms.rdm.loader.client.RefBookDataClientLoader;

/**
 * Настройщик загрузчиков файлов справочников RDM.
 */
@Configuration
public class RefBookDataClientLoaderConfigurer implements ClientLoaderConfigurer {

    @Override
    public void configure(ClientLoaderRunner runner) {
        runner.add("${rdm.backend.path}/loaders", "demo", "refBookData",
                "rdm.json", RefBookDataClientLoader.class);
    }
}
```
Здесь `rdm.backend.path` -- свойство, содержащее адрес REST-сервиса RDM,
`demo` -- значение `subject`,
`refBookData` -- значение `target`,
`rdm.json` -- путь к json-файлу конфигурации в `classpath` приложения,
`RefBookDataClientLoader` -- класс из модуля `rdm-loader-client` для формирования содержимого REST-запроса из ресурса с данными.
