# Textik — ТЗ, принятые решения и план реализации (MVP)

> **Как пользоваться файлом:** единая точка входа для новой сессии с ИИ.
> Разделы 1–2 — требования и решения (стабильны). **Раздел 3 — состояние кода на сегодня**,
> его нужно сверять с реальностью. 4 — задачи по этапам с чекбоксами. 5 — открытые вопросы
> и принятые технические решения. 6 — правила кода и тестов.
> Правило файла: только факты, без пересказа. Устаревшая строка хуже отсутствующей.

## 0. Быстрый старт

```bash
docker compose up -d            # Postgres 17, хост-порт 5433
./mvnw test                     # 86 тестов, ~35 с; БД в тестах — Testcontainers, compose не нужен
./mvnw spring-boot:run          # http://localhost:8080
./mvnw spring-boot:run -Dspring-boot.run.profiles=local   # офлайн, без ключа ИИ
```

- **Стек:** Java 21, Spring Boot **4.1.1**, Spring Security 7, Spring Data JPA, Thymeleaf,
  Liquibase, Spring AI 2.0.1 (OpenAI-совместимый клиент), Postgres 17, Lombok.
- **Структура кода:** 10 плоских пакетов, таблица и направления зависимостей — **6.8**.
- **Сделано:** Этапы 0–5 (инфраструктура, домен, аутентификация, онбординг, слой ИИ) — **3.5**.
- **Следующий этап:** 6 — главный экран, выбор темы, чтение — **4**.
- **Правила и запреты:** 6 (комментарии, Lombok, тесты) и 5.1 (не трогать changeset-файлы,
  не заводить `src/test/resources/application.yaml`).
- **Ключ ИИ не нужен для разработки:** профиль `local` подставляет заглушку (5.1).

---

## 1. Что это за проект (кратко)

**textik** — веб-приложение для вдумчивого изучения английского языка (уровни A1–B1)
на основе персонализированного контента. Цель — одна законченная сессия на 40–50 минут
по схеме **Read → Think → Discuss → Review**:

1. **Read** (~20 мин) — текст, сгенерированный ИИ под интересы и уровень.
2. **Think** (5–10 мин) — 5–10 single-choice вопросов по тексту, после каждого ответа — объяснение.
3. **Discuss** (15–20 мин) — диалог с ИИ по тексту, 3–5 обменов, ИИ держит тему.
4. **Review** (~5 мин) — суммаризация, ошибки в тестах, оценка уровня разговора, разбор ошибок.

### Экраны по ТЗ

| # | Экран | Содержимое |
|---|-------|-----------|
| 1 | Onboarding | Имя + «О себе» (свободный текст) + «Сферы интересов» (через запятую). После заполнения — главный экран. Профиль далее редактируемый |
| 2 | Главный экран | Название приложения, 4 плашки этапов (Read ~20 мин, Think 5–10 мин, Discuss 15–20 мин, Review ~5 мин), кнопка «Начать сессию» |
| 3 | Выбор текста | Карточки тем: название, краткое описание, «Начать чтение». Темы подбираются по интересам и уровню |
| 4 | Чтение | Название, описание, англоязычный текст, снизу «К вопросам» |
| 5 | Вопросы | 5–10 вопросов. При неверном ответе — краткое объяснение. Снизу «К обсуждению» |
| 6 | Обсуждение | Диалог с ИИ: задаёт вопросы, поддерживает разговор, возвращает к теме. Снизу «Перейти к ревью» |
| 7 | Review | Поздравление, суммаризация, число ошибок, оценка уровня разговора, основные ошибки. Сохранение в историю. Кнопка на главный экран |
| 8 | Прогресс | Профиль + история: имя, аватар, почта, число пройденных сессий, «О себе», интересующие темы. Всё редактируемо |

---

## 2. Принятые решения (уточнения к ТЗ)

| # | Вопрос | Решение | Детали |
|---|--------|---------|--------|
| 1 | Источник контента | **Генерация через LLM API** | Тексты и вопросы генерирует ИИ из профиля (интересы + уровень), готового контент-сида нет |
| 2 | Обсуждение (Discuss) | **Реальный LLM через API** | Диалог ведёт модель, не скрипт |
| 3 | База данных | **PostgreSQL через `compose.yaml`** | Контейнер `postgres:17`, том, healthcheck, порт **5433** (5432 занят локальным Postgres). Модуль docker-compose Spring Boot 4 не используем: контейнер для разработки поднимается вручную |
| 4 | Пользователи | **Логин + пароль** | Spring Security, форма регистрации/входа, BCrypt. **Email = логин**; в `users` хранится `username` — имя (Sergey и т.п.), видно в профиле и в диалоге с ИИ |
| 5 | Интерфейс | **Thymeleaf SSR** | Серверный рендеринг, один артефакт |
| 6 | Способ интеграции с LLM | **Spring AI** | Structured Output: модель отдаёт JSON → record в `dto` |
| 7 | LLM-провайдер | **Любой OpenAI-совместимый endpoint** | Официального Spring AI-провайдера Яндекса нет, но `spring-ai-starter-model-openai` работает с любым OpenAI-совместимом API. Провайдер, модель и ключ задаются **только переменными окружения** (`AI_BASE_URL`, `AI_MODEL`, `AI_API_KEY`) — смена провайдера не требует правок кода. Модель по умолчанию не выбрана, ключа нет (5) |
| 8 | Уровень пользователя (A1–B1) | **Ручной выбор в онбординге** | В исходном ТЗ поля нет, но тексты обязаны подбираться по уровню |
| 9 | Аватар | **Без загрузки файлов** | В MVP — инициалы из `username`. Загрузка файлов вне скоупа |
| 10 | Тип вопросов (Think) | **Только single-choice** | Один правильный из 4 вариантов, проверка детерминированная (сравнение с `correct_answer`). `q_type` и оценка открытых ответов убраны из MVP |
| 11 | Комментарии в коде | **Минимум: код должен говорить сам** | Механический код поручаем Lombok: служебные классы — `@UtilityClass` (6.6), внедрение зависимостей — `@RequiredArgsConstructor` (6.7). Правила и обоснование — 6.5 |
| 12 | Структура пакетов | **10 плоских пакетов по роли** | `entity`, `enums`, `repository`, `service`, `dto`, `exceptions`, `controller`, `config`, `security`, `utility`. Пакет отвечает на вопрос «какую роль играет класс», а не «в какой фиче он живёт»; вложенность (`byFeature`, `byLayer`) — когда появятся независимые модули. Таблица и направления зависимостей — 6.8 |
| 13 | Имя сущности сессии | **`LearningSession`** (таблица `sessions`) | Чтобы не конфликтовать с `HttpSession` и `org.hibernate.Session` |
| 14 | Восстановление пароля | **Вне скоупа** | Регистрация + вход + выход |
| 15 | Обязательность онбординга | **`HandlerInterceptor`** | Пока у пользователя нет `Profile`, любой защищённый маршрут (кроме `/onboarding`, `/logout`, `/login`, `/register`, `/error`, `/css/**`) отвечает 302 на `/onboarding`. Реализация — `OnboardingInterceptor` + `WebConfig`; `SecurityConfig` не менялся, `/onboarding` и `/progress` уже под `anyRequest().authenticated()` |
| 16 | Редактирование профиля | **Только `about`, `interests`, `level`** | Имя и email задаются при регистрации и на странице прогресса только показываются |
| 17 | Ввод интересов | **Одно поле через запятую, показ — «чипсами»** | Без JS-редактора тегов: `Interests.split()` режет строку для показа, `Interests.normalize()` чистит и склеивает перед сохранением (`" технологии , спорт ,,"` → `"технологии, спорт"`) |

