package pro.masterdoc.client.tracking

actual fun createEngineerLocationPingSource(): EngineerLocationPingSource =
    object : EngineerLocationPingSource {
        override suspend fun currentLocation(): EngineerLocationPoint? = null
    }
