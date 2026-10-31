package com.nuvio.tv.domain.model

import androidx.compose.runtime.Immutable

/**
 * A separate audio track (for example a dubbed track hosted apart from the video) that a plugin
 * returns next to a stream. The player merges it with the video and lists it in the audio menu.
 *
 * Delay: use the stream's `behaviorHints.audioDelayMs` together with `audioDelayLanguage`
 * (matched against [lang]); that path already exists and is not duplicated per track.
 */
@Immutable
data class ExternalAudio(
    val url: String,
    /** Language code of the track (BCP-47 or ISO 639), used for the menu entry and the delay scope. */
    val lang: String = "und",
    val label: String? = null,
    val headers: Map<String, String>? = null,
    /** Select this track automatically once it is available. Default: only listed in the menu. */
    val autoSelect: Boolean = false
)

/** More than this many tracks per stream are ignored. */
internal const val MAX_EXTERNAL_AUDIOS = 8

/**
 * Reads the `externalAudios` array of a plugin result (already decoded from JSON into lists and
 * maps). Entries without an http(s) `url` are dropped, as are duplicates of the same url.
 * Accepts `lang` or `language`, and `label` or `name`.
 */
internal fun parseExternalAudios(raw: Any?): List<ExternalAudio> {
    val list = raw as? List<*> ?: return emptyList()
    fun clean(value: Any?): String? =
        value?.toString()?.trim()?.takeIf { it.isNotEmpty() && !it.contains("[object") }

    val out = LinkedHashMap<String, ExternalAudio>()
    for (entry in list) {
        val obj = entry as? Map<*, *> ?: continue
        val url = clean(obj["url"])?.takeIf { it.startsWith("https://") || it.startsWith("http://") } ?: continue
        if (out.containsKey(url)) continue
        val headers = (obj["headers"] as? Map<*, *>)
            ?.mapNotNull { (k, v) ->
                val key = clean(k) ?: return@mapNotNull null
                val value = clean(v) ?: return@mapNotNull null
                key to value
            }
            ?.toMap()
            ?.ifEmpty { null }
        out[url] = ExternalAudio(
            url = url,
            lang = clean(obj["lang"]) ?: clean(obj["language"]) ?: "und",
            label = clean(obj["label"]) ?: clean(obj["name"]),
            headers = headers,
            autoSelect = obj["autoSelect"] == true
        )
        if (out.size >= MAX_EXTERNAL_AUDIOS) break
    }
    return out.values.toList()
}