### Следствия для архитектуры

- Вся работа с ИИ изолируется за интерфейсом **`AiGateway`** (Этап 5, сделан) — смена провайдера,
  модели или endpoint не ломает остальное приложение.
- Для офлайн-разработки и тестов есть spring-profile **`local`** со заглушкой
  `AiGateway` (предзаполненный контент без API-ключа) и опция резервной модели (Этап 5, 3.5).
- `LearningSession` — центральная сущность: статус продвижения
  `PROPOSED → READ → THINK → DISCUSS → DONE`, тема, текст, вопросы, переписка, результат.
- Все данные привязаны к пользователю; на маршрутах `/sessions/{id}/...` — проверка
  владельца (Этап 10).

---

## 3. Текущее состояние проекта

> Сверять с кодом при каждой сессии. Здесь — только то, что реально есть.

### 3.1 Инфраструктура (Этапы 0–1, выполнены)

- Spring Boot **4.1.1** (parent), Java **21** в pom, Maven Wrapper (`./mvnw`).
- `pom.xml`: `thymeleaf`, `webmvc`, `data-jpa`, `security`, `validation`,
  `thymeleaf-extras-springsecurity6` (3.1.5), `spring-boot-starter-liquibase` + `liquibase-core`,
  `postgresql` (runtime), **Spring AI 2.0.1** (BOM + `spring-ai-starter-model-openai`),
  `docker-compose`, Lombok, тест-стартеры (`spring-boot-starter-webmvc-test`,
  `spring-security-test`, `spring-boot-testcontainers`, `testcontainers-postgresql`).
- `compose.yaml`: Postgres 17, БД/пользователь/пароль `textik`, том, healthcheck, порт **5433**.
- `application.yaml`: datasource (env с дефолтами), `ddl-auto: validate`, `open-in-view: false`,
  Liquibase на `classpath:db/changelog/db.changelog-master.yaml`, плейсхолдеры
  `spring.ai.openai.*` с **обезличенными** именами переменных (`AI_API_KEY`, `AI_BASE_URL`,
  `AI_MODEL`, `AI_TEMPERATURE`) и `app.ai.fallback`. Комментарии в файле — только «почему».
- `db/changelog/db.changelog-master.yaml` подключает `changesets/001-create-users`,
  `002-create-profiles`, `003-create-sessions`, `004-create-questions`, `005-create-chat-messages`,
  `006-create-session-reviews`. Схема упрощена для MVP: нет индексов; убраны `proposals`,
  `avatar_url`, `updated_at`, `display_name`, `created_at`/`finished_at` (sessions), `q_index`,
  `answered_at`, `q_type` (и его CHECK). Остались CHECK на `level`, `status`, `role`.
  **Changeset-файлы 001–006 не редактируем** (сломается checksum) — только новые.
- Приложение стартует ~8 с, Liquibase применяет миграции при старте.

### 3.2 Домен (Этап 2, выполнен)

- `entity` (6 JPA-сущностей): `AppUser` (email=логин, `username` — имя, bcrypt-хэш),
  `Profile` («О себе», интересы, уровень), `LearningSession`, `Question` (single-choice),
  `ChatMessage`, `SessionReview`.
- `enums`: `SessionStatus`, `ChatRole` (USER/ASSISTANT). `QuestionType` не нужен — все вопросы
  одного типа. Уровень оставлен `String`, валидность гарантируют CHECK-ограничения.
- `repository`: 6 интерфейсов Spring Data JPA с derived-запросами под экраны, включая
  `findByIdAndUserId` (проверка владельца сессии) и `countBySessionIdAndRole` (лимит обсуждения).
- Особенности маппинга:
  - `@GeneratedValue(strategy = IDENTITY)` (в БД `GENERATED BY DEFAULT AS IDENTITY`);
  - `questions.options` — JSONB через `@JdbcTypeCode(SqlTypes.JSON)` → `List<String>`;
  - текстовые колонки помечены `columnDefinition = "text"` — иначе `ddl-auto: validate`
    ругается на расхождение `text` / `varchar(255)`;
  - `session_reviews.created_at` — `Instant` (→ `timestamptz`), заполняется приложением;
  - `Profile.userId` — assigned-id без ассоциации на `AppUser` (5.1, последний пункт).
- `ddl-auto: validate` проходит: схема и сущности согласованы.

### 3.3 Аутентификация (Этап 3, выполнен)

Spring Security 7, form login + BCrypt, миграции не потребовались — колонки `users` уже подходят.

