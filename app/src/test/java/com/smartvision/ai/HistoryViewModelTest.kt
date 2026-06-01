package com.smartvision.ai

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.smartvision.ai.domain.models.ScanHistoryItem
import com.smartvision.ai.domain.usecase.ClearHistoryUseCase
import com.smartvision.ai.domain.usecase.GetHistoryUseCase
import com.smartvision.ai.ui.screens.HistoryViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class HistoryViewModelTest {

    @get:Rule val rule = InstantTaskExecutorRule()
    private val dispatcher = UnconfinedTestDispatcher()

    @Mock lateinit var getHistory: GetHistoryUseCase
    @Mock lateinit var clearHistory: ClearHistoryUseCase

    private val fakeItems = listOf(
        ScanHistoryItem("1", moduleType="OCR",     summary="Hello world",       timestampMillis=1_000L),
        ScanHistoryItem("2", moduleType="Translate",summary="Namaste",          timestampMillis=2_000L),
        ScanHistoryItem("3", moduleType="OCR",     summary="Second OCR result", timestampMillis=3_000L)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        `when`(getHistory.invoke()).thenReturn(flowOf(fakeItems))
        `when`(getHistory.byType("OCR")).thenReturn(flowOf(fakeItems.filter { it.moduleType == "OCR" }))
        `when`(getHistory.search("Hello")).thenReturn(flowOf(fakeItems.filter { it.summary.contains("Hello") }))
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `default filter is All and returns all items`() = runTest {
        val vm = HistoryViewModel(getHistory, clearHistory)
        vm.historyItems.test {
            val items = awaitItem()
            Assert.assertEquals(3, items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filter by OCR returns only OCR items`() = runTest {
        val vm = HistoryViewModel(getHistory, clearHistory)
        vm.setFilter("OCR")
        vm.historyItems.test {
            val items = awaitItem()
            Assert.assertTrue(items.all { it.moduleType == "OCR" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search narrows results`() = runTest {
        val vm = HistoryViewModel(getHistory, clearHistory)
        vm.setSearch("Hello")
        vm.historyItems.test {
            val items = awaitItem()
            Assert.assertTrue(items.all { it.summary.contains("Hello") })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
