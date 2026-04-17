# Banking System

Многомодульный backend-проект на Java и Spring Boot, реализующий банковую систему с REST API, отдельным API Gateway, JWT-аутентификацией и асинхронной обработкой событий через Kafka.

Проект собран как набор связанных сервисов:

- **bank-presentation** — основной HTTP API банковой системы
- **bank-application** — бизнес-логика
- **bank-infrastructure** — сущности, DTO, репозитории и общая инфраструктура
- **API-Gateway** — точка входа с аутентификацией и ролевым доступом
- **Storage** — сервис-потребитель Kafka-событий с сохранением истории изменений в БД

## Что реализовано

Система поддерживает:

- регистрацию и хранение пользователей;
- создание банковских счетов;
- пополнение счёта;
- снятие средств;
- переводы между счетами;
- просмотр баланса;
- просмотр истории транзакций;
- работу с друзьями пользователя;
- фильтрацию пользователей по полу и цвету волос;
- ролевую модель **ADMIN / CLIENT**;
- вход по логину и паролю с получением **JWT**;
- выход с занесением токена в blacklist;
- публикацию событий об изменении пользователей и счетов в **Kafka**;
- отдельное асинхронное хранилище событий.

## Архитектура

### 1. Основной банковский сервис

Бизнес-логика разделена на три модуля:

#### `bank-application`

Содержит прикладные сервисы и мапперы:

- `AccountService`
- `UserService`
- `KafkaEventProducer`
- `AccountMapper`
- `UserMapper`
- `TransactionMapper`

Основные сценарии:

- создание счёта для пользователя;
- депозиты и снятие средств;
- переводы между счетами;
- расчёт комиссии при переводах;
- получение счетов и транзакций;
- регистрация пользователей;
- добавление и удаление друзей;
- отправка событий в Kafka-топики `client-topic` и `account-topic`.

#### `bank-infrastructure`

Содержит инфраструктурный слой:

- JPA-сущности `User`, `Account`, `Transaction`;
- DTO для API и событий;
- Spring Data JPA репозитории;
- enums для статусов и типов транзакций;
- собственные исключения и глобальный обработчик ошибок.

#### `bank-presentation`

Содержит REST-контроллеры:

- `UserController`
- `AccountController`
- `BankApplication`

Основные endpoint'ы покрывают:

- пользователей;
- друзей пользователя;
- пользовательские счета;
- создание счетов;
- баланс;
- переводы;
- историю транзакций;
- фильтрацию транзакций по типу и счёту.

### 2. API Gateway

`API-Gateway` — отдельное Spring Boot приложение, выступающее внешней точкой входа.

Что делает gateway:

- аутентифицирует пользователей;
- генерирует JWT;
- валидирует JWT в `JwtAuthFilter`;
- ограничивает доступ по ролям;
- хранит учётные записи gateway в таблице `gateway_users`;
- хранит отозванные токены в `token_blacklist`;
- проксирует вызовы в основной банковский API через `RestTemplate`.

Основные компоненты:

- `AuthController`
- `AdminController`
- `ClientController`
- `AuthService`
- `AdminServices`
- `ClientServices`
- `JwtServices`
- `UserDetailsServiceImpl`
- `SecurityConfig`

Ролевой доступ:

- **ADMIN** — создание администраторов и клиентов, просмотр пользователей и счетов;
- **CLIENT** — просмотр своих данных, работа с друзьями, пополнение, снятие и переводы.

### 3. Storage

`Storage` — отдельный сервис для асинхронной фиксации событий.

Он:

- подключается к Kafka;
- читает сообщения из топиков;
- сохраняет события в PostgreSQL;
- хранит отдельно события по клиентам и счетам.

Основные компоненты:

- `KafkaConsumer`
- `AccountEvent`
- `ClientEvent`
- `AccountEventRepository`
- `ClientEventRepository`

## Особенности бизнес-логики

### Переводы и комиссия

