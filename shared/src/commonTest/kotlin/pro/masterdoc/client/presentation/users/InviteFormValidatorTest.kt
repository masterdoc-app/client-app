package pro.masterdoc.client.presentation.users

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InviteFormValidatorTest {
    @Test
    fun rejectsEmptyFeatures() {
        assertEquals(
            InviteFormError.FeaturesRequired,
            InviteFormValidator.validate(
                email = "a@b.com",
                givenName = "A",
                familyName = "B",
                features = emptySet(),
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
                features = setOf("board"),
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
                features = setOf("board"),
            ),
        )
    }
}
