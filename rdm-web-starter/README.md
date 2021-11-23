# Стартер UI

Модуль используется при подключении RDM к клиенту
для автонастройки необходимых модулей и классов UI.

## Подключение

* Добавьте зависимость в `pom.xml`:
    ```xml
    <dependency>
      <groupId>ru.i-novus.ms.rdm</groupId>
        <artifactId>rdm-web-starter</artifactId>
        <version>${rdm.version}</version>
    </dependency>
    ```
  Здесь `rdm.version` - подключаемая версия RDM.


* Добавьте зависимость в `package.json`:
    ```json
    {
      "dependencies": {
        "n2o-config-rdm": "^2.0.1",
        "n2o-framework": "7.16.10",
      },
      ...
    }
    ```
  Здесь
  - `^2.0.1` - версия RDM-конфигурации N2O, используемая в RDM,
  - `7.16.10` - версия N2O Framework, используемая в RDM.


* Настройте параметры в `application.properties` клиента:

  Добавьте параметр RDM для UI:
  - `rdm.backend.path` -- адрес REST-сервиса RDM.
    
  Добавьте в параметр `spring.messages.basename` значение `messages.rdmui`.

  Добавьте параметры N2O для UI:
    ```properties
    # all objects are accessible
    n2o.access.deny_objects=false
    # all pages are accessible
    n2o.access.deny_pages=false
    # all urls are accessible
    n2o.access.deny_urls=false
    ```


## Подключение RDM с локализацией справочников

Выполните действия, перечисленные выше, а также следующие.

* Добавьте зависимости в `pom.xml`:
    ```xml
    <dependency>
        <groupId>ru.i-novus.ms.rdm</groupId>
        <artifactId>rdm-l10n-api</artifactId>
        <version>${rdm.version}</version>
    </dependency>
    ```
    ```xml
    <dependency>
        <groupId>ru.i-novus.ms.rdm</groupId>
        <artifactId>rdm-n2o-l10n</artifactId>
        <version>${rdm.version}</version>
    </dependency>
    ```


* Настройте параметры в `application.properties` клиента:

  Добавьте параметр RDM-L10N для UI:
  - `rdm.l10n.support` -- управление включением локализации на UI.
    
  Добавьте в параметр `spring.messages.basename` значение `messages.rdm_n2o_l10n`.