В `AccountService` реализована комиссия при переводе:

- **0%** — перевод самому себе;
- **3%** — перевод другу;
- **10%** — перевод другому пользователю, который не находится в списке друзей.

### Транзакции

Поддерживаются типы операций:

- `DEPOSIT`
- `WITHDRAW`
- `TRANSFER`

Поддерживаются статусы транзакций:

- `PENDING`
- `SUCCESS`
- `FAILED`

### Пользователи

Пользователь содержит:

- логин;
- имя;
- возраст;
- пол;
- цвет волос;
- список друзей;
- список счетов.

## Технологический стек

- **Java 23**
- **Spring Boot 3.4.4**
- **Spring Web**
- **Spring Data JPA**
- **Spring Security 6**
- **PostgreSQL**
- **Kafka**
- **JWT (jjwt 0.12.6)**
- **Swagger / Springdoc OpenAPI**
- **MapStruct**
- **Lombok**
- **Maven**
- **Docker Compose**
- **GitHub Actions**

## Структура репозитория

```text
.
├── API-Gateway
├── Storage
├── bank-application
├── bank-infrastructure
├── bank-presentation
├── docker-compose.yml
└── pom.xml
```

Корневой `pom.xml` — агрегирующий parent-проект с модулями:

- `bank-application`
- `bank-infrastructure`
- `bank-presentation`
- `API-Gateway`
- `Storage`

## Инфраструктура и порты

### PostgreSQL и pgAdmin

В корневом `docker-compose.yml` поднимаются:

- **PostgreSQL** на `localhost:54321`
- **pgAdmin** на `localhost:8080`

Параметры БД:

- database: `bank`
- user: `postgres`
- password: `postgres`

### Kafka

В `Storage/docker-compose.yml` поднимаются:

- **Zookeeper** на `2181`
- **Kafka** на `9092`

### Порты приложений

- **bank-presentation** — `8081`
- **API-Gateway** — `8082`
- **Storage** — `8083`

## Безопасность

Безопасность вынесена в `API-Gateway`.

Реализовано:

- stateless-конфигурация через `SessionCreationPolicy.STATELESS`;
- вход через `/api/auth/login`;
- JWT-фильтр `JwtAuthFilter`;
- шифрование паролей через `BCryptPasswordEncoder`;
- blacklist токенов при logout;
- разделение прав через `@PreAuthorize`.

Основные защищённые зоны:

- `/api/admin/**` — только `ADMIN`;
- `/api/client/**` — только `CLIENT`.

## Документация API

Swagger/OpenAPI включён в:

- `bank-presentation`
- `API-Gateway`

Судя по конфигурации, приложение публикует OpenAPI-документацию и Swagger UI через `springdoc`.

## Событийная модель

При изменениях в системе публикуются события двух типов:

- **Client events** — создание и изменение пользователей;
- **Account events** — создание счетов, изменение баланса и новые операции.

Банковый сервис публикует события в Kafka, а `Storage` сохраняет их в БД. Это позволяет отделить основную бизнес-логику от слоя аудита и асинхронного хранения истории изменений.

## Сборка и запуск

### 1. Клонирование репозитория

```bash
git clone https://github.com/EpicWhal3/Java-projects.git
cd Java-projects
```

### 2. Поднять PostgreSQL и pgAdmin

```bash
docker compose up -d
```

### 3. Поднять Kafka и Zookeeper

```bash
cd Storage
docker compose up -d
cd ..
```

### 4. Собрать проект

```bash
mvn clean install
```

### 5. Запустить приложения

В отдельных терминалах:

#### bank-presentation

```bash
cd bank-presentation
mvn spring-boot:run
```

#### API-Gateway

```bash
cd API-Gateway
mvn spring-boot:run
```

#### Storage

```bash
cd Storage
mvn spring-boot:run
```

## CI

В репозитории настроен GitHub Actions workflow `.github/workflows/java.yml`, который собирает проект через Maven при `push`.
