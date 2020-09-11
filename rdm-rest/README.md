 ## Описание свойств в файле ```application.properties```
 - `spring.activemq.broker-url` -- представляет собой адрес брокера сообщений ActiveMQ. 
 Через него проходят, например, сообщения о публикации справочника. По умолчанию "tcp://docker.one:8825".
 - `rdm.enable.publish.topic` -- если это свойство задано как true, при старте будут созданы экземпляры классов,
 необходимых для работы ActiveMQ и будет включено вещание событий публикации справочника. По - умолчанию false.
 - `rdm.publish.topic` -- название топика, по которому вещаются события публикации справочника. По - умолчанию "publish_topic".