# RDM REST
 
## Описание свойств в файле ```application.properties```
  - `spring.activemq.broker-url` -- адрес брокера сообщений ActiveMQ для вещания событий публикации справочника. По умолчанию "tcp://yandex.develop:9963".
  - `rdm.enable.publish.topic` -- признак создания при старте сервиса экземпляров классов, необходимых для вещания событий публикации справочника через брокер. По умолчанию "false".
  - `rdm.publish.topic` -- название топика, по которому вещаются события публикации справочника. По умолчанию "publish_topic".
  - `rdm.loader.enabled` -- признак загрузки справочников с использованием механизма лоадеров. По умолчанию "true".
  - `rdm.loader.max.file-size` -- максимальный размер файла (в байтах) справочников при загрузке с использованием механизма лоадеров. По умолчанию "20000000".
