package pro.masterdoc.client.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class WorkOrdersRepositoryTest {
    private val config =
        AuthConfig(
            gatewayBaseUrl = "https://api.test",
            clientId = "c",
            redirectUri = "https://app.test/callback",
        )

    @Test
    fun getBoardDecodesWeeks() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    assertEquals("GET", method)
                    assertTrue(url.contains("/work-orders/board"))
                    GatewayHttpResponse(
                        200,
                        """{"weeks":[{"weekStart":"2026-07-20","items":[]}]}""",
                    )
                }
            val repo = WorkOrdersRepository(config = config, http = http, tokenStore = tokens)
            val board = repo.getBoard(weeks = 4)
            assertEquals(1, board.weeks.size)
            assertEquals("2026-07-20", board.weeks[0].weekStart)
        }

    @Test
    fun listFiltersByAssignee() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    assertEquals("GET", method)
                    assertEquals("https://api.test/work-orders?assigneeId=engineer-1", url)
                    GatewayHttpResponse(
                        200,
                        """[{"id":"wo-1","orgId":"o","type":"emergency","status":"new","title":"T","assetId":"a","siteId":"s","dueAt":"2026-07-22","source":"api","createdAt":"t","updatedAt":"t","assigneeId":"engineer-1"}]""",
                    )
                }
            val repo = WorkOrdersRepository(config = config, http = http, tokenStore = tokens)
            val items = repo.list(assigneeId = "engineer-1")
            assertEquals(listOf("wo-1"), items.map { it.id })
            assertEquals("engineer-1", items.single().assigneeId)
        }

    @Test
    fun listFiltersByCreatedBy() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    assertEquals("GET", method)
                    assertEquals("https://api.test/work-orders?createdBy=customer-1", url)
                    GatewayHttpResponse(
                        200,
                        """[{"id":"wo-1","orgId":"o","type":"emergency","status":"new","title":"T","assetId":"a","siteId":"s","dueAt":"2026-07-22","source":"api","createdAt":"t","updatedAt":"t","createdBy":"customer-1","description":"Leak"}]""",
                    )
                }
            val repo = WorkOrdersRepository(config = config, http = http, tokenStore = tokens)
            val items = repo.list(createdBy = "customer-1")
            assertEquals(listOf("wo-1"), items.map { it.id })
            assertEquals("customer-1", items.single().createdBy)
            assertEquals("Leak", items.single().description)
        }

    @Test
    fun listFiltersByAssigneeAndCreatedBy() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    assertEquals("GET", method)
                    assertEquals(
                        "https://api.test/work-orders?assigneeId=engineer-1&createdBy=customer-1",
                        url,
                    )
                    GatewayHttpResponse(200, "[]")
                }
            val repo = WorkOrdersRepository(config = config, http = http, tokenStore = tokens)
            val items = repo.list(assigneeId = "engineer-1", createdBy = "customer-1")
            assertEquals(emptyList(), items)
        }

    @Test
    fun getBoardDecodesDurationHours() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    assertEquals("GET", method)
                    assertTrue(url.contains("/work-orders/board"))
                    GatewayHttpResponse(
                        200,
                        """
                        {
                          "weeks": [{
                            "weekStart": "2026-07-20",
                            "items": [{
                              "id": "wo-1",
                              "orgId": "o",
                              "type": "emergency",
                              "status": "new",
                              "title": "T",
                              "assetId": "a",
                              "siteId": "s",
                              "dueAt": "2026-07-22",
                              "durationHours": 16,
                              "source": "api",
                              "createdAt": "t",
                              "updatedAt": "t"
                            }]
                          }]
                        }
                        """.trimIndent(),
                    )
                }
            val repo = WorkOrdersRepository(config = config, http = http, tokenStore = tokens)
            val board = repo.getBoard(weeks = 1)
            assertEquals(16, board.weeks[0].items[0].durationHours)
        }

    @Test
    fun patchSendsDurationHours() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            var body: String? = null
            val http =
                RecordingGatewayHttpClient { method, url, _, b ->
                    assertEquals("PATCH", method)
                    assertTrue(url.endsWith("/work-orders/wo-1"))
                    body = b
                    GatewayHttpResponse(
                        200,
                        """{"id":"wo-1","orgId":"o","type":"emergency","status":"new","title":"T","assetId":"a","siteId":"s","dueAt":"2026-07-22","durationHours":24,"source":"api","createdAt":"t","updatedAt":"t"}""",
                    )
                }
            val repo = WorkOrdersRepository(config = config, http = http, tokenStore = tokens)
            repo.patch("wo-1", durationHours = 24)
            val json = Json.parseToJsonElement(body!!).jsonObject
            assertEquals(24, json["durationHours"]!!.toString().toInt())
        }

    @Test
    fun patchSendsLocationSnapshot() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            var body: String? = null
            val http =
                RecordingGatewayHttpClient { method, url, _, requestBody ->
                    assertEquals("PATCH", method)
                    assertTrue(url.endsWith("/work-orders/wo-1"))
                    body = requestBody
                    GatewayHttpResponse(
                        200,
                        """{"id":"wo-1","orgId":"o","type":"emergency","status":"in_progress","title":"T","assetId":"a","siteId":"s","dueAt":"2026-07-22","source":"api","createdAt":"t","updatedAt":"t"}""",
                    )
                }
            val repo = WorkOrdersRepository(config = config, http = http, tokenStore = tokens)
            repo.patch(
                "wo-1",
                status = "in_progress",
                location = EngineerLocationSnapshot(lat = 55.75, lon = 37.61, accuracyM = 12.0),
            )
            val location = Json.parseToJsonElement(body!!).jsonObject["location"]!!.jsonObject
            assertEquals(55.75, location["lat"]!!.toString().toDouble())
            assertEquals(37.61, location["lon"]!!.toString().toDouble())
            assertEquals(12.0, location["accuracyM"]!!.toString().toDouble())
        }

    @Test
    fun patchSendsNullAssignee() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            var body: String? = null
            val http =
                RecordingGatewayHttpClient { method, url, _, b ->
                    assertEquals("PATCH", method)
                    assertTrue(url.endsWith("/work-orders/wo-1"))
                    body = b
                    GatewayHttpResponse(
                        200,
                        """{"id":"wo-1","orgId":"o","type":"emergency","status":"new","title":"T","assetId":"a","siteId":"s","dueAt":"2026-07-22","durationHours":8,"assigneeId":null,"source":"api","createdAt":"t","updatedAt":"t"}""",
                    )
                }
            val repo = WorkOrdersRepository(config = config, http = http, tokenStore = tokens)
            repo.patch("wo-1", clearAssignee = true)
            val json = Json.parseToJsonElement(body!!).jsonObject
            assertTrue(json.containsKey("assigneeId"))
            assertEquals("null", json["assigneeId"].toString())
        }

    @Test
    fun patchSendsAssigneeId() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            var body: String? = null
            val http =
                RecordingGatewayHttpClient { method, url, _, b ->
                    assertEquals("PATCH", method)
                    assertTrue(url.endsWith("/work-orders/wo-1"))
                    body = b
                    GatewayHttpResponse(
                        200,
                        """{"id":"wo-1","orgId":"o","type":"emergency","status":"new","title":"T","assetId":"a","siteId":"s","dueAt":"2026-07-22","durationHours":8,"assigneeId":"engineer-1","source":"api","createdAt":"t","updatedAt":"t"}""",
                    )
                }
            val repo = WorkOrdersRepository(config = config, http = http, tokenStore = tokens)
            val updated = repo.patch("wo-1", assigneeId = "engineer-1")
            val json = Json.parseToJsonElement(body!!).jsonObject
            assertEquals("engineer-1", json["assigneeId"]!!.toString().trim('"'))
            assertEquals("engineer-1", updated.assigneeId)
        }

    @Test
    fun createEncodesAttachmentIds() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            var body = ""
            val http =
                RecordingGatewayHttpClient { method, url, _, requestBody ->
                    assertEquals("POST", method)
                    assertEquals("https://api.test/work-orders", url)
                    body = requestBody.orEmpty()
                    GatewayHttpResponse(
                        200,
                        """{"id":"wo-1","orgId":"o","type":"emergency","status":"new","title":"Фото","assetId":"a","siteId":"s","dueAt":"2026-07-22","source":"api","createdAt":"t","updatedAt":"t","attachmentIds":["att-1","att-2"]}""",
                    )
                }
            val repo = WorkOrdersRepository(config = config, http = http, tokenStore = tokens)

            val created =
                repo.create(
                    CreateWorkOrderRequest(
                        type = "emergency",
                        title = "Фото",
                        assetId = "a",
                        siteId = "s",
                        dueAt = "2026-07-22",
                        attachmentIds = listOf("att-1", "att-2"),
                    ),
                )

            val json = Json.parseToJsonElement(body).jsonObject
            assertEquals("""["att-1","att-2"]""", json["attachmentIds"].toString())
            assertEquals(listOf("att-1", "att-2"), created.attachmentIds)
        }

    @Test
    fun attachPostsAttachmentIds() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            var body = ""
            val http =
                RecordingGatewayHttpClient { method, url, _, requestBody ->
                    assertEquals("POST", method)
                    assertEquals("https://api.test/work-orders/wo-1/attachments", url)
                    body = requestBody.orEmpty()
                    GatewayHttpResponse(
                        200,
                        """{"id":"wo-1","orgId":"o","type":"emergency","status":"new","title":"Фото","assetId":"a","siteId":"s","dueAt":"2026-07-22","source":"api","createdAt":"t","updatedAt":"t","attachmentIds":["att-1"]}""",
                    )
                }
            val repo = WorkOrdersRepository(config = config, http = http, tokenStore = tokens)

            val updated = repo.attach("wo-1", listOf("att-1"))

            val json = Json.parseToJsonElement(body).jsonObject
            assertEquals("""["att-1"]""", json["attachmentIds"].toString())
            assertEquals(listOf("att-1"), updated.attachmentIds)
        }

    @Test
    fun managerKpisUsesReportPeriodAndDecodesRanking() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    assertEquals("GET", method)
                    assertEquals("https://api.test/reports/manager-kpis?from=2026-07-01&to=2026-07-31", url)
                    GatewayHttpResponse(
                        200,
                        """
                        {
                          "from":"2026-07-01",
                          "to":"2026-07-31",
                          "mttrHours":4.5,
                          "mttrSampleSize":3,
                          "mtbfHours":120.0,
                          "mtbfSampleSize":2,
                          "plannedCount":10,
                          "emergencyCount":4,
                          "plannedHours":40.0,
                          "emergencyHours":12.0,
                          "pprOnTime":7,
                          "pprLate":1,
                          "pprOpenOverdue":1,
                          "pprOpenPending":1,
                          "backlogUnder7d":2,
                          "backlog7to30d":1,
                          "backlogOver30d":0,
                          "backlogOverdue":1,
                          "downtimeRanking":[{"assetId":"asset-1","downtimeHours":18.5,"openIntervals":1}],
                          "availabilityPercent":92.1
                        }
                        """.trimIndent(),
                    )
                }
            val report = WorkOrdersRepository(config = config, http = http, tokenStore = tokens).managerKpis(
                from = "2026-07-01",
                to = "2026-07-31",
            )

            assertEquals("2026-07-01", report.from)
            assertEquals(4.5, report.mttrHours)
            assertEquals("asset-1", report.downtimeRanking.single().assetId)
            assertEquals(18.5, report.downtimeRanking.single().downtimeHours)
        }

    @Test
    fun marketLeaderReportsUseExactEndpointsAndDecodeContracts() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val responses =
                mapOf(
                    "/reports/kpi-trends?from=2026-07-01&to=2026-07-31" to
                        """{"bucket":"day","points":[{"bucketStart":"2026-07-01","mttrHours":4.5,"mttrSampleSize":3,"mtbfHours":80.0,"mtbfSampleSize":3,"availabilityPercent":92.1}]}""",
                    "/reports/reactive-completion?from=2026-07-01&to=2026-07-31" to
                        """{"createdCount":120,"closedCount":95,"completionRatePercent":79.2,"emergencyCount":40,"plannedCount":80,"reactivePercent":33.3}""",
                    "/reports/engineer-workload?from=2026-07-01&to=2026-07-31" to
                        """{"engineers":[{"userId":"engineer-1","closedCount":12,"hours":34.5}]}""",
                    "/reports/failure-frequency?from=2026-07-01&to=2026-07-31" to
                        """{"assets":[{"assetId":"asset-1","emergencyCount":7}]}""",
                )
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    assertEquals("GET", method)
                    val path = url.removePrefix("https://api.test")
                    GatewayHttpResponse(200, responses.getValue(path))
                }
            val repo = WorkOrdersRepository(config = config, http = http, tokenStore = tokens)

            val trends = repo.kpiTrends("2026-07-01", "2026-07-31")
            val reactive = repo.reactiveCompletion("2026-07-01", "2026-07-31")
            val workload = repo.engineerWorkload("2026-07-01", "2026-07-31")
            val frequency = repo.failureFrequency("2026-07-01", "2026-07-31")

            assertEquals("day", trends.bucket)
            assertEquals(4.5, trends.points.single().mttrHours)
            assertEquals(79.2, reactive.completionRatePercent)
            assertEquals("engineer-1", workload.engineers.single().userId)
            assertEquals(7, frequency.assets.single().emergencyCount)
        }

    @Test
    fun equipmentWorkOrdersUsesAssetAndPeriod() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    assertEquals("GET", method)
                    assertEquals(
                        "https://api.test/reports/equipment-work-orders?assetId=pump-1&from=2026-07-01&to=2026-07-31",
                        url,
                    )
                    GatewayHttpResponse(
                        200,
                        """[{"id":"wo-1","orgId":"o","type":"emergency","status":"new","title":"Утечка","assetId":"pump-1","siteId":"s","dueAt":"2026-07-22","source":"api","createdAt":"2026-07-10T00:00:00Z","updatedAt":"t"}]""",
                    )
                }
            val items =
                WorkOrdersRepository(config = config, http = http, tokenStore = tokens)
                    .equipmentWorkOrders(assetId = "pump-1", from = "2026-07-01", to = "2026-07-31")
            assertEquals("wo-1", items.single().id)
            assertEquals("Утечка", items.single().title)
        }

    @Test
    fun siteWorkOrdersUsesSiteAndPeriod() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    assertEquals("GET", method)
                    assertEquals(
                        "https://api.test/reports/site-work-orders?siteId=ceh-1&from=2026-07-01&to=2026-07-31",
                        url,
                    )
                    GatewayHttpResponse(
                        200,
                        """[{"id":"wo-1","orgId":"o","type":"emergency","status":"new","title":"Утечка","assetId":"pump-1","siteId":"ceh-1","dueAt":"2026-07-22","source":"api","createdAt":"2026-07-10T00:00:00Z","updatedAt":"t"}]""",
                    )
                }
            val items =
                WorkOrdersRepository(config = config, http = http, tokenStore = tokens)
                    .siteWorkOrders(siteId = "ceh-1", from = "2026-07-01", to = "2026-07-31")
            assertEquals("wo-1", items.single().id)
            assertEquals("Утечка", items.single().title)
        }

    @Test
    fun overdueOpenWorkOrdersHitsExactPath() =
        runBlocking {
            val tokens = InMemoryTokenStore()
            tokens.write(AuthTokens(accessToken = "at"))
            val http =
                RecordingGatewayHttpClient { method, url, _, _ ->
                    assertEquals("GET", method)
                    assertEquals("https://api.test/reports/overdue-open-work-orders", url)
                    GatewayHttpResponse(
                        200,
                        """[{"id":"wo-1","orgId":"o","type":"emergency","status":"new","title":"Утечка","assetId":"pump-1","siteId":"s","dueAt":"2026-07-01","source":"api","createdAt":"2026-07-10T00:00:00Z","updatedAt":"t"}]""",
                    )
                }
            val items =
                WorkOrdersRepository(config = config, http = http, tokenStore = tokens)
                    .overdueOpenWorkOrders()
            assertEquals("wo-1", items.single().id)
            assertEquals("Утечка", items.single().title)
        }
}
