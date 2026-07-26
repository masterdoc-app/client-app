package pro.masterdoc.client.presentation.audit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuditEventDescriptionTest {
    @Test
    fun describesAdminInvite() {
        assertEquals(
            "Пригласил пользователя",
            AuditEventDescription.title(
                action = "admin.invite",
                method = "POST",
                path = "/admin/users/invites",
            ),
        )
    }

    @Test
    fun describesSiteCreate() {
        assertEquals(
            "Создал площадку",
            AuditEventDescription.title(action = "site.create", method = "POST", path = "/sites"),
        )
    }

    @Test
    fun describesAssetMove() {
        assertEquals(
            "Переместил оборудование на другую площадку",
            AuditEventDescription.title(action = "asset.move", method = "POST", path = "/assets/1/move"),
        )
    }

    @Test
    fun describesUiNavSelectWithDestination() {
        assertEquals(
            "Открыл раздел «Оборудование»",
            AuditEventDescription.title(
                action = "ui.shell.nav.select",
                method = "UI",
                path = "MainShell",
                requestSummary = """{"index":"2","destination":"Equipment"}""",
            ),
        )
    }

    @Test
    fun describesUiShellOpen() {
        assertEquals(
            "Открыл главный экран",
            AuditEventDescription.title(
                action = "ui.MainShell.open",
                method = "UI",
                path = "MainShell",
            ),
        )
    }

    @Test
    fun describesGenericGetPath() {
        assertEquals(
            "Просмотрел площадки",
            AuditEventDescription.title(action = "get:/sites", method = "GET", path = "/sites"),
        )
    }

    @Test
    fun describesUnknownActionWithMethodPath() {
        val title =
            AuditEventDescription.title(
                action = "weird.thing",
                method = "POST",
                path = "/x",
            )
        assertTrue(title.contains("POST"))
        assertTrue(title.contains("/x"))
    }

    @Test
    fun describesAuditList() {
        assertEquals(
            "Открыл журнал действий",
            AuditEventDescription.title(action = "audit.list", method = "GET", path = "/admin/audit"),
        )
    }

    @Test
    fun nullActionFallsBackToMethodPath() {
        assertEquals(
            "Запрос GET /health",
            AuditEventDescription.title(action = null, method = "GET", path = "/health"),
        )
    }
}
