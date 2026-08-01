package pro.masterdoc.client.navigation

/** Russian labels for product features (UI / profile). */
fun FeatureId.titleRu(): String =
    when (this) {
        FeatureId.Tickets -> "Заявки"
        FeatureId.Board -> "Доска"
        FeatureId.Engineer -> "Инженер"
        FeatureId.Map -> "Карта"
        FeatureId.Charts -> "ППР"
        FeatureId.Equipment -> "Оборудование"
        FeatureId.Profile -> "Профиль"
        FeatureId.BlackBox -> "Чёрный ящик"
        FeatureId.Ai -> "ИИ"
        FeatureId.Users -> "Админ"
    }
