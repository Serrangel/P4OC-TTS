package dev.blazelight.p4oc.data.files.ofish

internal object OfishMutationParser {
    private val STATUS_LINE = Regex("^###\\s+(\\d{3})\\s+(\\S+)(?:\\s+(.*))?$")
    private val KEY_VALUE = Regex("(\\w+)=([^\\s]+)")

    fun parse(output: String): OfishMutationStatus {
        val line = output.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("### ") }
            .lastOrNull()
            ?: return OfishMutationStatus.Malformed("Malformed OFISH mutation output: missing status line")

        val match = STATUS_LINE.matchEntire(line)
            ?: return OfishMutationStatus.Malformed("Malformed OFISH mutation output: invalid status line")
        val code = match.groupValues[1].toInt()
        val status = match.groupValues[2]
        val remainder = match.groupValues.getOrNull(3).orEmpty().trim()
        val values = KEY_VALUE.findAll(remainder).associate { it.groupValues[1] to it.groupValues[2].trim() }

        return when (code) {
            200, 201 -> OfishMutationStatus.Ok(
                code = code,
                status = status,
                hash = values["hash"],
                uploadToken = values["upload"],
                values = values,
            )
            204 -> OfishMutationStatus.Deleted
            404 -> OfishMutationStatus.Missing
            409 -> OfishMutationStatus.Conflict(actualHash = values["actual"])
            412 -> OfishMutationStatus.PreconditionFailed(reason = values["reason"])
            500 -> OfishMutationStatus.Failed(message = "OFISH mutation failed", reason = values["reason"])
            501 -> OfishMutationStatus.CapabilitiesMissing(
                missing = remainder
                    .removePrefix(status)
                    .trim()
                    .split(Regex("\\s+"))
                    .filter { it.isNotBlank() },
            )
            else -> OfishMutationStatus.Malformed("Unsupported OFISH mutation status code: $code")
        }
    }
}

internal sealed interface OfishMutationStatus {
    data class Ok(
        val code: Int,
        val status: String,
        val hash: String? = null,
        val uploadToken: String? = null,
        val values: Map<String, String> = emptyMap(),
    ) : OfishMutationStatus

    data object Deleted : OfishMutationStatus

    data object Missing : OfishMutationStatus

    data class Conflict(val actualHash: String?) : OfishMutationStatus

    data class PreconditionFailed(val reason: String?) : OfishMutationStatus

    data class CapabilitiesMissing(val missing: List<String>) : OfishMutationStatus

    data class Failed(
        val message: String,
        val reason: String? = null,
    ) : OfishMutationStatus

    data class Malformed(val message: String) : OfishMutationStatus
}