| Файл | Содержимое |
|------|------------|
| `config/SecurityConfig` | бины `PasswordEncoder` (BCrypt), `UserDetailsService` (поверх `AppUserRepository.findByEmailIgnoreCase`), `SecurityContextRepository` (`HttpSessionSecurityContextRepository`), `SessionAuthenticationStrategy` (`ChangeSessionIdAuthenticationStrategy`), `SecurityFilterChain` |
| `security/AppUserDetails` | record в сессии: `id`, `email`, `displayName`, `passwordHash`; `getUsername()` = email по контракту; единственная роль `ROLE_USER` |
| `service/UserRegistrationService` | нормализует email в нижний регистр, проверяет занятость, кодирует пароль, ловит гонку по уникальному индексу |
| `exceptions/EmailAlreadyTakenException` | Бизнес-ошибка «email уже занят» |
| `dto/RegistrationForm` | record: email, имя, пароль 8..72 символов + подтверждение (верхняя граница — лимит BCrypt в 72 байта) |
| `controller/AuthController` | `GET /register`, `POST /register`, `GET /login`; внедрение зависимостей — Lombok `@RequiredArgsConstructor` |
| `controller/HomeController` | `GET /` — временный главный экран («Привет, <имя>!»), на Этапе 6 дорастёт |
| `resources/templates` | `fragments.html`, `onboarding.html`, `progress.html`, `register.html`, `login.html`, `home.html` + `static/css/app.css` |

Поведение: **email = логин** (`.usernameParameter("email")`, 5.1); после регистрации —
сразу главный экран, без второго ввода (контекст ставит контроллер, id сессии меняет
`SessionAuthenticationStrategy` — 5.1). Открыты `/register`, `/login`, `/css/**`, `/error`,
остальное за авторизацией; CSRF включён; ролей не заводили — у всех `ROLE_USER`. Все сообщения
валидации и ошибок — русские.

### 3.4 Профиль, онбординг, прогресс (Этап 4, выполнен)

| Слой | Классы |
|------|--------|
| `dto` | `ProfileForm` — record с `about` (≤ 2000), `interests` (≤ 500), `level` (`@Pattern` `A1\|A2\|B1`), фабрика `empty()`, список `LEVELS` |
| `service` | `ProfileService` (`exists`, `find`, `create`, `update`), `SessionService` (`findHistory`) |
| `utility` | `Interests` — `split()` для показа, `normalize()` для записи |
| `security` | `OnboardingInterceptor` — 302 на `/onboarding`, пока профиля нет |
| `config` | `WebConfig` — регистрирует интерсептор и список исключений |
| `controller` | `OnboardingController` (`/onboarding`), `ProfileController` (`/progress`) |
| Шаблоны | `fragments.html` (`head(title)`, `nav(active)`), `onboarding.html`, `progress.html`, обновлены `home.html`, `login.html`, `register.html` |

Поведение:

- **Онбординг обязателен.** Пользователь без `Profile` на любом защищённом маршруте получает
  302 на `/onboarding` (15). `/onboarding` для пользователя с профилем — 302 на `/progress`,
  чтобы нельзя было затереть профиль повторно. Уровень выбирается «карточками»-радио
  (`A1`/`A2`/`B1`), интересы — одним полем через запятую; при сохранении 302 на `/`.
- **Профиль.** `/progress` показывает имя, email, уровень, «чипсы» интересов и текст «О себе»;
  ниже форма редактирования тех же трёх полей. Имя и email только показываются (16). Успех →
  302 на `/progress?saved` + зелёное уведомление «Профиль сохранён». Ошибка валидации →
  форма с русскими сообщениями, в базе старое значение.
- **Прогресс.** Секция «История сессий»: счётчик `sessionCount` и список тем со статусом;
  пока данных нет — пустое состояние «Здесь появятся ваши сессии». Сессии отдаёт
  `SessionRepository.findByUserIdOrderByIdDesc(userId)`, поле `profile` в БД не заводим.
- Интерфейс — русский, стили в `static/css/app.css`: навигация с активным пунктом, карточки
  уровней, «чипсы», пустое состояние.

### 3.5 Слой ИИ (Этап 5, выполнен)

| Слой | Классы | Роль |
|------|--------|------|
| `service` | `AiGateway` | Интерфейс: `generateTopics`, `generateText`, `generateQuestions`, `continueDiscussion`, `generateReview` |
| `service` | `SpringAiGateway` | Боевая реализация: `ChatClient.Builder` + `BeanOutputConverter`; любая ошибка → `AiGatewayException` |
| `service` | `StubAiGateway` | Заглушка для офлайна: 3 темы, текст ~400 слов, 5 вопросов, реплики по ходу разговора, итоги |
| `service` | `FallbackAiGateway` | Декоратор «страховка»: при `AiGatewayException` основной модели отвечает резервная |
| `dto` | `TopicProposal`, `GeneratedText`, `GeneratedQuestion`, `ReviewData` | Records structured output |
| `exceptions` | `AiGatewayException` | Единая ошибка слоя ИИ (endpoint, пустой ответ, неразбираемый JSON) |
| `utility` | `AiPrompts` | 5 промптов (темы, текст, вопросы, обсуждение, ревью) как `@UtilityClass`-константы |
| `config` | `AiGatewayConfig`, `AiProperties` | Выбор реализации: профиль `local` → заглушка, иначе → боевая; `app.ai.fallback` → обёртка резервной |

Поведение:

- **Смена провайдера — только конфиг.** Три переменные окружения: `AI_BASE_URL`, `AI_MODEL`,
  `AI_API_KEY` (+ необязательный `AI_TEMPERATURE`). Работает любой OpenAI-совместимый endpoint
  (Яндекс, OpenAI, Groq, Ollama). Код при смене провайдера не меняется.
- **Офлайн без ключа.** Профиль `local` (`--spring.profiles.active=local`) подставляет
  `StubAiGateway`: весь цикл Read→Think→Discuss→Review проходит без сети.
- **Страховка.** `AI_FALLBACK=stub` оборачивает боевую модель в `FallbackAiGateway`: при сбое
  основной (таймаут, пустой ответ, неразбираемый JSON) ученик получает заготовленный контент,
  а в лог уходит предупреждение. Упали обе — выбрасывается `AiGatewayException`, у которого
  резервная ошибка в `suppressed`.
