package pro.masterdoc.client.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppUpdatePolicyTest {
    @Test
    fun versionCodeFromSemVer_matchesKkalScanScheme() {
        assertEquals(10000, versionCodeFromSemVer(1, 0, 0))
        assertEquals(10020, versionCodeFromSemVer(1, 0, 20))
        assertEquals(20103, versionCodeFromSemVer(2, 1, 3))
    }

    @Test
    fun majorFromVersionCode_dividesBy10000() {
        assertEquals(1, majorFromVersionCode(10020))
        assertEquals(2, majorFromVersionCode(20000))
    }

    @Test
    fun selectUpdateFlow_nullWhenNoNewer() {
        assertNull(selectUpdateFlow(10000, 10000))
        assertNull(selectUpdateFlow(10020, 10000))
    }

    @Test
    fun selectUpdateFlow_immediateOnMajorBump() {
        assertEquals(AppUpdateFlow.Immediate, selectUpdateFlow(10020, 20000))
    }

    @Test
    fun selectUpdateFlow_silentOnMinorOrPatch() {
        assertEquals(AppUpdateFlow.Silent, selectUpdateFlow(10000, 10100))
        assertEquals(AppUpdateFlow.Silent, selectUpdateFlow(10000, 10001))
    }
}
