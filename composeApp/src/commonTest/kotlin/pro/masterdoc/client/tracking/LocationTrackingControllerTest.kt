package pro.masterdoc.client.tracking

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import pro.masterdoc.client.auth.EngineerLocationDto
import pro.masterdoc.client.auth.EngineerLocationsGateway
import pro.masterdoc.client.auth.UpdateEngineerLocationRequest
import pro.masterdoc.client.auth.WorkOrderDto

class LocationTrackingControllerTest {
    @Test
    fun startingInProgressPingsImmediatelyForOpenAssignedWork() =
        runBlocking {
            val repository = FakeLocationsRepository()
            val controller =
                LocationTrackingController(
                    repository = repository,
                    locationSource = FakeLocationSource(),
                    displayName = { "Иван Петров" },
                    dispatcher = Dispatchers.Unconfined,
                )

            controller.onWorkOrdersChanged(listOf(workOrder(status = "new")))
            controller.onStartedInProgress()
            delay(1)

            assertEquals(1, repository.updates.size)
            assertEquals("Иван Петров", repository.updates.single().displayName)
            controller.close()
        }

    @Test
    fun clearingOpenAssignedWorkDeletesLocationAndStopsTracking() =
        runBlocking {
            val repository = FakeLocationsRepository()
            val controller =
                LocationTrackingController(
                    repository = repository,
                    locationSource = FakeLocationSource(),
                    dispatcher = Dispatchers.Unconfined,
                )

            controller.onWorkOrdersChanged(listOf(workOrder(status = "in_progress")))
            delay(1)
            controller.onWorkOrdersChanged(emptyList())
            delay(1)

            assertEquals(1, repository.deletes)
            controller.close()
        }

    private fun workOrder(status: String): WorkOrderDto =
        WorkOrderDto(
            id = "wo-1",
            orgId = "org",
            type = "emergency",
            status = status,
            title = "Leak",
            assetId = "asset",
            siteId = "site",
            dueAt = "2026-07-30",
            source = "manual",
            createdAt = "now",
            updatedAt = "now",
            assigneeId = "engineer",
        )
}

private class FakeLocationSource : EngineerLocationPingSource {
    override suspend fun currentLocation(): EngineerLocationPoint = EngineerLocationPoint(55.75, 37.62, 8.0)
}

private class FakeLocationsRepository : EngineerLocationsGateway {
    val updates = mutableListOf<UpdateEngineerLocationRequest>()
    var deletes = 0

    override suspend fun putMe(body: UpdateEngineerLocationRequest): EngineerLocationDto {
        updates += body
        return EngineerLocationDto(
            userId = "engineer",
            lat = body.lat,
            lon = body.lon,
            recordedAt = "now",
        )
    }

    override suspend fun list(): List<EngineerLocationDto> = emptyList()

    override suspend fun deleteMe() {
        deletes++
    }
}