- **Structured Output без `response_format`.** JSON-схему добавляет `BeanOutputConverter.getFormat()`
  в текст промпта, поэтому не требуется поддержка `json_schema` на стороне провайдера — иначе
  часть OpenAI-совместимых API не заработала бы. Ответ в ```json```-блоке тоже разбирается.
- `continueDiscussion` и `generateReview` читают `chatMessages` и `questions` сессии, поэтому
  вызывающий код обязан быть `@Transactional` (см. 5.1).

### 3.6 Тесты — 86, `./mvnw test` зелёные (~35 с)

| Слой | Классы | Тестов |
|------|--------|--------|
| Юнит, домен (без Spring и БД) | `QuestionTests` 4, `SessionStatusTests` 5, `LearningSessionTests` 7 | 16 |
| Юнит, слой ИИ (без Spring и сети) | `SpringAiGatewayTests` 10, `StubAiGatewayTests` 5, `FallbackAiGatewayTests` 3 | 18 |
| Репозитории (`@DataJpaTest` + Testcontainers) | `AppUserRepositoryTests` 4, `ProfileRepositoryTests` 4, `SessionRepositoryTests` 6, `QuestionRepositoryTests` 5, `ChatMessageRepositoryTests` 4, `SessionReviewRepositoryTests` 4 | 27 |
| Веб-слой (MockMvc) | `RegistrationTests` 6, `LoginTests` 4, `OnboardingTests` 7, `ProgressTests` 5 | 22 |
| Конфигурация бинов | `AiGatewayWiringTests` 1, `LocalAiGatewayWiringTests` 1 | 2 |
| Smoke | `TextikV1ApplicationTests` (контекст + схема из миграций) | 1 |

- Базовые классы в `support/`: `PostgresRepositoryTest` (`@DataJpaTest`), `PostgresIntegrationTest`
  (`@SpringBootTest` + контейнер + профиль `test`), `WebTest` (`+ @AutoConfigureMockMvc`),
  `PostgresTestConfiguration` (контейнер как Spring-бин), `TestFixtures` (фабрики данных:
  `user`, `userWithPassword`, `userWithProfile`, `session`), `FakeChatModel` (`@UtilityClass`:
  `answeringWith`, `recording`, `failing`).
- Требования к тестам — 6.1–6.4.
- MockMvc-тесты авторизуются через `user(AppUserDetails.of(user))`: с `user(...)` из
  `spring-security-test` в контексте лежит чужой principal, и интерсептор профиль не увидит.
- Слой ИИ в тестах **не ходит в сеть**: `SpringAiGateway` собирается с
  `ChatClient.builder(FakeChatModel...)` — так проверяются и промпт, и разбор JSON без Spring-контекста;
  `FallbackAiGateway` проверяется на Mockito-заглушках `AiGateway`.
- `LocalAiGatewayWiringTests` поднимает контекст с профилями `test` + `local` и убеждается, что
  бин `AiGateway` — заглушка: офлайн-режим не может отвалиться молча.

### 3.7 Проверено на живом приложении

`spring-boot:run` + `curl` с cookie-jar: аноним → 302 на `/login`; регистрация → 302 на `/`,
затем `/` → 302 на `/onboarding`; онбординг отдаёт форму и принимает
`" технологии , спорт ,, "` → в базе `технологии, спорт`; главная доступна (200);
`/progress` показывает email, уровень и чипсы; `level=C2` → форма с ошибкой
«Выберите уровень A1, A2 или B1»; успешное сохранение → 302 на `/progress?saved` и «Профиль
сохранён»; выход → `/login?logout`, после него `/` снова 302 на `/login`. Строка в БД:
`about=Теперь читаю книги`, `interests=книги, музыка`, `level=B1`.
CSRF-токен после входа перевыпускается (5.1).

Слой ИИ на живом приложении **не проверялся**: ключа и выбранной модели нет (5, раздел 5).
Проверено автоматическими тестами: разбор JSON, отказоустойчивость, оба режима wiring (3.6).
Живой вызов с настоящим ключом — первая задача Этапа 10 (README с переменными окружения).

### 3.8 Чего ещё нет

- Главный экран с плашками этапов, выбор темы, чтение (Этап 6); вопросы (7), обсуждение (8),
  ревью (9). Слой ИИ к ним готов, но ещё не вызывается из приложения.
- Сервис сессий, который сохраняет сгенерированный контент в БД и двигает статусы
  `PROPOSED → READ → THINK → DISCUSS → DONE` (Этапы 6–9).
- Восстановление пароля, аватар с загрузкой, статистика между сессиями — вне скоупа MVP.

---

## 4. План задач по этапам

### Этап 0. Проверка заготовки — выполнен
- [x] `./mvnw compile` и `./mvnw test` проходят; приложение стартует.

### Этап 1. Инфраструктура: зависимости + Postgres + конфиг — выполнен
- [x] Зависимости в `pom.xml`, `compose.yaml` (Postgres 17, порт 5433), `application.yaml`,
      Liquibase-миграции `001`–`006` (схема — 3.1).

### Этап 2. Доменная модель — выполнен
- [x] 6 сущностей, 2 enum, 6 репозиториев, 16 юнит-тестов домена (3.2).

### Этап 3. Аутентификация — выполнен
- [x] Form login, BCrypt, регистрация с автологином, выход, 10 тестов MockMvc + ручная
      проверка живого приложения (3.3, 3.4).

### Этап 4. Онбординг + профиль + прогресс (каркас) — выполнен
- [x] Онбординг: «О себе», интересы (через запятую), уровень (A1/A2/B1). Имя задаётся при
      регистрации (`username`) и показывается в профиле. После входа без профиля → редирект
      на онбординг (`OnboardingInterceptor`, 15).
- [x] Профиль: просмотр + редактирование `about`, `interests`, `level`; имя и email только
      показываются (16). `Profile` грузится по `userId` через `ProfileService` (5.1).
- [x] Прогресс: профиль + история сессий (`SessionService.findHistory`, пустое состояние;
      данные появятся на Этапах 6–9).
