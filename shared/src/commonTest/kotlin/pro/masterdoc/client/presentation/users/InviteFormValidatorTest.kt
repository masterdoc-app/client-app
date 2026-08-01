package pro.masterdoc.client.presentation.users

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InviteFormValidatorTest {
    @Test
    fun rejectsEmptyRoles() {
        assertEquals(
            InviteFormError.RolesRequired,
            InviteFormValidator.validate(
                email = "a@b.com",
                givenName = "A",
                familyName = "B",
                roles = emptySet(),
            ),
        )
    }

    @Test
    fun rejectsBlankEmail() {
        assertEquals(
            InviteFormError.EmailInvalid,
            InviteFormValidator.validate(
                email = " ",
                givenName = "A",
                familyName = "B",
                roles = setOf("board"),
            ),
        )
    }

    @Test
    fun acceptsValid() {
        assertNull(
            InviteFormValidator.validate(
                email = "a@b.com",
                givenName = "A",
                familyName = "B",
                roles = setOf("manager"),
            ),
        )
    }
}
