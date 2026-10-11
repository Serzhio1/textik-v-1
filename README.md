# textik

Веб-приложение для изучения английского языка (A1–B1) с персонализированным ИИ-контентом.
Одна сессия — 40–50 минут по схеме **Read → Think → Discuss → Review**:

| Этап | Что происходит | Время |
|------|----------------|-------|
| Read | текст, сгенерированный под интересы и уровень | ~20 мин |
| Think | 5–10 вопросов с объяснением ошибок | 5–10 мин |
| Discuss | диалог с ИИ по тексту, ИИ держит тему | 15–20 мин |
| Review | итоги: ошибки, оценка уровня разговора, разбор | ~5 мин |

## Стек

Java 21 · Spring Boot 4.1.1 · Spring Security 7 · Spring Data JPA · Thymeleaf · Liquibase ·
Spring AI 2.0.1 · PostgreSQL 17 · Lombok · Testcontainers · JUnit 5

## Быстрый старт

```bash
docker compose up -d                                   # Postgres 17 на порту 5433
./mvnw spring-boot:run -Dspring-boot.run.profiles=local  # http://localhost:8080, без ключа ИИ
```

Регистрация (email = логин) → онбординг (имя, «О себе», интересы, уровень) → главный экран
→ выбор темы → чтение → (в разработке: вопросы и далее до ревью).

Тесты (своя БД в Testcontainers, `compose` не нужен):

```bash
./mvnw test
```

## Настройка ИИ

Провайдер задаётся только переменными окружения — код менять не нужно. Без ключа
работает офлайн-заглушка: профиль `local` или `AI_PROVIDER=stub`.

| Переменная | По умолчанию | Назначение |
|------------|--------------|------------|
| `AI_PROVIDER` | `gigachat` | `gigachat` \| `openai` \| `stub` |
| `AI_BASE_URL` | `https://api.giga.chat/v1` | endpoint любого OpenAI-совместимого API |
| `AI_MODEL` | `GigaChat-2` | модель |
| `AI_TEMPERATURE` | `0.7` | необязательно |
| `AI_API_KEY` | — | ключ для `openai` |
| `GIGACHAT_AUTH_KEY` | — | ключ из GigaChat Studio → Настройки API |
| `AI_FALLBACK` | `none` | `stub` — отдавать заготовку при сбое основной модели |

Примеры:

```bash
# GigaChat
AI_PROVIDER=gigachat GIGACHAT_AUTH_KEY=<key> AI_BASE_URL=https://api.giga.chat/v1 AI_MODEL=GigaChat-2 ./mvnw spring-boot:run

# OpenAI
AI_PROVIDER=openai AI_API_KEY=sk-... AI_BASE_URL=https://api.openai.com AI_MODEL=gpt-4o-mini ./mvnw spring-boot:run
```

> Хост выдачи токена `ngw.devices.sberbank.ru:9443` отдаёт цепочку сертификатов, которой нет
> в стандартном truststore JDK. На Debian/Ubuntu сертификаты НУЦ Минцифры уже в системном
> store; чтобы JVM их увидела, добавьте в рантайм `-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts`
> (или импортируйте их в `cacerts` используемого JDK). Ещё две особенности GigaChat: ответ
> `chat.completions` не содержит `id`, нужный openai-java SDK, и модель любит вставлять в JSON
> «сырые» переносы строк (плюс обрамлять ответ в ```json). Приложение чинит это само: перехватчик
> (`chatCompletionIdCustomizer`) добавляет `id`, санитайзер (`ChatCompletionJson`) экранирует
> переносы и снимает обрамление, промпт жёстко требует чистый JSON, а неразобранный ответ
> перезапрашивается один раз. Все ошибки ИИ-слоя (и текст «сырого» ответа) логируются в консоль.

## Переменные БД

`DB_URL` (`jdbc:postgresql://localhost:5433/textik`), `DB_USERNAME`, `DB_PASSWORD` —
дефолты `textik/textik` совпадают с `compose.yaml`. Схема применяется Liquibase при старте
(`src/main/resources/db/changelog`); changeset-файлы не редактируются, изменения — новыми файлами.

## Структура

Плоские пакеты по роли: `entity`, `enums`, `repository`, `service`, `dto`, `exceptions`,
`controller`, `config`, `security`, `utility`. Направления: `controller → service → repository → entity`.

Вся работа с ИИ изолирована за интерфейсом `AiGateway` (`service/AiGateway.java`):
`generateTopics`, `generateText`, `generateQuestions`, `continueDiscussion`, `generateReview`.
Реализации: `SpringAiGateway` (боевая), `StubAiGateway` (офлайн), `FallbackAiGateway` (страховка).

## Состояние

Готово: инфраструктура и БД, домен, аутентификация, онбординг с профилем, прогресс,
слой ИИ, создание сессии — главный экран, выбор темы, чтение (в истории на «Прогрессе»
только завершённые сессии). Не сделано: вопросы, обсуждение, ревью, проверка владельца на
остальных маршрутах сессии, загрузочные состояния.

Подробности состояния, решений и плана — в [PROJECT_PLAN.md](PROJECT_PLAN.md).