- [x] Общие фрагменты `head`/`nav` — дублирования разметки больше нет.
- [x] Проверка: 12 новых тестов MockMvc + ручная проверка живого приложения (3.4, 3.6, 3.7).

### Этап 5. Слой ИИ (AiGateway) — выполнен
- [x] Интерфейс `AiGateway` + records structured output в `dto`: `TopicProposal`,
      `GeneratedText`, `GeneratedQuestion`, `ReviewData`.
- [x] Методы: `generateTopics`, `generateText`, `generateQuestions`, `continueDiscussion`,
      `generateReview`.
- [x] Реализация через Spring AI `ChatClient` (OpenAI-стартер, любой совместимый endpoint),
      Structured Output через `BeanOutputConverter`.
- [x] Промпты (`utility/AiPrompts`):
      1. 3 идеи тем по интересам/уровню → экран выбора;
      2. полный текст 350–500 слов под уровень;
      3. 5–10 вопросов (один правильный из 4 вариантов) с правильными ответами и объяснениями;
      4. обсуждение — «держимся темы, возвращаем ушедшего» (лимит 3–5 обменов считает Этап 8);
      5. ревью — резюме, частые ошибки, оценка уровня разговора.
- [x] **Заглушка** для профиля `local` без API-ключа + `FallbackAiGateway` как резервная модель.
- [x] Проверка: 18 тестов слоя ИИ + 2 теста wiring, сеть не используется; полный цикл офлайн
      проверяется на Этапах 6–9. Живой вызов с настоящим ключом — Этап 10.

### Этап 6. Главный экран + выбор темы + чтение (Read)
- [ ] Home: название приложения, 4 плашки этапов с временем + «Начать сессию».
- [ ] Создание сессии → генерация 3 тем → `select.html` (карточки: название + описание +
      «Начать чтение»). Три предложения хранятся в HTTP-сессии: `sessions.proposals` в БД нет,
      обновление страницы перегенерирует их (осознанный MVP-трейдофф, 5.1).
- [ ] Выбор темы → генерация полного текста → `read.html` (название, описание, текст,
      «К вопросам»).
- [x] Экраны подключают `head`/`nav` из `fragments.html` (Этап 4), вопрос про layout закрыт (5).
- [ ] Проверка: «Начать сессию» → 3 темы → читаем текст.

### Этап 7. Вопросы (Think)
- [ ] Генерация 5–10 вопросов при переходе «К вопросам», сохранение в `questions`.
- [ ] Экран по одному вопросу; после ответа — детерминированная проверка (сравнение
      выбранного варианта с `correct_answer`).
- [ ] При неверном ответе — объяснение. В конце — «К обсуждению».
- [ ] Проверка: отвечаю на все вопросы, видны объяснения, ведётся счёт правильно/неправильно.

### Этап 8. Обсуждение (Discuss)
- [ ] `discuss.html`: диалоговое окно (переписка из `chat_messages`), поле ввода.
- [ ] Отправка → ответ ИИ (промпт этапа 5, пункт 4). Счётчик обменов: после 3–5 со стороны
      юзера активируется «Перейти к ревью».
- [ ] Проверка: отвлекаюсь от темы — ИИ возвращает к тексту; после лимита доступно ревью.

### Этап 9. Ревью (Review)
- [ ] `generateReview()`: резюме, число ошибок в тестах, оценка уровня разговора, разбор
      основных ошибок.
- [ ] `review.html`: поздравление + результаты + «На главный экран».
- [ ] Сохранение в `session_reviews`, статус сессии → DONE, попадание в историю.
- [ ] «Прогресс» показывает реальную историю сессий (сортировка по `id` — дат у сессий нет,
      5.1).
- [ ] Проверка: полный круг Read→Think→Discuss→Review, итоги в истории.

### Этап 10. Доводка и защита
- [ ] Проверка владельца сессии на всех `/sessions/{id}/...` (чужая → 404/403).
- [ ] Обработка ошибок ИИ: таймауты, ретраи, понятное сообщение + повтор, защита от
      дублирующей генерации.
- [ ] Загрузочные состояния, валидация форм.
- [ ] Тесты: проверка ответа, проверка владельца, генерация через stub.
- [ ] README: как получить API-ключ (Яндекс/OpenAI/другой провайдер), какие поля конфига
      заполнить: `AI_API_KEY`, `AI_BASE_URL`, `AI_MODEL`, необязательный `AI_FALLBACK`;
      живой вызов всех пяти методов `AiGateway`.
- [ ] `./mvnw test` зелёный, полный пользовательский сценарий без падений.

---

## 5. Открытые вопросы

- [ ] Получить API-ключ любого OpenAI-совместимого провайдера и выбрать модель; прописать
      `AI_API_KEY`, `AI_BASE_URL`, `AI_MODEL`. **Этап 5 кодом не блокирован** — слой готов и
      работает офлайн на заглушке; блокирует только живой вызов (Этап 10, README).
- [ ] Прогресс между сессиями пользователя (статистика по времени, «выучено слов» и т.п.) —
      вне скоупа MVP, кандидаты на следующую итерацию.

> Вопрос про layout-шаблон закрыт на Этапе 4: `fragments.html` с фрагментами `head(title)` и
> `nav(active)` — дублирования `<head>` больше нет, и на Этапах 6–9 новые страницы подключают
> фрагменты одной строкой `th:replace`.

### 5.1 Принятые технические решения (не пересматривать)

- **Провайдер ИИ не зашит в код.** Только `spring.ai.openai.*` с переменными окружения
  `AI_BASE_URL` / `AI_MODEL` / `AI_API_KEY`: подходит любой OpenAI-совместимый endpoint
  (Яндекс, OpenAI, Groq, Ollama). Добавить второго провайдера — новый класс, реализующий
  `AiGateway`; переключить — одна переменная. Резервная модель включается `AI_FALLBACK=stub`
  (`FallbackAiGateway`): при `AiGatewayException` основной модели отвечает заглушка.
