package dev.blazelight.p4oc.data.files.ofish

import dev.blazelight.p4oc.data.remote.dto.MessageWrapperDto
import dev.blazelight.p4oc.data.remote.dto.ShellCommandRequest

/**
 * Executes the OFISH shell-environment capability probe in an ephemeral session.
 *
 * If this probe is wired into production and its shell execution can trigger server permission
 * requests, the integration must register the ephemeral session with the conservative OFISH
 * permission auto-approver / SSE broker before executing the command. This class currently has no
 * production callsite and intentionally does not auto-approve permissions by itself.
 */
internal class OfishCapabilityProbe(
    private val client: OfishWorkspaceClient,
    private val sessionFactory: OfishSessionFactory,
    private val shellAgent: String = DEFAULT_SHELL_AGENT,
) {
    suspend fun probe(): OfishProbeResult = runCatching {
        sessionFactory.withSession(OPERATION_NAME) { session ->
            val response = client.executeShellCommand(
                sessionId = session.id,
                request = ShellCommandRequest(
                    agent = shellAgent,
                    model = null,
                    command = OfishCapabilityProbeCommand.build(),
                ),
            )
            OfishCapabilityParser.parse(OfishProbeOutputExtractor.extract(response))
        }
    }.getOrElse { error ->
        OfishProbeResult.Failed("OFISH capability probe failed", error)
    }

    private companion object {
        const val OPERATION_NAME = "probe"
        const val DEFAULT_SHELL_AGENT = "build"
    }
}

internal object OfishProbeOutputExtractor {
    fun extract(message: MessageWrapperDto): String = buildString {
        message.parts.forEach { part ->
            part.text?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            part.state?.output?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            part.state?.raw?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            part.state?.error?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
        }
    }
}
