# RuStore Console — чеклист публикации Fixaverse

Тексты: [`copy.md`](copy.md)  
Ассеты: `icon-512.png`, `upload/screenshot-01.png`, `upload/screenshot-02.png`

## Первый bind пакета (обязательно один раз)

RuStore API **не создаёт** новое приложение. Package берётся из AAB при первой загрузке в Console:

1. `gh workflow run rustore-release.yml -f skip_publish=true -f version_name=1.0.0 -f version_code=10000`
2. Скачать artifact `client-app-release-aab`
3. В [console.rustore.ru](https://console.rustore.ru) → создать приложение / «Загрузить» → выбрать этот AAB (`pro.masterdoc.client`)
4. Заполнить карточку из `copy.md`, иконку и скриншоты
5. Дальше публикации — через workflow **без** `skip_publish` или MCP `publish_aab`

## Совпадение витрины и APK (модерация)

Название и иконка **на устройстве** должны совпадать с карточкой:

- `composeApp/.../res/values/strings.xml` → `app_name` = `Fixaverse` (как в Console)
- `AndroidManifest` → `@mipmap/ic_launcher` из `store/rustore/icon-512-opaque.png`
- В Console — то же имя `Fixaverse` и тот же `icon-512.png`

Если API версий даёт `403` на `pro.masterdoc.client`: Console → Разработчик → API RuStore → ключ → добавить приложение Fixaverse (ключ, созданный до появления приложения, его не покрывает).

## Перед загрузкой

- [x] Приложение с package `pro.masterdoc.client` видно в Console (после первого AAB)
- [x] Загрузить `icon-512.png`
- [x] Загрузить скриншоты 1080×1920 (`upload/screenshot-01…04.png`)
- [x] Вставить название, краткое и полное описание из `copy.md`
- [x] Выбрать категорию «Полезные инструменты» и рейтинг `18+`
- [x] Указать приложение как бесплатное
- [ ] Добавить ссылку на политику конфиденциальности на странице приложения (если потребует модерация)
- [x] Контакт поддержки: `mail@antonbutov.com`, сайт `https://fixaverse.ru`
- [x] Обоснование `ACCESS_*_LOCATION` + типы данных «местоположение»

## Загрузка версии

- [x] Подписанный release AAB `1.0.0` / `10000`
- [x] Загрузить AAB + PEPK-подпись (см. `.secrets/dist/`)
- [x] Комментарий модератору с тестовым входом (`copy.md` / `.secrets/rustore-moderator.md`)
- [x] Отправлено на модерацию: версия `1.0.0(10000)`, статус «Ожидает модерацию»

## После публикации

- [ ] Проверить карточку в каталоге RuStore
- [ ] Установить приложение из RuStore на тестовое Android-устройство
- [ ] Проверить вход, загрузку заявок и сценарий обязательного обновления
