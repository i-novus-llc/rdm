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
`target` -- вид данных -- значение должно быть `refBookData`.
Содержимое запроса должно включать прикреплённые xml-файлы и иметь `content-type` = `"multipart/form-data"`.

*Пример использования в приложении*:
```java
import net.n2oapp.platform.loader.autoconfigure.ClientLoaderConfigurer;
import net.n2oapp.platform.loader.client.ClientLoaderRunner;
import org.springframework.context.annotation.Configuration;
import ru.inovus.ms.rdm.loader.client.RefBookDataClientLoader;

/**
 * Настройщик загрузчиков файлов справочников RDM.
 */
@Configuration
public class RefBookDataClientLoaderConfigurer implements ClientLoaderConfigurer {

    @Override
    public void configure(ClientLoaderRunner runner) {
        runner.add("${rdm.backend.path}/loaders", "demo", "refBookData",
                "RefBookData.xml", RefBookDataClientLoader.class);
    }
}
```
Здесь `rdm.backend.path` -- свойство, содержащее адрес REST-сервиса RDM,
`demo` -- значение `subject`,
`refBookData` -- значение `target`,
`RefBookData.xml` -- адрес ресурса с данными в `classpath` приложения,
`RefBookDataClientLoader` -- класс из модуля `rdm-loader-client` для формирования содержимого REST-запроса из ресурса с данными.
