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
    fun chooseListFromMethodGoesToForm() {
        assertEquals(TicketsCreateStep.Form, chooseList(TicketsCreateStep.Method))
    }

    @Test
    fun backFromFormGoesToMethod() {
        assertEquals(TicketsCreateStep.Method, backFromForm(TicketsCreateStep.Form))
    }

    @Test
    fun backFromMethodGoesToList() {
        assertEquals(TicketsCreateStep.List, backFromMethod(TicketsCreateStep.Method))
    }

    @Test
    fun afterSuccessfulCreateFromFormGoesToList() {
        assertEquals(TicketsCreateStep.List, afterSuccessfulCreate(TicketsCreateStep.Form))
    }
}