- **Structured Output делаем через `BeanOutputConverter`, а не через `response_format`:** схема
  JSON-схемой добавляется в текст промпта (`getFormat()`), поэтому не требуется поддержка
  `json_schema` на стороне провайдера — иначе часть OpenAI-совместимых API не заработала бы.
  Списки (`List<TopicProposal>`, `List<GeneratedQuestion>`) идут «голым» JSON-массивом,
  ответ в блоке ```json``` тоже разбирается. Нативный structured output Spring AI 2 — кандидат
  на следующую итерацию, если у выбранного провайдера он гарантированно работает.
- **`AiGateway` лежит в плоском пакете `service`**, без вложенности `service/ai` (6.8): фича
  одна, роль одна. Из этого следует документированное исключение из правила «`service` не знает
  про `dto`» — шлюз обменивается records-ами `dto`. Обратной зависимости нет: `dto` не знает
  про `service`.
- **`continueDiscussion` и `generateReview` принимают `LearningSession`** и читают её
  `chatMessages` и `questions` (LAZY, при `open-in-view: false`). Вызывающий код обязан быть
  `@Transactional`, иначе `LazyInitializationException`. Если понадобится вызывать вне
  транзакции — переходить на `fetch = EAGER` или на отдельные запросы.
- **`ReviewData` не содержит числа вопросов:** их считает код по `Question.isCorrect()`,
  поручать счёт модели нельзя. В `session_reviews` они и так есть (`SessionReview.of`).
- **Замена `Profile` (сущность) на `@Profile` (аннотация) в одном файле невозможна** — Java
  не различает импорт по имени. Поэтому профиль подставляется на бинах в `config`
  (`AiGatewayConfig`), а не аннотацией на классе реализации. Побочный эффект, который стоит
  знать: **любая ошибка компиляции в одном классе «ломает» Lombok во всём модуле** — annotation
  processing не доходит до конца, и в ошибках появляются фантомные «cannot find symbol: getRole()»
  на сгенерированных Lombok-методах. Настоящая первая ошибка — в начале списка.
- **Changeset-файлы 001–006 не трогаем** — иначе сломается checksum. Новые изменения — новыми
  файлами (нумерация с 007).
- **`src/test/resources/application.yaml` заводить нельзя** — одноимённый файл затеняет
  `src/main/resources/application.yaml`, в тестах пропадают `spring.ai.openai.*`, и контекст падает
  с «At least one credential source must be specified». Тестовые настройки — только
  `application-test.yaml` + `@ActiveProfiles("test")`. Если файл всё-таки создадут — чистить
  `target/test-classes` (Maven не удаляет старые ресурсы).
- **Testcontainers поднимается как Spring-бин** (`PostgresTestConfiguration` +
  `@ServiceConnection`), а не полем `@Container`: контекст кэшируется, контейнер один на набор
  тестов. В `pom.xml` есть неиспользуемый `testcontainers-junit-jupiter` — `@Container` применять
  не нужно. Первый запуск скачивает образ и `testcontainers/ryuk`, дальше быстро.
- **Boot 4: автоконфигурация разъехалась по отдельным стартерам** —
  `spring-boot-autoconfigure` не содержит Security/JPA. В тестах MockMvc даёт
  `spring-boot-starter-webmvc-test`, `@AutoConfigureMockMvc` лежит в
  `org.springframework.boot.webmvc.test.autoconfigure`, `@DataJpaTest` — в
  `...boot.data.jpa.test.autoconfigure`; аутентификация — `spring-security-test` (`csrf()`,
  `authenticated()`).
- **Liquibase 5.0.x: change type `addCheckConstraint` отсутствует** — CHECK-ограничения
  добавляются через `sql`-изменения в YAML-changelog.
- **Spring AI OpenAI-стартер не поднимается без непустого `api-key`** — в `application.yaml`
  стоят плейсхолдеры.
- **Логин по email, а не по `username`:** фильтр ждёт параметр `username`, поэтому в
  `SecurityConfig` указано `.usernameParameter("email")` — при переименовании поля формы
  придётся менять и его.
- **Автологин после регистрации** — официальный приём Spring Security: контроллер сохраняет
  контекст через бин `SecurityContextRepository` (без бина фильтр не прочитал бы контекст из
  сессии) и вызывает `SessionAuthenticationStrategy` — защита от session fixation.
- **В Spring Security 7 `MessageSourceDelegatingAuthenticationFailureHandler` удалён** — текст
  ошибки входа не переопределяется через message source; «Неверный email или пароль» рисует
  наш `login.html` по флагу `?error`.
- **CSRF-токен перевыпускается при успешном входе** — `POST /logout` со старым токеном отдаёт
  403, со свежим — 302 на `/login?logout`. В MockMvc это не видно (`with(csrf())` подставляет
  токен на каждый запрос), Thymeleaf подставляет скрытое поле `_csrf` сам.
- **Схема упрощена под MVP:** `sessions.proposals` нет (темы хранятся в HTTP-сессии),
  `sessions.created_at`/`finished_at` нет (история сортируется по `id`; нужны даты — changeset
  007 `ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now()`), `q_type` нет (все вопросы
  single-choice), `users.display_name` нет (роль играет `username`).
- **`Profile` не связан ассоциацией с `AppUser`:** `profiles.user_id` — общий PK (PK = FK на
  `users.id`), что несовместимо со Spring Data JPA («must be manually assigned before persist()»
  и «This class does not define an IdClass»). **Решено:** `user_id` — assigned-id
  (`Long userId`), профиль берётся из `profileRepository.findById(currentUserId)`, каскада нет
  (удаление профиля делает `ON DELETE CASCADE` в БД). Понадобится `user.getProfile()` — changeset
  007 с отдельным `id BIGINT GENERATED ... IDENTITY` + `UNIQUE (user_id)`.
- **Проверка «профиль заполнен» живёт в MVC-слое, а не в `SecurityConfig`:** `HandlerInterceptor`
  вызывается после фильтров, поэтому анонимного пользователя до него не доходит, а
  `Authentication` уже в `SecurityContext` — principal доступен без запроса к БД. Проверка
  `profiles.exists(id)` дешёвая (PK), но выполняется на каждый защищённый запрос; если понадобится
  оптимизация — кеш в сессии.
