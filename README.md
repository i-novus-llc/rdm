# Требования
- Openjdk 11
- PostgreSQL 11
- Artemis или ActiveMQ
- N2O Security Admin 4
- N2O Audit 2

# Cтек технологий
- Java 11
- JDBC
- JPA 2
- JAX-RS
- JMS
- Spring Boot 2.1
- Spring Cloud Greenwich
- Liquibase 3.6.2
- N2O Platform 3
- N2O UI Framework 7
- React

# Структура проекта
- `rdm-frontend` - запускаемый модуль UI
- `rdm-n2o` - конфигурационные файлы N2O 
- `rdm-api` - общие интерфейсы и модели
- `rdm-impl` - общие классы имплементации для модуля -api
- `rdm-rest` - запускаемый модуль бэкэнда
- `rdm-sync-spring-boot-starter` - стартер для синхронизации с RDM
- `rdm-esnsi` - запускаемый модуль для интеграции с ЕСНСИ

# Варианты сборки
1) сборка всех модулей maven-профиль:  build-all-modules. (без сборки статики )
2) сборка для синхронизации с rdm. отключенный maven-профиль  build-all-modules (соберет только rdm-api и rdm-sync-spring-boot-starter) 
3) сборка статики для frontend maven-профиль: frontend-build
