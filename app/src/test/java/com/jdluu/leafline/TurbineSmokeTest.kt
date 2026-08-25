package com.jdluu.leafline

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TurbineSmokeTest {

    @Test
    fun stateFlowEmitsInitialValueAndUpdate() = runTest {
        val flow = MutableStateFlow("initial")
        flow.test {
            assertEquals("initial", awaitItem())
            flow.value = "updated"
            assertEquals("updated", awaitItem())
        }
    }
}
