package pro.masterdoc.client

import kotlin.test.Test
import kotlin.test.assertEquals
import pro.masterdoc.client.auth.GatewayHttpException

class LoginErrorMessageTest {
    @Test
    fun unauthorizedShowsCredentialError() {
        assertEquals(
            "Неверный email или пароль",
            loginErrorMessage(GatewayHttpException(401, "raw backend detail")),
        )
    }

    @Test
    fun upstreamAndNetworkFailuresShowTemporaryError() {
        assertEquals(
            "Сервис входа временно недоступен",
            loginErrorMessage(GatewayHttpException(502, "raw backend detail")),
        )
        assertEquals(
            "Сервис входа временно недоступен",
            loginErrorMessage(Exception("network detail")),
        )
    }

    @Test
    fun otherHttpFailuresShowGenericError() {
        assertEquals(
            "Не удалось войти. Попробуйте ещё раз",
            loginErrorMessage(GatewayHttpException(400, "raw backend detail")),
        )
    }
}
