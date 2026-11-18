# Zolie API

Backend for Zolie: social feed, Reels, stories, 18+ dating and messaging in one account.
Spring Boot 4, Java 21, JPA, PostgreSQL 16 with PostGIS, Flyway, STOMP WebSocket.

## Run it

1. Copy `.env.example` to `.env` and set `DB_USERNAME` and `DB_PASSWORD`.
2. Start the database: `docker compose up -d`
3. Start the API: `./gradlew bootRun` (or run `ZolieApiApplication` from the IDE)
4. Open Swagger: http://localhost:8080/swagger-ui.html

To run the API in Docker too: `docker compose --profile app up -d --build`

Flyway creates and updates the schema on startup (`src/main/resources/db/migration`).
Hibernate only validates that the entities match it.

## Test in Swagger

With `DEV_AUTH_ENABLED=true` in `.env` you do not need Firebase:

1. Click **Authorize** and enter `dev:dara` (any lowercase name, 3 to 30 characters).
2. Call `POST /api/v1/me/sync` once to create that account.
3. Use a second token such as `dev:sophea` (another browser or a private window) for follows, chats and dating.
4. Add `:admin` (`dev:boss:admin`) for the `/admin` endpoints.

Uploads use a fake storage until `R2_ENDPOINT` is set: call `POST /media/upload-url`, skip the real upload,
then `POST /media/{id}/complete`.

Live chat: connect a STOMP client to `ws://localhost:8080/ws` with the header `Authorization: Bearer <token>`.

## Structure

```
controller/     REST and WebSocket endpoints
service/        service interfaces
service/impl/   business rules
repository/     Spring Data repositories and SQL
model/          JPA entities, enums and composite keys
dto/            request and response records
mapper/         entity to response mapping
security/       token check, current user, rate limiter
config/         security, Swagger, WebSocket, storage, Firebase
exception/      error codes and the error response format
job/            scheduled jobs
```

## Production settings

Set `DEV_AUTH_ENABLED=false`, `FIREBASE_CREDENTIALS_PATH` (service account JSON) and the `R2_*` values.
Never commit `.env`.
