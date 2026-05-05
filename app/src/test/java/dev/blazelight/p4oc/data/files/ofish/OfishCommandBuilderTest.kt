package dev.blazelight.p4oc.data.files.ofish

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfishCommandBuilderTest {
    private val capabilities = OfishCapabilities(
        hasBase64 = true,
        base64DecodeFlag = "-d",
        hashCommand = HashCommand.SHA256SUM,
        hasMv = true,
        hasMkdir = true,
        hasRm = true,
        hasAwk = true,
        hasMktemp = true,
    )

    private val builder = OfishCommandBuilder(delimiterId = { "test_id" })

    @Test
    fun `write quotes paths with POSIX single quote escaping`() {
        val command = builder.write("dir/weird'name.txt", "content", null, capabilities)

        assertTrue(command.contains("P='dir/weird'\\''name.txt'"))
    }

    @Test
    fun `write contains marker heredoc conflict guard and atomic write steps`() {
        val command = builder.write("dir/file.txt", "content", "expected", capabilities)

        assertTrue(command.contains("#OFISH_WRITE"))
        assertTrue(command.contains("<<'__OFISH_PAYLOAD_test_id__'"))
        assertTrue(command.contains("### 404 missing"))
        assertTrue(command.contains("### 409 conflict actual=%s"))
        assertTrue(command.contains("mkdir -p"))
        assertTrue(command.contains("mktemp"))
        assertTrue(command.contains("trap cleanup"))
        assertTrue(command.contains("mv -f"))
    }

    @Test
    fun `write uses base64 heredoc not raw payload`() {
        val raw = "hello secret content"
        val command = builder.write("file.txt", raw, null, capabilities)

        assertFalse(command.contains(raw))
        assertTrue(command.contains("aGVsbG8gc2VjcmV0IGNvbnRlbnQ="))
    }

    @Test
    fun `delete contains marker and directory precondition`() {
        val command = builder.delete("dir/file.txt")

        assertTrue(command.contains("#OFISH_DELETE"))
        assertTrue(command.contains("[ -d \"\$P\" ]"))
        assertTrue(command.contains("### 412 precondition reason=directory"))
        assertTrue(command.contains("### 404 missing"))
    }

    @Test
    fun `upload commands contain markers and heredoc chunks`() {
        val init = builder.uploadInit("dir/file.bin", null, capabilities)
        val chunk = builder.uploadChunk("dir/.ofish.upload.abc", byteArrayOf(1, 2, 3), capabilities)
        val finish = builder.uploadFinish("dir/file.bin", "dir/.ofish.upload.abc", null, capabilities)
        val abort = builder.uploadAbort("dir/.ofish.upload.abc")

        assertTrue(init.contains("#OFISH_UPLOAD_INIT"))
        assertTrue(chunk.contains("#OFISH_UPLOAD_CHUNK"))
        assertTrue(chunk.contains("<<'__OFISH_PAYLOAD_test_id__'"))
        assertTrue(finish.contains("#OFISH_UPLOAD_FINISH"))
        assertTrue(abort.contains("#OFISH_UPLOAD_ABORT"))
    }

    @Test
    fun `upload finish rechecks expected hash before move`() {
        val command = builder.uploadFinish("dir/file.bin", "dir/.ofish.upload.abc", "expected", capabilities)

        assertTrue(command.contains("EXPECTED='expected'"))
        assertTrue(command.contains("### 404 missing"))
        assertTrue(command.contains("### 409 conflict actual=%s"))
        assertTrue(command.indexOf("### 409 conflict") < command.indexOf("mv -f"))
    }
}
