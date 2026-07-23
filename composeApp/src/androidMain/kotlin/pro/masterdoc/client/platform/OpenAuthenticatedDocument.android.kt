package pro.masterdoc.client.platform

actual suspend fun openAuthenticatedDocument(
    url: String,
    bearerToken: String,
    filename: String,
    mimeType: String,
) {
    // Android shell is not the primary target for equipment PDF open yet.
    throw UnsupportedOperationException("Opening documents is not wired on Android")
}
