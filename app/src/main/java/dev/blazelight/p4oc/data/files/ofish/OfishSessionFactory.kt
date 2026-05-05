package dev.blazelight.p4oc.data.files.ofish

import dev.blazelight.p4oc.core.log.AppLog
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.security.SecureRandom

internal data class OfishSession(
    val id: String,
    val title: String,
)

internal data class OfishSweepReport(
    val scanned: Int,
    val staleFound: Int,
    val deleted: Int,
    val failed: Int,
)

internal class OfishSessionFactory(
    private val client: OfishWorkspaceClient,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val shortId: () -> String = { randomShortId() },
) {
    private val activeSessionIds = mutableSetOf<String>()

    suspend fun <T> withSession(
        operationName: String,
        block: suspend (OfishSession) -> T,
    ): T {
        val title = OfishSessionNames.build(operationName, nowMs(), shortId())
        val created = client.createSession(title)
        val session = OfishSession(id = created.id, title = created.title)
        synchronized(activeSessionIds) { activeSessionIds += session.id }
        try {
            return block(session)
        } finally {
            synchronized(activeSessionIds) { activeSessionIds -= session.id }
            withContext(NonCancellable) {
                runCatching { client.deleteSession(session.id) }
                    .onFailure { error -> AppLog.w(TAG, "Failed to delete OFISH session ${session.id}: ${error.message}") }
            }
        }
    }

    suspend fun sweepStaleSessions(maxAgeMillis: Long, limit: Int? = DEFAULT_SWEEP_LIMIT): OfishSweepReport {
        return runCatching {
            val now = nowMs()
            val sessions = client.listSessionsCurrentWorkspace(limit)
            var staleFound = 0
            var deleted = 0
            var failed = 0

            sessions
                .filter { OfishSessionNames.isOfishTitle(it.title) }
                .forEach { session ->
                    val isActive = synchronized(activeSessionIds) { session.id in activeSessionIds }
                    if (isActive) return@forEach

                    val timestamp = OfishSessionNames.parseTimestamp(session.title) ?: (session.time.updated ?: session.time.created)
                    if (now - timestamp >= maxAgeMillis) {
                        staleFound += 1
                        runCatching { client.deleteSession(session.id) }
                            .onSuccess { deleted += 1 }
                            .onFailure { error ->
                                failed += 1
                                AppLog.w(TAG, "Failed to sweep stale OFISH session ${session.id}: ${error.message}")
                            }
                    }
                }

            OfishSweepReport(
                scanned = sessions.size,
                staleFound = staleFound,
                deleted = deleted,
                failed = failed,
            )
        }.getOrElse { error ->
            AppLog.w(TAG, "Failed to list OFISH sessions for sweep: ${error.message}")
            OfishSweepReport(scanned = 0, staleFound = 0, deleted = 0, failed = 1)
        }
    }

    private companion object {
        const val TAG = "OfishSessionFactory"
        const val DEFAULT_SWEEP_LIMIT = 100

        private val RANDOM = SecureRandom()

        fun randomShortId(): String {
            val bytes = ByteArray(4)
            RANDOM.nextBytes(bytes)
            return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
        }
    }
}
