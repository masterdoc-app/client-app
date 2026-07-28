package pro.masterdoc.client.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeatureLabelsTest {
    @Test
    fun titleRu_coversAllFeatures() {
        assertEquals("Заявки", FeatureId.Tickets.titleRu())
        assertEquals("Доска", FeatureId.Board.titleRu())
        assertEquals("Карта", FeatureId.Map.titleRu())
        assertEquals("ППР", FeatureId.Charts.titleRu())
        assertEquals("Оборудование", FeatureId.Equipment.titleRu())
        assertEquals("Профиль", FeatureId.Profile.titleRu())
        assertEquals("Чёрный ящик", FeatureId.BlackBox.titleRu())
        assertEquals("Админ", FeatureId.Users.titleRu())
        assertEquals(FeatureId.entries.size, FeatureId.entries.map { it.titleRu() }.size)
        assertTrue(FeatureId.entries.none { it.wireValue == "copilot" })
        assertTrue(FeatureId.entries.none { it.titleRu() == "Наставник" })
    }
}
