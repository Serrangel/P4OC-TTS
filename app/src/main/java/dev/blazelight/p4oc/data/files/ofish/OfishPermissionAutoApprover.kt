package dev.blazelight.p4oc.data.files.ofish

import dev.blazelight.p4oc.data.remote.dto.PermissionResponseRequest
import dev.blazelight.p4oc.domain.model.Permission
import dev.blazelight.p4oc.domain.model.PermissionResponse

internal class OfishPermissionAutoApprover(
    private val responder: OfishPermissionResponder,
) {
    private val activeSessions = mutableMapOf<String, RegisteredSession>()
    private val handledCallIds = mutableSetOf<String>()

    fun registerSession(
        sessionId: String,
        predicate: (Permission) -> Boolean,
    ): Registration {
        synchronized(this) {
            activeSessions[sessionId] = RegisteredSession(predicate)
        }
        return Registration { unregisterSession(sessionId) }
    }

    suspend fun handle(permission: Permission): PermissionAutoApprovalResult {
        val callId = permission.callID ?: return PermissionAutoApprovalResult.NotHandled
        val registered = synchronized(this) { activeSessions[permission.sessionID] }
            ?: return PermissionAutoApprovalResult.NotHandled
        if (!registered.predicate(permission)) return PermissionAutoApprovalResult.NotHandled

        val shouldReply = synchronized(this) { handledCallIds.add(callId) }
        if (!shouldReply) return PermissionAutoApprovalResult.AlreadyHandled

        return if (responder.respondOnce(permission.id)) {
            PermissionAutoApprovalResult.Handled
        } else {
            synchronized(this) { handledCallIds.remove(callId) }
            PermissionAutoApprovalResult.Failed
        }
    }

    private fun unregisterSession(sessionId: String) {
        synchronized(this) { activeSessions.remove(sessionId) }
    }

    private data class RegisteredSession(
        val predicate: (Permission) -> Boolean,
    )

    fun interface Registration : AutoCloseable {
        override fun close()
    }
}

internal sealed interface PermissionAutoApprovalResult {
    data object Handled : PermissionAutoApprovalResult
    data object AlreadyHandled : PermissionAutoApprovalResult
    data object NotHandled : PermissionAutoApprovalResult
    data object Failed : PermissionAutoApprovalResult
}

internal interface OfishPermissionResponder {
    suspend fun respondOnce(permissionId: String): Boolean
}

internal class OfishWorkspacePermissionResponder(
    private val client: OfishWorkspaceClient,
) : OfishPermissionResponder {
    override suspend fun respondOnce(permissionId: String): Boolean =
        client.respondToPermission(permissionId, PermissionResponseRequest(reply = PermissionResponse.ONCE.value))
}
