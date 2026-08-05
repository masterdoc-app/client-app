package pro.masterdoc.client.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class TicketsCreateFlowTest {
    @Test
    fun defaultStepIsList() {
        assertEquals(TicketsCreateStep.List, defaultTicketsCreateStep())
    }

    @Test
    fun openCreateFromListGoesToMethod() {
        assertEquals(TicketsCreateStep.Method, openCreate(TicketsCreateStep.List))
    }

    @Test
    fun chooseListFromMethodGoesToEquipmentList() {
        assertEquals(TicketsCreateStep.EquipmentList, chooseList(TicketsCreateStep.Method))
    }

    @Test
    fun selectEquipmentFromEquipmentListGoesToForm() {
        assertEquals(TicketsCreateStep.Form, selectEquipment(TicketsCreateStep.EquipmentList))
    }

    @Test
    fun backFromFormGoesToEquipmentList() {
        assertEquals(TicketsCreateStep.EquipmentList, backFromForm(TicketsCreateStep.Form))
    }

    @Test
    fun backFromEquipmentListGoesToMethod() {
        assertEquals(TicketsCreateStep.Method, backFromEquipmentList(TicketsCreateStep.EquipmentList))
    }

    @Test
    fun backFromMethodGoesToList() {
        assertEquals(TicketsCreateStep.List, backFromMethod(TicketsCreateStep.Method))
    }

    @Test
    fun afterSuccessfulCreateFromFormGoesToList() {
        assertEquals(TicketsCreateStep.List, afterSuccessfulCreate(TicketsCreateStep.Form))
    }

    @Test
    fun photoSourceActionsToggleOpenAndClosed() {
        assertEquals(true, togglePhotoSourceActions(false))
        assertEquals(false, togglePhotoSourceActions(true))
    }
}
