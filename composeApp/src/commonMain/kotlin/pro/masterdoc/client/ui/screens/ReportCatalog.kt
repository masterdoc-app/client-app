package pro.masterdoc.client.ui.screens

enum class ReportId {
    KpiSummary,
    PlannedVsEmergency,
    PprCompliance,
    Backlog,
    DowntimeRanking,
    EquipmentDowntime,
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
    )
