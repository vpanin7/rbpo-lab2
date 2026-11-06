# Лабораторная работа №4 — учёт коммунальных ресурсов

Вариант 20. Продолжение лабораторной №3: Java 17, Spring Boot, Maven, Spring JDBC и PostgreSQL 16.

## Доступ к API

| Кто | Что может делать |
| --- | --- |
| Без входа | Получить CSRF-токен и зарегистрироваться |
| Житель (`RESIDENT`) | Посмотреть свой профиль, открыть своё помещение по ID и оплатить его начисления |
| Администратор (`ADMIN`) | Выполнять CRUD для всех пяти сущностей, подтверждать показания, начислять плату, передавать помещения, заменять счётчики и включать или отключать аккаунты жителей |

Вход — HTTP Basic, логин — email. Пароль хранится в PostgreSQL только в виде BCrypt-хеша. Публичная регистрация всегда выдаёт роль жителя. У администратора есть `PATCH /api/users/{id}/account` с телом `{"enabled": false}` или `{"enabled": true}`. Отключённый аккаунт не может войти.

При оплате идентификатор жителя берётся из Basic Auth, а текущий владелец проверяется внутри транзакции. В запросе оплаты нужны только `period` и `amount`. После передачи помещения старый владелец теряет доступ.

## Подготовка PostgreSQL

Для демонстрации создай в pgAdmin отдельную базу `utilities_lab4` с владельцем `postgres`. Так данные предыдущей лабы останутся доступны, а сценарий начнётся с чистых данных. Docker не требуется.

1. Если роль БД `utilities_app` уже создана в лабе №3, используй её пароль. Если роли ещё нет, выполни от имени `postgres` (замени `YOUR_DB_PASSWORD` своим паролем):

   ```sql
   CREATE ROLE utilities_app LOGIN PASSWORD 'YOUR_DB_PASSWORD'
       NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
   ```

2. В Query Tool **базы `utilities_lab4`** выполни файлы в таком порядке: [schema.sql](db/schema.sql), [seed.sql](db/seed.sql), [security.sql](db/security.sql).
3. В той же базе выдай права на таблицы и последовательности:

   ```sql
   GRANT CONNECT ON DATABASE utilities_lab4 TO utilities_app;
   GRANT USAGE ON SCHEMA public TO utilities_app;
   REVOKE CREATE ON SCHEMA public FROM PUBLIC;
   GRANT SELECT, INSERT, UPDATE, DELETE
       ON app_users, premises, meters, meter_readings, utility_charges TO utilities_app;
   GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO utilities_app;
   ```

`security.sql` добавляет таблицу аккаунтов и выдаёт права на неё. Если нужно подключить существующую базу третьей лабы, выполни в ней только `security.sql` и укажи эту базу в `DB_URL`. Сценарий запросов рассчитан на свежие демонстрационные данные.

## Запуск на Windows

В PowerShell из папки с `pom.xml`:

```powershell
$env:DB_URL='jdbc:postgresql://localhost:5432/utilities_lab4'
$env:DB_USER='utilities_app'
$env:DB_PASSWORD='YOUR_DB_PASSWORD'
$env:SPRING_PROFILES_ACTIVE='demo'
$env:DEMO_ADMIN_PASSWORD='YOUR_ADMIN_PASSWORD'
$env:DEMO_RESIDENT_PASSWORD='YOUR_RESIDENT_PASSWORD'
.\mvnw.cmd spring-boot:run
```

Замени три заглушки своими паролями. Для аккаунтов API пароль должен содержать минимум 12 символов, заглавную и строчную латинские буквы, цифру и спецсимвол; максимум 72 байта UTF-8. Пароль БД и пароли для входа в API — разные настройки.

Профиль `demo` добавляет аккаунты для `admin@example.com`, `resident1@example.com` и `resident2@example.com`. У двух демонстрационных жителей общий пароль. Повторный запуск не сбрасывает пароли и состояния уже созданных аккаунтов.

## Проверка

- **Postman:** импортируй [коллекцию](postman/utilities.postman_collection.json). Заполни `adminPassword`, `residentPassword`, `signupPassword` в локальных значениях переменных. Первые два пароля возьми из запуска, третий придумай для нового жителя. Оставь cookie jar включённым. Запусти коллекцию сверху вниз на свежей базе.
- **IntelliJ IDEA:** скопируй `http-client.private.env.example.json` в `http-client.private.env.json` и подставь те же три пароля. В [requests.http](requests.http) выбери окружение `local` и укажи предыдущий завершённый месяц в `period`. Запускай запросы сверху вниз.

Первый запрос `GET /api/csrf` возвращает имя заголовка и токен. Клиент сохраняет cookie сессии и передаёт токен в `X-CSRF-TOKEN` для POST, PATCH и DELETE. Basic Auth отправляется отдельно в каждом защищённом запросе.

Ожидаемые отказы: `401` — не удалось войти; `403` — нет прав или токена CSRF; `400` и `409` — неверные данные или нарушение правил операции. Запрос без CSRF может получить `403` ещё до проверки логина. Запросы меняют базу, поэтому при повторном запуске сценария с теми же данными ответы могут отличаться.
