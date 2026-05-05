package dev.blazelight.p4oc.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import dev.blazelight.p4oc.core.log.AppLog
import dev.blazelight.p4oc.domain.model.Permission
import dev.blazelight.p4oc.domain.model.Question
import dev.blazelight.p4oc.domain.model.QuestionOption
import dev.blazelight.p4oc.domain.model.QuestionRequest
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DialogQueueManagerTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        mockkObject(AppLog)
        every { AppLog.d(any(), any<String>()) } returns Unit
        every { AppLog.d(any(), any<() -> String>()) } returns Unit
        every { AppLog.e(any(), any<String>()) } returns Unit
        every { AppLog.e(any(), any<String>(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(AppLog)
    }

    @Test
    fun enqueuePermission_addsPermissionByCallId() {
        val handle = SavedStateHandle()
        val manager = DialogQueueManager(handle, json)
        val permission = permission(id = "p1", callId = "call-1")

        manager.enqueuePermission(permission)

        assertEquals(permission, manager.pendingPermissionsByCallId.value["call-1"])
    }

    @Test
    fun enqueuePermission_withNullCallIdDoesNotAddAnything() {
        val handle = SavedStateHandle()
        val manager = DialogQueueManager(handle, json)
        val permission = permission(id = "p1", callId = null)

        manager.enqueuePermission(permission)

        assertTrue(manager.pendingPermissionsByCallId.value.isEmpty())
    }

    @Test
    fun clearPermission_removesMatchingPermissionById() {
        val handle = SavedStateHandle()
        val manager = DialogQueueManager(handle, json)
        val first = permission(id = "p1", callId = "call-1")
        val second = permission(id = "p2", callId = "call-2")

        manager.enqueuePermission(first)
        manager.enqueuePermission(second)
        manager.clearPermission(first.id)

        assertNull(manager.pendingPermissionsByCallId.value["call-1"])
        assertEquals(second, manager.pendingPermissionsByCallId.value["call-2"])
    }

    @Test
    fun clearPermissionByRequestId_removesMatchingPermissionById() {
        val handle = SavedStateHandle()
        val manager = DialogQueueManager(handle, json)
        val first = permission(id = "request-1", callId = "call-1")
        val second = permission(id = "request-2", callId = "call-2")

        manager.enqueuePermission(first)
        manager.enqueuePermission(second)
        manager.clearPermissionByRequestId("request-1")

        assertNull(manager.pendingPermissionsByCallId.value["call-1"])
        assertEquals(second, manager.pendingPermissionsByCallId.value["call-2"])
    }

    @Test
    fun corruptSavedStateHandleData_doesNotCrash() {
        val handle = SavedStateHandle(
            mapOf(
                KEY_PENDING_QUESTION to "{bad-json}",
                KEY_PENDING_QUESTIONS_QUEUE to "[bad-json"
            )
        )

        val manager = DialogQueueManager(handle, json)

        assertNull(manager.pendingQuestion.value)
        assertNull(handle.get<String>(KEY_PENDING_QUESTION))
        assertNull(handle.get<String>(KEY_PENDING_QUESTIONS_QUEUE))
    }

    private fun permission(id: String, callId: String?): Permission {
        return Permission(
            id = id,
            type = "read",
            patterns = listOf("*.kt"),
            sessionID = "session-1",
            messageID = "message-1",
            callID = callId,
            title = "Allow read",
            metadata = buildJsonObject { },
            always = emptyList()
        )
    }

    @Test
    fun enqueueQuestion_showsImmediately_whenNoCurrentQuestion() {
        val handle = SavedStateHandle()
        val manager = DialogQueueManager(handle, json)
        val question = questionRequest(id = "q1")

        manager.enqueueQuestion(question)

        assertEquals(question, manager.pendingQuestion.value)
        assertEquals(json.encodeToString(question), handle.get<String>(KEY_PENDING_QUESTION))
    }

    @Test
    fun clearQuestion_advancesToNextInQueue() {
        val handle = SavedStateHandle()
        val manager = DialogQueueManager(handle, json)
        val first = questionRequest(id = "q1")
        val second = questionRequest(id = "q2")

        manager.enqueueQuestion(first)
        manager.enqueueQuestion(second)
        assertEquals(first, manager.pendingQuestion.value)

        manager.clearQuestion()
        assertEquals(second, manager.pendingQuestion.value)
    }

    @Test
    fun clearQuestion_clears_whenQueueEmpty() {
        val handle = SavedStateHandle()
        val manager = DialogQueueManager(handle, json)
        val only = questionRequest(id = "q1")

        manager.enqueueQuestion(only)
        manager.clearQuestion()

        assertNull(manager.pendingQuestion.value)
        assertNull(handle.get<String>(KEY_PENDING_QUESTION))
    }

    private fun questionRequest(id: String): QuestionRequest {
        return QuestionRequest(
            id = id,
            sessionID = "session-1",
            questions = listOf(
                Question(
                    header = "Header",
                    question = "Q?",
                    options = listOf(QuestionOption(label = "Yes", description = ""))
                )
            )
        )
    }

    private companion object {
        const val KEY_PENDING_QUESTION = "pending_question"
        const val KEY_PENDING_QUESTIONS_QUEUE = "pending_questions_queue"
    }
}
