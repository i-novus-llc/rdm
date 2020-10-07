# Требования
- OpenJDK 14
- PostgreSQL 11
- Artemis или ActiveMQ
- N2O Security Admin 4
- N2O Audit 2

# Cтек технологий
- Java 14
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
- `rdm-api` - общие интерфейсы и модели
- `rdm-impl` - общие классы имплементации для модуля `rdm-api`
- `rdm-l10n-api` - общие интерфейсы и модели локализации записей справочников
- `rdm-l10n-impl` - общие классы имплементации локализации записей справочников
- `rdm-n2o-api` - общие интерфейсы и модели N2O 
- `rdm-n2o` - общие классы имплементации и конфигурационные файлы N2O 
- `rdm-n2o-l10n` - общие классы имплементации N2O для локализации записей справочников
- *`rdm-frontend`* - запускаемый модуль UI
- *`rdm-rest`* - запускаемый модуль бэкэнда
- `rdm-sync-spring-boot-starter` - стартер для синхронизации с RDM
- `rdm-esnsi` - запускаемый модуль для интеграции с ЕСНСИ

# Варианты сборки
1) Сборка всех модулей: maven-профиль `build-all-modules` (без сборки статики).
2) Сборка для синхронизации с rdm: отключённый maven-профиль `build-all-modules` (соберёт только rdm-api и rdm-sync-spring-boot-starter). 
3) Сборка статики для frontend'а: maven-профиль `frontend-build`.
4) Сборка с поддержкой локализации записей справочников: maven-профиль `l10n`.
