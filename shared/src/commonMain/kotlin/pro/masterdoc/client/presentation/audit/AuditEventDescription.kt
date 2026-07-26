package pro.masterdoc.client.presentation.audit

/**
 * Human-readable Russian titles for black-box / gateway audit actions.
 */
object AuditEventDescription {
    fun title(
        action: String?,
        method: String,
        path: String,
        requestSummary: String? = null,
    ): String {
        val key = action?.trim()?.takeIf { it.isNotEmpty() }
        if (key != null) {
            namedTitle(key, requestSummary)?.let { return it }
            pathFallback(key)?.let { return it }
        }
        return "Запрос ${method.uppercase()} $path"
    }

    private fun namedTitle(
        action: String,
        requestSummary: String?,
    ): String? =
        when (action) {
            "admin.invite" -> "Пригласил пользователя"
            "admin.users.list" -> "Открыл список пользователей"
            "admin.roles.set" -> "Изменил роли пользователя"
            "admin.invite.resend" -> "Повторно отправил приглашение"
            "admin.user.delete" -> "Удалил пользователя"
            "audit.list" -> "Открыл журнал действий"
            "site.create" -> "Создал площадку"
            "site.update" -> "Изменил площадку"
            "site.delete" -> "Удалил площадку"
            "asset.create" -> "Добавил оборудование"
            "asset.move" -> "Переместил оборудование на другую площадку"
            "asset.confirm" -> "Подтвердил карточку оборудования"
            "asset.reject" -> "Отклонил черновик оборудования"
            "ui.Root.open" -> "Открыл приложение"
            "ui.Root.close" -> "Закрыл приложение"
            "ui.MainShell.open" -> "Открыл главный экран"
            "ui.MainShell.close" -> "Закрыл главный экран"
            "ui.shell.nav.select", "ui.shell.nav.navigate" -> {
                val dest = destinationLabel(parseProp(requestSummary, "destination"))
                if (dest != null) "Открыл раздел «$dest»" else "Перешёл в другой раздел"
            }
            "ui.shell.nav.deeplink" -> "Открыл приложение по ссылке"
            else ->
                when {
                    action.startsWith("ui.") && action.endsWith(".open") ->
                        "Открыл экран «${uiComponentLabel(action.removePrefix("ui.").removeSuffix(".open"))}»"
                    action.startsWith("ui.") && action.endsWith(".close") ->
                        "Закрыл экран «${uiComponentLabel(action.removePrefix("ui.").removeSuffix(".close"))}»"
                    else -> null
                }
        }

    private fun pathFallback(action: String): String? {
        if (!action.contains(':')) return null
        val (methodPart, pathPart) = action.split(':', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        return when {
            methodPart.equals("get", ignoreCase = true) && pathPart == "/sites" -> "Просмотрел площадки"
            methodPart.equals("get", ignoreCase = true) && pathPart == "/assets" -> "Просмотрел оборудование"
            methodPart.equals("get", ignoreCase = true) && pathPart == "/maintenance-maps" ->
                "Просмотрел карты ППР"
            methodPart.equals("get", ignoreCase = true) && pathPart.startsWith("/work-orders/board") ->
                "Открыл доску заказов"
            methodPart.equals("get", ignoreCase = true) && pathPart.startsWith("/work-orders") ->
                "Просмотрел заказы"
            methodPart.equals("get", ignoreCase = true) && pathPart.startsWith("/admin/users") ->
                "Открыл список пользователей"
            methodPart.equals("get", ignoreCase = true) && pathPart == "/admin/audit" ->
                "Открыл журнал действий"
            methodPart.equals("get", ignoreCase = true) -> "Просмотрел данные ($pathPart)"
            methodPart.equals("post", ignoreCase = true) -> "Отправил данные ($pathPart)"
            methodPart.equals("put", ignoreCase = true) || methodPart.equals("patch", ignoreCase = true) ->
                "Обновил данные ($pathPart)"
            methodPart.equals("delete", ignoreCase = true) -> "Удалил данные ($pathPart)"
            else -> null
        }
    }

    private fun destinationLabel(raw: String?): String? =
        when (raw?.trim()?.takeIf { it.isNotEmpty() }) {
            "Board", "board" -> "Доска"
            "Charts", "charts" -> "ППР"
            "Equipment", "equipment" -> "Оборудование"
            "Copilot", "copilot" -> "Наставник"
            "Users", "users", "user_invite" -> "Админ"
            "Profile", "profile" -> "Профиль"
            "Tickets", "tickets" -> "Заявки"
            "Map", "map" -> "Карта"
            else -> raw?.takeIf { it.isNotBlank() }
        }

    private fun uiComponentLabel(name: String): String =
        when (name) {
            "Root" -> "Приложение"
            "MainShell" -> "Главный экран"
            else -> name
        }

    private fun parseProp(
        summary: String?,
        key: String,
    ): String? {
        if (summary.isNullOrBlank()) return null
        val pattern = Regex(""""$key"\s*:\s*"([^"]*)"""")
        return pattern.find(summary)?.groupValues?.getOrNull(1)
    }
}
