package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RoleRouterTest {
    @Test
    fun technologist_mapsToTechnologPath() {
        val route = RoleRouter.resolve(listOf("technologist"))
        assertEquals(RoleRoute.App("/technolog/"), route)
    }

    @Test
    fun technologist_caseInsensitive() {
        val route = RoleRouter.resolve(listOf("Technologist"))
        assertEquals(RoleRoute.App("/technolog/"), route)
    }

    @Test
    fun otherRoles_noWebApp() {
        val route = RoleRouter.resolve(listOf("dispatcher", "engineer"))
        assertIs<RoleRoute.NoWebApp>(route)
        assertEquals(listOf("dispatcher", "engineer"), route.roles)
    }

    @Test
    fun emptyRoles_noWebApp() {
        assertIs<RoleRoute.NoWebApp>(RoleRouter.resolve(emptyList()))
    }
}
