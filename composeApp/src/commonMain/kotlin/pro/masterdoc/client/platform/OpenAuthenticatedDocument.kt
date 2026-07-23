package pro.masterdoc.client.platform

/**
 * Opens a document fetched with Bearer auth (blob URL on web).
 */
expect suspend fun openAuthenticatedDocument(
    url: String,
    bearerToken: String,
    filename: String,
    mimeType: String = "application/pdf",
)
