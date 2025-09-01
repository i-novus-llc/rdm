# Требования

- OpenJDK 21
- PostgreSQL 12
- Artemis или ActiveMQ
- N2O Security Admin 9.0.1

# Стек технологий

- Java 21+
- JDBC
- JPA 2
- JAX-RS
- JMS
- Spring Boot 3.5.3
- Spring Cloud 2025
- Liquibase 4.31
- N2O Boot Platform 7.0
- N2O UI Framework 7.29
- React

# Структура проекта

*Общие модули*:
- `rdm-api` - общие интерфейсы и модели.
- `rdm-impl` - общие классы имплементации для модуля `rdm-api`.
- `rdm-l10n-api` - общие интерфейсы и модели для локализации записей справочников.
- `rdm-l10n-impl` - общие классы имплементации для локализации записей справочников.
- `rdm-n2o-api` - общие интерфейсы и модели N2O. 
- `rdm-n2o` - общие классы имплементации и конфигурационные файлы N2O. 
- `rdm-n2o-l10n` - общие классы имплементации N2O для локализации записей справочников.
- `rdm-rest` - общие классы для REST-API.
 
*Автоконфигураторы*:
- `rdm-rest-spring-boot-autoconfigure` - автоконфигуратор REST-API бэкенда (для проектов, использующих RDM).
- `rdm-web-spring-boot-autoconfigure` - автоконфигуратор UI (для проектов, использующих RDM).

*Стартеры*:
- `rdm-rest-spring-boot-starter` - стартер REST-API бэкенда (для проектов, использующих RDM).
- `rdm-web-spring-boot-starter` - стартер UI (для проектов, использующих RDM).

*Запускаемые модули*:
- `rdm-frontend` - запускаемый модуль фронтенда (UI).
- `rdm-service` - запускаемый модуль бэкенда.
- `rdm-esnsi` - запускаемый модуль для интеграции с ЕСНСИ.

# Варианты сборки

1) Сборка всех модулей: maven-профиль `build-all-modules` (без сборки статики и без поддержки локализации).
2) Сборка статики для фронтенда: maven-профиль `frontend-build`.
3) Сборка с поддержкой локализации записей справочников: maven-профили `build-all-modules` и `l10n`.

# Установка

### Создать пользователя rdm/rdm

```
CREATE ROLE rdm
   LOGIN
   ENCRYPTED PASSWORD 'SCRAM-SHA-256$4096:u+JptXqe/kgjAT9EeGp2QQ==$1xSn7KweSg38yEoGxUYUTZQ2BnNHA0FckB9dLPNoh64=:LuGkaxoqOZfPlpe0uNEzEiABBtZyETodU2NziloUdFQ='
   SUPERUSER INHERIT CREATEDB CREATEROLE NOREPLICATION;
```

### Создать БД rdm

```
CREATE DATABASE rdm
    WITH
    OWNER = rdm
    ENCODING = 'UTF8'
    LC_COLLATE = 'Russian_Russia.1251'
    LC_CTYPE = 'Russian_Russia.1251'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;
```

### Подключить FTS

1. Скачать архив `ru-dict.zip`.
2. Разархивировать файлы архива в /usr/local/share/tsearch_data
3. На созданной БД выполнить из-под суперпользователя:

```
CREATE TEXT SEARCH DICTIONARY ispell_ru (
    template = ispell,
    dictfile = ru,
    afffile  = ru
);
 
CREATE TEXT SEARCH CONFIGURATION ru (COPY = russian);
ALTER TEXT SEARCH CONFIGURATION ru
    ALTER MAPPING
        FOR word, hword, hword_part
        WITH ispell_ru, russian_stem;
```
