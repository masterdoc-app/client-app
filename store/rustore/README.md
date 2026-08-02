# RuStore store assets — Fixaverse

Файлы карточки приложения:

| Файл | Назначение |
|------|------------|
| `icon-mark.svg` | Векторная метка Fixaverse (по бренду `masterdoc-toir` / `logo-fixaverse`) |
| `icon-512.png` | Иконка 512×512, растеризация SVG (растры в landing слишком мягкие) |
| `icon-512-opaque.png` | RGB на тёмном фоне — для загрузки в RuStore Console |
| `upload/screenshot-*.png` | Портретные скриншоты 1080×1920 |
| `copy.md` | Тексты карточки и комментарий модератору |
| `console-checklist.md` | Чеклист ручной публикации |

## Публикация через API

```bash
cd client-app
python3 -m venv .venv-rustore
.venv-rustore/bin/pip install -r scripts/requirements-rustore.txt
export RUSTORE_KEY_ID=...
export RUSTORE_PRIVATE_KEY=...
export AAB_PATH=composeApp/build/outputs/bundle/release/composeApp-release.aab
.venv-rustore/bin/python scripts/rustore_publish.py
```

Скрипт создаёт или переиспользует черновик, загружает AAB и отправляет версию на модерацию. Иконку и скриншоты при первой публикации загрузите в RuStore Console вручную.

Если API возвращает `403`, проверьте права ключа в разделе «API RuStore» и привязку package `pro.masterdoc.client` к карточке приложения.
