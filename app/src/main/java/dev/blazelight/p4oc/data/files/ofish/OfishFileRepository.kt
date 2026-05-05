package dev.blazelight.p4oc.data.files.ofish

import dev.blazelight.p4oc.data.files.FileCapabilities
import dev.blazelight.p4oc.data.files.FileList
import dev.blazelight.p4oc.data.files.FileOperationResult
import dev.blazelight.p4oc.data.files.FileRepository
import dev.blazelight.p4oc.data.files.FileUploadRequest
import dev.blazelight.p4oc.data.files.FileUploadResult
import dev.blazelight.p4oc.data.files.FileWriteRequest
import dev.blazelight.p4oc.data.files.FileWriteResult
import dev.blazelight.p4oc.domain.model.FileContent
import dev.blazelight.p4oc.domain.model.Symbol

internal class OfishFileRepository(
    private val delegate: FileRepository,
    private val mutationClient: OfishMutationClient,
) : FileRepository {
    override suspend fun listFiles(path: String): FileOperationResult<FileList> = delegate.listFiles(path)

    override suspend fun readFile(path: String): FileOperationResult<FileContent> = delegate.readFile(path)

    override suspend fun searchSymbols(query: String): FileOperationResult<List<Symbol>> = delegate.searchSymbols(query)

    override suspend fun writeFile(request: FileWriteRequest): FileOperationResult<FileWriteResult> =
        mutationClient.writeFile(request)

    override suspend fun deleteFile(path: String): FileOperationResult<Unit> = mutationClient.deleteFile(path)

    override suspend fun uploadFile(request: FileUploadRequest): FileOperationResult<FileUploadResult> =
        mutationClient.uploadFile(request)

    override suspend fun capabilities(): FileCapabilities {
        val delegateCapabilities = delegate.capabilities()
        val mutationsAvailable = mutationClient.mutationCapabilities() is OfishProbeResult.Available
        return delegateCapabilities.copy(
            canWrite = mutationsAvailable,
            canDelete = mutationsAvailable,
            canUpload = mutationsAvailable,
        )
    }
}
