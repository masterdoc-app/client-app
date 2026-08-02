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
)

fun reportCatalogItems(): List<ReportCatalogItem> =
    listOf(
        ReportCatalogItem(ReportId.KpiSummary, "Сводка KPI", "MTTR, MTBF и готовность"),
        ReportCatalogItem(ReportId.PlannedVsEmergency, "Плановые vs аварийные", "Объём и часы работ"),
        ReportCatalogItem(ReportId.PprCompliance, "Выполнение ППР", "Вовремя, с опозданием, открытые"),
        ReportCatalogItem(ReportId.Backlog, "Очередь заявок", "Возраст и просрочки"),
        ReportCatalogItem(ReportId.DowntimeRanking, "Рейтинг простоев", "Оборудование по часам простоя"),
        ReportCatalogItem(ReportId.EquipmentDowntime, "Простои оборудования", "Шкала простоев по дням"),
    )
