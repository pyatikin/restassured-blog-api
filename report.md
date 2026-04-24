# Test Report — Blog API

### API тесты (`BlogApiHomeworkTest.java`)

| Тест | Описание |
|------|----------|
| `shouldRegisterUserWithValidRequiredFields` | Регистрация с валидными данными, проверка всех полей в ответе |
| `shouldReturnValidationErrorForInvalidEmailOnRegistration` | Регистрация с невалидным email → 400, error объект |
| `shouldLoginWithValidCredentials` | Логин существующим пользователем → token + refresh_token |
| `shouldReturnUnauthorizedForWrongPassword` | Логин с неверным паролем → 401 |
| `shouldRefreshAccessToken` | Получение нового access token по refresh_token |
| `shouldReturnCurrentUserProfile` | GET /api/profile авторизованным пользователем |
| `shouldUpdateCurrentUserProfile` | PUT /api/profile с новым firstName/lastName |
| `shouldReturnPaginatedPostsList` | GET /api/posts?page=1&limit=10, проверка пагинации |
| `shouldFilterPostsByCategory` | Создать пост с category=technology, проверить фильтр |
| `shouldCreatePublishedPost` | POST /api/posts с isDraft=false, проверить author.email |
| `shouldCreateDraftPost` | POST /api/posts с isDraft=true |
| `shouldReturnOnlyCurrentUserPosts` | GET /api/posts/my — только посты текущего пользователя |
| `shouldReturnFeedPosts` | Создать второго пользователя, его пост, GET /api/posts/feed — чужие посты |
| `shouldReturnSinglePostById` | Создать пост, GET /api/posts/{id} — id, title, statistics |
| `shouldUpdateExistingPost` | Создать пост, PUT /api/posts/{id} с новым title/description |
| `shouldDeletePost` | Создать пост, DELETE, затем GET → 404 |
| `shouldAddPostToFavorites` | POST /api/posts/{id}/favorite с isFavorite=true |
| `shouldReturnFavoritePosts` | Добавить пост в избранное, GET /api/posts/favorites |
| `shouldUploadImageFileForPost` | Multipart upload PNG → id, url, mimeType, filename |
| `shouldReturnUploadedFileMetadata` | Upload, затем GET /api/files/{id} |
| `shouldCreateUserReport` | Зарегистрировать второго пользователя, POST /api/profile/report/{id} |

### UI тесты

| Тест | Файл | Описание |
|------|------|----------|
| `shouldRegisterUserFromRegisterPage` | `RegisterUiTest.java` | Открыть /register, заполнить все поля, клик — редирект на /login |
| `shouldLoginWithExistingUserCredentials` | `LoginUiTest.java` | Зарегистрировать пользователя через API, открыть /login, войти — уйти со страницы |

---

## Теги

| Тег | Тесты | Почему |
|-----|-------|--------|
| `smoke` | `shouldRegisterUserWithValidRequiredFields` | Без регистрации ничего не работает |
| `smoke` | `shouldLoginWithValidCredentials` | Базовая проверка что auth вообще живёт |
| `smoke` | `shouldReturnCurrentUserProfile` | Проверка авторизованного доступа |
| `smoke` | `shouldCreatePublishedPost` | Главная фича приложения |
| `smoke` | `shouldReturnSinglePostById` | Основное чтение контента |
| `smoke` | `shouldReturnUserProfileById` *(ExamplesTest)* | Проверка profile по id |
| `smoke` | `shouldCreatePostForAuthorizedUser` *(ExamplesTest)* | Создание поста — core flow |
| `regression` | Валидация email, неверный пароль | Edge cases auth |
| `regression` | Refresh token, update profile | Фичи вокруг auth |
| `regression` | Пагинация, фильтр по категории | Параметры листинга |
| `regression` | Черновик, my posts, update/delete поста | Полный CRUD постов |
| `regression` | Favorites, file upload, file metadata, report | Периферийные фичи |
| `e2e` | `shouldReturnFeedPosts` | Требует двух пользователей — самый тяжёлый API-тест |
| `e2e` | `shouldRegisterUserFromRegisterPage` | UI — полный браузерный флоу |
| `e2e` | `shouldLoginWithExistingUserCredentials` | UI — полный браузерный флоу |

**Логика выбора:**
- `smoke` — минимальный набор для проверки, что система вообще работает, запускается быстро на каждый пуш
- `regression` — полное покрытие всех фич, запускается после smoke
- `e2e` — тяжёлые сценарии: браузер или несколько пользователей, запускается последними

---

## Пайплайн (`.github/workflows/api-tests.yml`)

Запускается на каждый пуш в `master` и через `workflow_dispatch`.

Три последовательные job — каждый стартует только если предыдущая прошёла:

```
smoke → regression → e2e
```

```yaml
jobs:
  smoke:     mvn clean test -Dtest.groups=smoke
  regression: needs: smoke  →  mvn clean test -Dtest.groups=regression
  e2e:        needs: regression  →  mvn clean test -Dtest.groups=e2e
```

**Логика выбора:**
- Быстрый фидбек — если smoke упал, regression не запускается
- e2e идут последними, они дольше всего