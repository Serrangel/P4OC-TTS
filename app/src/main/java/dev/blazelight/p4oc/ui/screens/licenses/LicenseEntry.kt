package dev.blazelight.p4oc.ui.screens.licenses

import androidx.annotation.RawRes
import dev.blazelight.p4oc.R

/**
 * SPDX-style license identifier with metadata for display and full-text loading.
 */
enum class License(
    val spdxId: String,
    val displayName: String,
    @param:RawRes val rawTextRes: Int,
    val isCopyleft: Boolean,
    /**
     * Whether shipping this component triggers a copyleft source-availability
     * notice in the licenses screen. True for LGPL (relinking + source) and
     * GPL (corresponding-source-on-request) entries shipped under the Play
     * Store distribution path.
     */
    val requiresSourceNotice: Boolean,
) {
    APACHE_2_0(
        spdxId = "Apache-2.0",
        displayName = "Apache License 2.0",
        rawTextRes = R.raw.license_apache_2_0,
        isCopyleft = false,
        requiresSourceNotice = false,
    ),
    MIT(
        spdxId = "MIT",
        displayName = "MIT License",
        rawTextRes = R.raw.license_mit,
        isCopyleft = false,
        requiresSourceNotice = false,
    ),
    LGPL_2_1(
        spdxId = "LGPL-2.1-or-later",
        displayName = "GNU Lesser General Public License, version 2.1",
        rawTextRes = R.raw.license_lgpl_2_1,
        isCopyleft = true,
        requiresSourceNotice = true,
    ),
    GPL_3_0(
        spdxId = "GPL-3.0-or-later",
        displayName = "GNU General Public License, version 3",
        rawTextRes = R.raw.license_gpl_3_0,
        isCopyleft = true,
        requiresSourceNotice = true,
    ),
}

/**
 * One third-party component shipped in the app.
 *
 * Keep this list in sync with `gradle/libs.versions.toml`,
 * `app/build.gradle.kts`, and (for vendored grammars and other non-Maven
 * artifacts) `app/src/main/assets/textmate/SOURCES.md`.
 *
 * [version] is the displayed version string for the entry. For Maven
 * dependencies use the artifact version (e.g. `"5.3.2"`). For vendored
 * upstream snapshots that do not carry a release tag, use the
 * `"commit:<short-sha>"` form (mirroring `SOURCES.md`). `null` is reserved
 * for entries that are documented but not yet shipped — there should be
 * no shipped entries with a null version.
 */
data class LicenseEntry(
    val name: String,
    val version: String?,
    val license: License,
    val upstreamUrl: String,
    val notes: String? = null,
)
