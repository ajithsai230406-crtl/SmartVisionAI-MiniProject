package com.smartvision.ai

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.domain.model.HistoryRecord
import com.smartvision.ai.presentation.history.HistoryViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule val rule = InstantTaskExecutorRule()
    private val dispatcher = UnconfinedTestDispatcher()

    private val historyRepository = mockk<HistoryRepository>(relaxed = true)

    private val fakeRecords = listOf(
        HistoryRecord(1, "OCR", "Title 1", "Details 1", 1000L),
        HistoryRecord(2, "Chat", "Title 2", "Details 2", 2000L),
        HistoryRecord(3, "OCR", "Title 3", "Details 3", 3000L)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { historyRepository.observeHistory() } returns flowOf(fakeRecords)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `history flow receives repository list`() = runTest {
        val vm = HistoryViewModel(historyRepository)
        // Wait for flow collection to emit non-empty list
        val result = vm.history.first { it.isNotEmpty() }
        Assert.assertEquals(3, result.size)
        Assert.assertEquals("Title 1", result[0].title)
    }

    @Test
    fun `clearHistory triggers repository clear`() = runTest {
        val vm = HistoryViewModel(historyRepository)
        vm.clearHistory()
        coVerify(exactly = 1) { historyRepository.clear() }
    }
}
