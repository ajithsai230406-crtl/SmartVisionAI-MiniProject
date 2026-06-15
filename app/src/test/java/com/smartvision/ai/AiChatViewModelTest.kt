package com.smartvision.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.smartvision.ai.data.repository.AiAssistantRepository
import com.smartvision.ai.data.repository.FirebaseRepository
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.domain.model.ChatHistoryItem
import com.smartvision.ai.domain.model.ChatMessage
import com.smartvision.ai.presentation.chat.AiChatViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class AiChatViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()
    private val dispatcher = UnconfinedTestDispatcher()

    private val aiAssistantRepository = mockk<AiAssistantRepository>(relaxed = true)
    private val historyRepository = mockk<HistoryRepository>(relaxed = true)
    private val firebaseRepository = mockk<FirebaseRepository>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    private val fakeChatHistory = listOf(
        ChatHistoryItem("1", "uid_123", "Hello AI", "Hello User", 1000L),
        ChatHistoryItem("2", "uid_123", "How is the weather", "It is sunny", 2000L)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkConstructor(TextToSpeech::class)
        every { anyConstructed<TextToSpeech>().stop() } returns 0
        every { anyConstructed<TextToSpeech>().shutdown() } just Runs
        every { anyConstructed<TextToSpeech>().speak(any(), any(), any(), any()) } returns 0

        every { firebaseRepository.getCurrentUid() } returns "uid_123"
        coEvery { firebaseRepository.getChatHistoryPaginated(any(), any(), any()) } returns Result.success(fakeChatHistory)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `loadChatHistory restores chats from firebase and maps QA pairs to separate messages`() = runTest {
        val vm = AiChatViewModel(aiAssistantRepository, historyRepository, firebaseRepository, context)

        // Wait for coroutine inside init to finish loading
        val messages = vm.messages.value
        // fakeChatHistory contains 2 QA pairs.
        // Since getChatHistoryPaginated sorts DESCENDING, the items are:
        // Item 1 (1000L) -> userMessage="Hello AI", aiResponse="Hello User"
        // Item 2 (2000L) -> userMessage="How is the weather", aiResponse="It is sunny"
        // Reversed: Item 2 first, then Item 1:
        // Item 2 (2000L) reversed order puts 2000L first or 1000L?
        // Wait, items.reversed() on [Item 1, Item 2] becomes [Item 2, Item 1].
        // So loadedMessages is Item 2 user, Item 2 bot, Item 1 user, Item 1 bot.
        // Wait, let's verify. Yes.
        Assert.assertEquals(4, messages.size)
        Assert.assertEquals("How is the weather", messages[0].text)
        Assert.assertTrue(messages[0].fromUser)
        Assert.assertEquals("It is sunny", messages[1].text)
        Assert.assertFalse(messages[1].fromUser)
    }

    @Test
    fun `loadNextPage prepends older chats to the message list`() = runTest {
        val firstPage = List(15) { i ->
            ChatHistoryItem(
                id = "first_$i",
                uid = "uid_123",
                userMessage = "User message $i",
                aiResponse = "AI response $i",
                timestamp = 10000L - i
            )
        }

        coEvery { firebaseRepository.getChatHistoryPaginated(any(), any(), any()) } coAnswers {
            val lastTimestamp = args[2] as Long?
            if (lastTimestamp == null) {
                Result.success(firstPage)
            } else {
                Result.success(fakeChatHistory)
            }
        }

        val vm = AiChatViewModel(aiAssistantRepository, historyRepository, firebaseRepository, context)
        
        // Assert initial load shows 30 messages (15 QA pairs)
        Assert.assertEquals(30, vm.messages.value.size)

        vm.loadNextPage()

        val messages = vm.messages.value
        // 30 initial messages + 4 prepended messages = 34 messages
        Assert.assertEquals(34, messages.size)
        Assert.assertEquals("How is the weather", messages[0].text)
        Assert.assertEquals("It is sunny", messages[1].text)
    }

    @Test
    fun `send query triggers repo save and saves QA pair to firestore after streaming finishes`() = runTest {
        coEvery { firebaseRepository.getChatHistoryPaginated(any(), any(), any()) } returns Result.success(emptyList())
        coEvery { aiAssistantRepository.replyToWithHistoryStream(any(), any(), any()) } returns flowOf("Hi ", "there!")

        val vm = AiChatViewModel(aiAssistantRepository, historyRepository, firebaseRepository, context)
        vm.send("Hello AI")

        // Wait/verify that the combined QA pair is saved to Firestore
        coVerify(exactly = 1) {
            firebaseRepository.saveChatHistory("uid_123", "Hello AI", "Hi there!", any())
        }
    }

    @Test
    fun `clearChat triggers cloud deletion and resets pagination states`() = runTest {
        val vm = AiChatViewModel(aiAssistantRepository, historyRepository, firebaseRepository, context)
        vm.clearChat()

        coVerify(exactly = 1) {
            firebaseRepository.clearAllChatHistory("uid_123")
        }
        Assert.assertTrue(vm.isLastPage.value)
    }
}
