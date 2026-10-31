@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.nuvio.tv.ui.screens.player

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks

private const val EXTERNAL_AUDIO_TAG = "ExternalAudio"

/**
 * Selects the first plugin-provided external audio that is flagged `autoSelect`, once its tracks
 * show up in the track list. Runs at most once per stream, so a later choice in the audio menu is
 * never overridden. Without an `autoSelect` entry the external tracks are only listed in the menu.
 * ExoPlayer only: external audio is merged by a MergingMediaSource, whose child 0 is the main stream
 * and whose children 1.. are the external audios in order.
 */
internal fun PlayerRuntimeController.applyExternalAudioDefault(tracks: Tracks) {
    if (externalAudioDefaultApplied) return
    val audios = streamExternalAudios
    val position = audios.indexOfFirst { it.autoSelect }
    if (position < 0) {
        externalAudioDefaultApplied = true
        return
    }
    val player = _exoPlayer ?: return
    val child = position + 1
    val group = tracks.groups.firstOrNull { g ->
        g.type == C.TRACK_TYPE_AUDIO && g.length > 0 &&
            g.mediaTrackGroup.id.substringBefore(':').toIntOrNull() == child
    } ?: return

    val wantLang = audios[position].lang.lowercase().take(2)
    val trackIndex = (0 until group.length).firstOrNull { i ->
        wantLang.isNotEmpty() && wantLang != "un" &&
            group.getTrackFormat(i).language?.lowercase()?.startsWith(wantLang) == true
    } ?: 0

    externalAudioDefaultApplied = true
    Log.i(
        EXTERNAL_AUDIO_TAG,
        "Auto-selecting external audio '${audios[position].label}' group=${group.mediaTrackGroup.id} idx=$trackIndex"
    )
    player.trackSelectionParameters = player.trackSelectionParameters
        .buildUpon()
        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        .build()
}
