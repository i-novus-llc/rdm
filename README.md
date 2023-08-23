# Требования

- OpenJDK 11
- PostgreSQL 11
- Artemis или ActiveMQ
- N2O Security Admin 4
- N2O Audit 2

# Стек технологий

- Java 11
- JDBC
- JPA 2
- JAX-RS
- JMS
- Spring Boot 2.1
- Spring Cloud Greenwich
- Liquibase 3.6.2
- N2O Platform 4
- N2O UI Framework 7
- React

# Структура проекта

- `rdm-api` - общие интерфейсы и модели.
- `rdm-impl` - общие классы имплементации для модуля `rdm-api`.
- `rdm-l10n-api` - общие интерфейсы и модели локализации записей справочников.
- `rdm-l10n-impl` - общие классы имплементации локализации записей справочников.
- `rdm-n2o-api` - общие интерфейсы и модели N2O. 
- `rdm-n2o` - общие классы имплементации и конфигурационные файлы N2O. 
- `rdm-n2o-l10n` - общие классы имплементации N2O для локализации записей справочников.
- `rdm-rest` - общие классы для REST-API.
- *`rdm-frontend`* - запускаемый модуль фронтенда (UI).
- *`rdm-service`* - запускаемый модуль бэкенда.
- `rdm-web-spring-boot-autoconfigure` - автоконфигуратор UI (для проектов, использующих RDM).
- `rdm-web-spring-boot-starter` - стартер UI (для проектов, использующих RDM).
- `rdm-rest-spring-boot-starter` - стартер REST-API бэкэнда.
- `rdm-esnsi` - запускаемый модуль для интеграции с ЕСНСИ.

# Варианты сборки
1) Сборка всех модулей: maven-профиль `build-all-modules` (без сборки статики и без поддержки локализации).
2) Сборка статики для фронтенда: maven-профиль `frontend-build`.
3) Сборка с поддержкой локализации записей справочников: maven-профиль `l10n` (профиль `build-all-modules` не нужен).
