package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class MaintenanceMapDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesMapWithItems() {
        val raw =
            """
            {
              "id":"m1",
              "assetId":"a1",
              "orgId":"o1",
              "title":"Карта обслуживания: кран-балка",
              "status":"draft",
              "source":"ai_generated",
              "items":[
                {
                  "id":"i1",
                  "title":"Осмотр балок",
                  "kind":"inspection",
                  "interval":{"every":1,"unit":"days"},
                  "criticality":"high",
                  "sourceRef":"ежедневно"
                }
              ]
            }
            """.trimIndent()
        val map = json.decodeFromString(MaintenanceMapDto.serializer(), raw)
        assertEquals("draft", map.status)
        assertEquals(1, map.items.size)
        assertEquals("Осмотр балок", map.items[0].title)
        assertEquals(1, map.items[0].interval.every)
    }
}