- **В Thymeleaf имя переменной `session` зарезервировано** (`${session}` — web variables map):
  цикл по истории сессий назван `past`.
- **`th:field` и `BindingResult` не переживают подмену model-атрибута:** на пустой
  `ProfileForm` в модели остаётся объект формы из `@ModelAttribute` с его ошибками. Поэтому при
  ошибке валидации `ProfileController` докладывает только остальные атрибуты страницы
  (`addCommonAttributes`), а `profileForm` не перезаписывает.

---

## 6. Требования к коду и тестам

### 6.1 Читаемость тестов

1. **Русский `@DisplayName` на каждом тестовом классе и каждом методе.** Имена классов и методов
   — английские. Текст начинается с `Тест, в котором мы проверяем, что ...` и описывает
   поведение, а не код: не «проверяем findByEmail», а «проверяем, что пользователь находится
   по email».
2. **Комментарии `//Arrange`, `//Act`, `//Assert`** (без пробела после `//`) — единственная
   разрешённая разметка в тестах. **Пустой этап не размечается:** если в `Arrange` нечего
   подготовить, строки `//Arrange` и пустой строки после неё нет — остаётся `//Act` и,
   возможно, `//Assert`. Проверка перед коммитом (должна молчать):
   `for f in $(grep -rl '//Arrange' src/test/java); do awk '/^[[:space:]]*\/\/Arrange[[:space:]]*$/{getline l; if (l ~ /^[[:space:]]*$/) print FILENAME":"NR}' "$f"; done`
3. **Один тест — одно поведение**, до ~3 ассертов; если проверок много — это несколько тестов.
4. Тест не повторяет структуру продакшн-кода: юнит-тест домена собирает состояние напрямую,
   интеграционный — через публичные методы сущности.

### 6.2 Слои тестов

| Слой | Что тестируем | Как запускаем |
|------|---------------|---------------|
| Юнит | Доменная логика: `SessionStatus.next()`, `Question.answer()`, `LearningSession.userTurns()` | Чистый JUnit 5 + AssertJ, без Spring и БД |
| Репозитории | Запросы Spring Data, маппинг, JSONB, согласие схемы и сущностей (`ddl-auto: validate`) | `@DataJpaTest` + Testcontainers |
| Сервисы (с Этапа 5) | Бизнес-логика, переходы статусов | `@SpringBootTest` + Testcontainers, ИИ заглушен |
| Веб (MockMvc) | Роуты, валидация форм, редиректы, CSRF | `WebTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`) |
| Smoke | Контекст поднимается, схема из миграций доступна | `@SpringBootTest` + Testcontainers |

- **Один класс на репозиторий**, имя `<Репозиторий>Tests`; базовый класс
  `PostgresRepositoryTest` собирает `@DataJpaTest` + Liquibase + Testcontainers + профиль `test`.
- **Внешние вызовы (LLM) в тестах не ходят в сеть** — заглушка `AiGateway` или Mockito на
  границе сервиса.

### 6.3 Изоляция данных

- **Никакого ручного `@Transactional` на тестовых классах** — rollback внутри `@DataJpaTest`
  встроенный, это его штатный механизм.
- В тестах на `@SpringBootTest` rollback не работает, поэтому данные делаем **уникальными**:
  email через `TestFixtures.randomEmail()`, никаких фиксированных значений, на которые мог бы
  повлиять другой тест.
- Общие фикстуры — в `TestFixtures` и в самих сущностях (фабричные `of(...)`, `proposed(...)`),
  а не копипаст в каждом тесте.
- Тесты не зависят от порядка выполнения и не делят состояние.

### 6.4 БД в тестах

- Тесты поднимают **свой** Postgres в Testcontainers (`postgres:17-alpine`) через
  `@ServiceConnection`; состояние контейнера `docker compose` на результат не влияет, а сам
  compose в тестах отключён (`spring.docker.compose.enabled: false` в `application-test.yaml`).
- Схема приходит из Liquibase — те же changeset-файлы, что и в проде, поэтому тесты заодно
  проверяют, что миграции и сущности согласованы.

### 6.5 Комментарии: почему их почти нет

Правило действует на весь проект, а не только на тесты. Комментарий не компилируется и не
проверяется: он устаревает при первой правке, дублирует видимое в коде и создаёт иллюзию
понимания. Вместо комментария — имя, маленький метод, ясная структура.

| Вместо комментария | Что делаем |
|--------------------|-----------|
| `// проверяем, что статус следующий` | Имя теста и `@DisplayName`: `goesToRead` / «Тест, в котором мы проверяем, что чтение переходит к вопросам» |
| `// считаем количество правильных ответов` | Имя метода: `countBySessionIdAndIsCorrectFalse` |
| `// здесь костыль, потому что ...` | Рефакторинг, чтобы «почему» исчезло; если нельзя — короткий комментарий **почему**, с указанием, что будет удалено |
| Рассуждения, «TODO на Этапе N», значения констант | Живут здесь, в `PROJECT_PLAN.md` (5.1) — там актуализируются централизованно |

Правила:

1. Комментарий не пересказывает код: если его можно убрать, не потеряв смысл, — убираем.
2. Вместо «что делает» — улучшаем имя; вместо «почему так» — рефакторим.
3. В тестах допустимы только `//Arrange //Act //Assert` (6.1), и только на непустых этапах.
4. В конфигах (`application.yaml`, `compose.yaml`, changelog) допустимы короткие пояснения
   «почему, а не что» — пересказ свойств не пишем.
5. Разработчик **обязан** удалять комментарии, которые стали неактуальны, вместе с правкой кода:
   устаревший комментарий хуже отсутствующего.

Сейчас: в `main`-коде Java комментариев нет вообще; в `application.yaml` — четыре пояснения
«почему» (плейсхолдеры `spring.ai.openai.*` и `app.ai.fallback`); в тестах — только
`//Arrange //Act //Assert`.

### 6.6 Служебные классы: `@UtilityClass`

