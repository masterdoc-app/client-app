package pro.masterdoc.client.navigation

/** Russian labels for product features (UI / profile). */
fun FeatureId.titleRu(): String =
    when (this) {
        FeatureId.Tickets -> "Заявки"
        FeatureId.Board -> "Доска"
        FeatureId.Map -> "Карта"
        FeatureId.Charts -> "ППР"
        FeatureId.Equipment -> "Оборудование"
        FeatureId.Profile -> "Профиль"
        FeatureId.BlackBox -> "Чёрный ящик"
        FeatureId.Users -> "Админ"
    }
