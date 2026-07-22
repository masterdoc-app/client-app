package pro.masterdoc.client.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class FeatureLabelsTest {
    @Test
    fun titleRu_coversAllFeatures() {
        assertEquals("Заявки", FeatureId.Tickets.titleRu())
        assertEquals("Доска", FeatureId.Board.titleRu())
        assertEquals("Карта", FeatureId.Map.titleRu())
        assertEquals("Графики", FeatureId.Charts.titleRu())
        assertEquals("Оборудование", FeatureId.Equipment.titleRu())
        assertEquals("Профиль", FeatureId.Profile.titleRu())
        assertEquals("Наставник", FeatureId.Copilot.titleRu())
        assertEquals("Пользователи", FeatureId.Users.titleRu())
    }
}