Класс, существующий только ради статических методов (`TestFixtures`, будущие `TestData`,
хелперы), помечаем `@UtilityClass` (пакет `lombok.experimental`; `lombok.UtilityClass`
не существует). Lombok сам делает класс `final`, члены — статическими, конструктор —
приватным; инстанцировать такой класс физически нельзя, и это выражает аннотация, а не ручной код.

- Ключевое слово `static` руками **не пишем** — Lombok делает члены статическими сам
  (проверено `javap`: в байт-коде они всё равно `public static`).
- Применено к `support/TestFixtures`, `support/FakeChatModel`, `utility/Interests`,
  `utility/AiPrompts`; Lombok подключён и для `default-testCompile`
  (`maven-compiler-plugin`, `annotationProcessorPaths`).
- `@Component`-классы, `@TestConfiguration` и базовые тест-классы (`PostgresRepositoryTest`,
  `PostgresTestConfiguration`, `WebTest`) аннотацию не получают: им нужен Spring или
  наследование.

### 6.7 Внедрение зависимостей: `@RequiredArgsConstructor`

Класс с `private final`-полями получает `@RequiredArgsConstructor` — Lombok генерирует
конструктор в порядке объявления полей. Ручной конструктор «присвоил поля — и всё» длиннее, его
легко забыть обновить при добавлении зависимости, и он пересказывает то, что уже выражает
аннотация (то же, что 6.5 и 6.6).

- Проверено по всему проекту: `private final`-поля есть у `controller/AuthController`
  (4 зависимости), `service/UserRegistrationService` (2), `service/SpringAiGateway` (1),
  `service/FallbackAiGateway` (2) — все на `@RequiredArgsConstructor`. Lombok подключён и для
  main, и для test.
- Исключения: JPA-сущности (поля не могут быть `final`, конструктор — часть доменного API),
  `EmailAlreadyTakenException` (считает строку и зовёт `super(...)`), классы без полей
  (`HomeController`, `SecurityConfig`), тесты (зависимости внедряются полями `@Autowired`).
- Проверка перед коммитом: `grep -rn "this\.[a-zA-Z]* = " src/main/java` — если в найденных
  конструкторах идёт только присваивание `final`-полей, конструктор лишний.

### 6.8 Структура пакетов

Пакет — это ответ на вопрос «какую роль играет класс», а не «в какой фиче он живёт». Фичи у
проекта одна, ролей десять, поэтому пакеты плоские: без `byFeature`, без `byLayer`, без
вложенности. Вложенность появится, только когда появятся независимые модули.

| Пакет | Что в нём лежит | Чего там не бывает |
|-------|-----------------|-------------------|
| `entity` | 6 JPA-сущностей: `AppUser`, `Profile`, `LearningSession`, `Question`, `ChatMessage`, `SessionReview` | Spring-сервисов, HTTP |
| `enums` | `SessionStatus`, `ChatRole` | `@Entity` (перечисления маппятся `@Enumerated`) |
| `repository` | 6 интерфейсов Spring Data JPA | Бизнес-логики и `@Transactional` (транзакции — в сервисах) |
| `service` | `UserRegistrationService`, `ProfileService`, `SessionService`, `AiGateway` + `SpringAiGateway`, `StubAiGateway`, `FallbackAiGateway` | HTTP-аннотаций, `Model`, `HttpServletRequest` |
| `dto` | `RegistrationForm`, `ProfileForm`, records structured output: `TopicProposal`, `GeneratedText`, `GeneratedQuestion`, `ReviewData` | JPA-аннотаций и связей с БД |
| `exceptions` | `EmailAlreadyTakenException`, `AiGatewayException` (+ будущие: сессия не найдена, не владелец) | `@Service`, обработчиков `@ControllerAdvice` |
| `controller` | `AuthController`, `HomeController`, `OnboardingController`, `ProfileController` | Бизнес-логики, `@Entity`, прямого `Repository` |
| `config` | `SecurityConfig`, `WebConfig` (регистрация `HandlerInterceptor`), `AiGatewayConfig`, `AiProperties` | Контроллеров, сервисов, сущностей |
| `security` | `AppUserDetails`, `OnboardingInterceptor` | `@Configuration` — это уже `config` |
| `utility` | stateless-хелперы main (`@UtilityClass`): `Interests`, `AiPrompts` | `@Component`, `@Service`, состояния |

Направления зависимостей (единственные разрешённые):

```
controller → service → repository → entity
      ↓          ↓            ↓
     dto   exceptions/enums  enums
  security/config
```

- `controller` знает `service`, `dto`, `security` (кто вошёл) и `config` (Spring сам);
  `service` **не знает** про `controller`. `dto` в знает только `AiGateway`: обмен records-ами
  structured output — сознательное исключение, обоснованное в 5.1.
- `service` знает `dto` (только `AiGateway`), `entity`, `exceptions`, `utility`, `enums`
  и Spring AI — но не HTTP. Именно поэтому `AiGateway` отделяет ИИ от остального приложения.
- `security` знает `service` и `entity`: `OnboardingInterceptor` спрашивает `ProfileService`,
  есть ли профиль. Исключение из «`security` — лист», обоснованное в 5.1.
- `repository` знает только `entity` и `enums`.
- `entity` и `enums` не знают ни о ком, кроме `jakarta.persistence` и Lombok: домен не должен
  зависеть от Spring, иначе его нельзя unit-тестировать без контекста (поэтому 16 доменных
  тестов идут без Spring).
- `utility` и `exceptions` — листья: не зависят ни от кого, от них не зависят.
  `Interests` — тоже лист: `ProfileService` вызывает его статически, внедрять нечего.
  `AiPrompts` — лист: константы, которые читает `SpringAiGateway`.
- Тесты повторяют структуру main: `entity/*`, `enums/*`, `controller/*`, `repository/*`,
  `service/*`, `config/*`; тестовая инфраструктура — в `support` (`PostgresTestConfiguration`,
  базовые классы, `TestFixtures`, `FakeChatModel`). `support` — это не `utility`: у базовых
  классов нет статических методов.
- **После переезда пакетов — обязателен `./mvnw clean test`:** в `target/` остаются `.class`
  старых пакетов, и без `clean` JUnit падает с `NoClassDefFoundError` на старых именах.
