package pro.masterdoc.client.presentation.users

object InviteFormValidator {
    fun validate(
        email: String,
        givenName: String,
        familyName: String,
        features: Set<String>,
    ): InviteFormError? {
        if (email.isBlank() || '@' !in email) return InviteFormError.EmailInvalid
        if (givenName.isBlank()) return InviteFormError.GivenNameRequired
        if (familyName.isBlank()) return InviteFormError.FamilyNameRequired
        if (features.isEmpty()) return InviteFormError.FeaturesRequired
        return null
    }
}

enum class InviteFormError {
    EmailInvalid,
    GivenNameRequired,
    FamilyNameRequired,
    FeaturesRequired,
}
