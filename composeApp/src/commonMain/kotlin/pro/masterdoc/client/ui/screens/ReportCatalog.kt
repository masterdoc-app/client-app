package pro.masterdoc.client.ui.screens

enum class ReportId {
    KpiSummary,
    PlannedVsEmergency,
    PprCompliance,
    Backlog,
    DowntimeRanking,
    EquipmentDowntime,
    KpiTrends,
    ReactiveCompletion,
    EngineerWorkload,
    FailureFrequency,
    EquipmentWorkOrders,
    OverdueOpenWorkOrders,
    SiteWorkOrders,
    TimeToFirstAction,
    PprPlanFact,
}

data class ReportCatalogItem(
    val id: ReportId,
    val title: String,
    val subtitle: String,
    /** Краткая справка: что показывает отчёт (внизу экрана детали). */
    val description: String,
)

fun reportCatalogItems(): List<ReportCatalogItem> =
    listOf(
        ReportCatalogItem(
            id = ReportId.KpiSummary,
            title = "Сводка KPI",
            subtitle = "MTTR, MTBF и готовность",
            description =
                "Показывает среднюю длительность ремонта (MTTR), среднее время между отказами (MTBF) " +
                    "и процент готовности оборудования за выбранный период.",
        ),
        ReportCatalogItem(
            id = ReportId.PlannedVsEmergency,
            title = "Плановые vs аварийные",
            subtitle = "Объём и часы работ",
            description =
                "Сравнивает плановые и аварийные заявки: сколько их было и сколько часов работ " +
                    "пришлось на каждый тип за выбранный период.",
        ),
        ReportCatalogItem(
            id = ReportId.PprCompliance,
            title = "Выполнение ППР",
            subtitle = "Вовремя, с опозданием, открытые",
            description =
                "Показывает выполнение планово-предупредительных работ: сколько пунктов сделано " +
                    "вовремя, с опозданием, а также сколько открытых просроченных и ещё ожидающих срока.",
        ),
        ReportCatalogItem(
            id = ReportId.Backlog,
            title = "Очередь заявок",
            subtitle = "Возраст и просрочки",
            description =
                "Распределяет открытые заявки по возрасту (младше 7 дней, от 7 до 30, старше 30) " +
                    "и отдельно считает просроченные.",
        ),
        ReportCatalogItem(
            id = ReportId.DowntimeRanking,
            title = "Рейтинг простоев",
            subtitle = "Оборудование по часам простоя",
            description =
                "Ранжирует оборудование по суммарным часам простоя за период — сверху единицы " +
                    "с наибольшим простоем.",
        ),
        ReportCatalogItem(
            id = ReportId.EquipmentDowntime,
            title = "Простои оборудования",
            subtitle = "Шкала простоев по дням",
            description =
                "Шкала по дням: для каждого оборудования показаны интервалы простоя. " +
                    "Зелёный — закрытый ремонт, оранжевый — ещё в работе.",
        ),
        ReportCatalogItem(
            id = ReportId.KpiTrends,
            title = "Динамика KPI",
            subtitle = "MTTR, MTBF, готовность во времени",
            description =
                "Показывает изменения MTTR, MTBF и готовности оборудования по дням или неделям " +
                    "за выбранный период.",
        ),
        ReportCatalogItem(
            id = ReportId.ReactiveCompletion,
            title = "Реактивность и закрытие",
            subtitle = "% аварийных и доля закрытых",
            description =
                "Показывает, какую долю созданных заявок составляют аварийные, " +
                    "и сколько заявок удалось закрыть за выбранный период.",
        ),
        ReportCatalogItem(
            id = ReportId.EngineerWorkload,
            title = "Нагрузка инженеров",
            subtitle = "Заявки и часы по людям",
            description =
                "Сравнивает нагрузку инженеров: сколько заявок каждый закрыл и сколько часов " +
                    "заняла работа за выбранный период.",
        ),
        ReportCatalogItem(
            id = ReportId.FailureFrequency,
            title = "Частота отказов",
            subtitle = "Топ оборудования по авариям",
            description =
                "Ранжирует оборудование по числу аварийных заявок, созданных за выбранный период.",
        ),
        ReportCatalogItem(
            id = ReportId.EquipmentWorkOrders,
            title = "Детальный отчёт",
            subtitle = "Заявки по единице оборудования",
            description =
                "По выбранному оборудованию показывает все заявки, которые пересекают выбранный период: " +
                    "открытые и закрытые. Нажмите строку, чтобы открыть карточку.",
        ),
        ReportCatalogItem(
            id = ReportId.OverdueOpenWorkOrders,
            title = "Просроченные",
            subtitle = "Открытые с истёкшим сроком",
            description =
                "Показывает открытые заявки, у которых срок уже прошёл. " +
                    "Нажмите строку, чтобы открыть карточку.",
        ),
        ReportCatalogItem(
            id = ReportId.SiteWorkOrders,
            title = "По площадке",
            subtitle = "Заявки по цеху",
            description =
                "По выбранной площадке показывает заявки, пересекающие период. " +
                    "Нажмите строку, чтобы открыть карточку.",
        ),
        ReportCatalogItem(
            id = ReportId.TimeToFirstAction,
            title = "Время реакции",
            subtitle = "До перевода в работу",
            description =
                "Показывает заявки, созданные за период, и сколько времени прошло до перевода в работу. " +
                    "Нажмите строку, чтобы открыть карточку.",
        ),
        ReportCatalogItem(
            id = ReportId.PprPlanFact,
            title = "ППР: план и факт",
            subtitle = "Пункты ТО за период",
            description =
                "Показывает пункты планово-предупредительных работ с исходом: просрочен, ожидает, " +
                    "вовремя или с опозданием. Нажмите строку, чтобы открыть карточку.",
        ),
    )
