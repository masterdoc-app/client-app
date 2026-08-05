package pro.masterdoc.client.ui.screens

enum class TicketsCreateStep {
    List,
    Method,
    EquipmentList,
    Form,
}

fun defaultTicketsCreateStep(): TicketsCreateStep = TicketsCreateStep.List

fun openCreate(step: TicketsCreateStep): TicketsCreateStep =
    when (step) {
        TicketsCreateStep.List -> TicketsCreateStep.Method
        else -> step
    }

fun chooseList(step: TicketsCreateStep): TicketsCreateStep =
    when (step) {
        TicketsCreateStep.Method -> TicketsCreateStep.EquipmentList
        else -> step
    }

fun selectEquipment(step: TicketsCreateStep): TicketsCreateStep =
    when (step) {
        TicketsCreateStep.EquipmentList -> TicketsCreateStep.Form
        else -> step
    }

fun backFromForm(step: TicketsCreateStep): TicketsCreateStep =
    when (step) {
        TicketsCreateStep.Form -> TicketsCreateStep.EquipmentList
        else -> step
    }

fun backFromEquipmentList(step: TicketsCreateStep): TicketsCreateStep =
    when (step) {
        TicketsCreateStep.EquipmentList -> TicketsCreateStep.Method
        else -> step
    }

fun backFromMethod(step: TicketsCreateStep): TicketsCreateStep =
    when (step) {
        TicketsCreateStep.Method -> TicketsCreateStep.List
        else -> step
    }

fun afterSuccessfulCreate(step: TicketsCreateStep): TicketsCreateStep =
    when (step) {
        TicketsCreateStep.Form -> TicketsCreateStep.List
        else -> step
    }

fun togglePhotoSourceActions(expanded: Boolean): Boolean = !expanded
