package dev.blazelight.p4oc.data.files.ofish

import dev.blazelight.p4oc.domain.model.Permission
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfishPermissionAutoApproverTest {
    @Test
    fun `handles only registered session with callID and approving predicate`() = runTest {
        val responder = FakeResponder()
        val approver = OfishPermissionAutoApprover(responder)
        approver.registerSession("ofish-1") { permission -> permission.patterns == listOf("exact") }

        val result = approver.handle(permission(sessionId = "ofish-1", callId = "call-1", patterns = listOf("exact")))

        assertTrue(result is PermissionAutoApprovalResult.Handled)
        assertEquals(listOf("perm-1"), responder.responded)
    }

    @Test
    fun `does not handle unmatched permissions`() = runTest {
        val responder = FakeResponder()
        val approver = OfishPermissionAutoApprover(responder)
        approver.registerSession("ofish-1") { permission -> permission.patterns == listOf("exact") }

        assertTrue(approver.handle(permission(sessionId = "other", callId = "call-1", patterns = listOf("exact"))) is PermissionAutoApprovalResult.NotHandled)
        assertTrue(approver.handle(permission(sessionId = "ofish-1", callId = null, patterns = listOf("exact"))) is PermissionAutoApprovalResult.NotHandled)
        assertTrue(approver.handle(permission(sessionId = "ofish-1", callId = "call-2", patterns = listOf("other"))) is PermissionAutoApprovalResult.NotHandled)
        assertEquals(emptyList<String>(), responder.responded)
    }

    @Test
    fun `responder false returns failed and rolls back callID for retry`() = runTest {
        val responder = FakeResponder(responses = ArrayDeque(listOf(false, true)))
        val approver = OfishPermissionAutoApprover(responder)
        approver.registerSession("ofish-1") { true }

        assertTrue(approver.handle(permission(id = "perm-1", callId = "call-1")) is PermissionAutoApprovalResult.Failed)
        assertTrue(approver.handle(permission(id = "perm-2", callId = "call-1")) is PermissionAutoApprovalResult.Handled)
        assertEquals(listOf("perm-1", "perm-2"), responder.responded)
    }

    @Test
    fun `does not reply twice for same callID`() = runTest {
        val responder = FakeResponder()
        val approver = OfishPermissionAutoApprover(responder)
        approver.registerSession("ofish-1") { true }

        assertTrue(approver.handle(permission(id = "perm-1", callId = "call-1")) is PermissionAutoApprovalResult.Handled)
        assertTrue(approver.handle(permission(id = "perm-2", callId = "call-1")) is PermissionAutoApprovalResult.AlreadyHandled)
        assertEquals(listOf("perm-1"), responder.responded)
    }

    private class FakeResponder(
        private val responses: ArrayDeque<Boolean> = ArrayDeque(),
    ) : OfishPermissionResponder {
        val responded = mutableListOf<String>()

        override suspend fun respondOnce(permissionId: String): Boolean {
            responded += permissionId
            return responses.removeFirstOrNull() ?: true
        }
    }

    private fun permission(
        id: String = "perm-1",
        sessionId: String = "ofish-1",
        callId: String? = "call-1",
        patterns: List<String> = listOf("exact"),
    ): Permission = Permission(
        id = id,
        type = "shell",
        patterns = patterns,
        sessionID = sessionId,
        messageID = "message-1",
        callID = callId,
        title = "permission",
        metadata = buildJsonObject { },
        always = emptyList(),
    )
}
