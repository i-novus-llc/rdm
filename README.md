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
