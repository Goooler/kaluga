/*
 Copyright 2023 Splendo Consulting B.V. The Netherlands

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package com.splendo.kaluga.media

import com.splendo.kaluga.base.flow.SpecialFlowValue
import com.splendo.kaluga.base.state.HandleAfterOldStateIsRemoved
import com.splendo.kaluga.base.state.HandleBeforeOldStateIsRemoved
import com.splendo.kaluga.base.state.KalugaState
import kotlin.time.Duration

sealed class PlaybackError : Exception() {
    object Unsupported : PlaybackError()
    object IO : PlaybackError()
    object TimedOut : PlaybackError()
    object MalformedMediaSource : PlaybackError()
    object PlaybackHasEnded : PlaybackError()
    object Unknown : PlaybackError()
}

sealed interface PlaybackState : KalugaState {

    data class PlaybackParameters(
        val rate: Float = 1.0f,
        val loopMode: LoopMode = LoopMode.NotLooping
    )

    sealed class LoopMode {
        object NotLooping : LoopMode()
        object LoopingForever : LoopMode()
        data class LoopingForFixedNumber(val loops: UInt) : LoopMode()
    }

    sealed interface Active : PlaybackState {
        val reset: suspend  () -> Uninitialized
        val end: suspend () -> Ended
        fun failWithError(error: PlaybackError): suspend () -> Error
    }

    interface Uninitialized : Active {
        fun initialize(url: String): suspend () -> InitializedOrError
    }

    sealed interface InitializedOrError : Active

    interface Initialized : InitializedOrError {
        fun prepared(media: PlayableMedia): suspend  () -> Idle
    }

    sealed interface Prepared : Active {

        val playableMedia: PlayableMedia

        val stop: suspend () -> Stopped
        suspend fun seekTo(duration: Duration): Boolean
    }

    interface Idle : Prepared {
        fun start(playbackParameters: PlaybackParameters = PlaybackParameters()): suspend () -> Started
    }

    sealed interface Playing : Prepared {
        val playbackParameters: PlaybackParameters
        fun updatePlaybackParameters(new: PlaybackParameters): suspend () -> Playing
    }

    interface Started : Playing {
        val pause: suspend () -> Paused
        val completed: suspend () -> Completed
    }

    interface Paused : Playing {
        val start: suspend () -> Started
    }

    interface Stopped : Active {
        val reinitialize: suspend () -> Initialized
    }

    interface Completed : Prepared {
        val willLoop: Boolean
        fun start(playbackParameters: PlaybackParameters = PlaybackParameters()): suspend () -> Started
        val restartIfLooping: suspend  () -> Prepared
    }

    interface Error : InitializedOrError {
        val error: PlaybackError
    }

    interface Ended : PlaybackState, SpecialFlowValue.Last
}

internal sealed class PlaybackStateImpl {

    sealed class Active : PlaybackStateImpl() {
        internal abstract val mediaManager: MediaManager

        val reset: suspend  () -> PlaybackState.Uninitialized = { Uninitialized(mediaManager) }
        val end: suspend () -> PlaybackState.Ended = { Ended }
        fun failWithError(error: PlaybackError) = suspend { Error(error, mediaManager) }
    }

    data class Uninitialized(override val mediaManager: MediaManager) : Active(), PlaybackState.Uninitialized, HandleBeforeOldStateIsRemoved<PlaybackState> {
        override fun initialize(url: String): suspend () -> PlaybackState.InitializedOrError = mediaManager.createPlayableMedia(url)?.let {
            { Initialized(it, mediaManager) }
            } ?: failWithError(PlaybackError.MalformedMediaSource)

        override suspend fun beforeOldStateIsRemoved(oldState: PlaybackState) {
            when (oldState) {
                !is Uninitialized -> mediaManager.reset()
                else -> {}
            }
        }
    }

    data class Initialized(private val playableMedia: PlayableMedia, override val mediaManager: MediaManager) : Active(), PlaybackState.Initialized, HandleAfterOldStateIsRemoved<PlaybackState> {
        override fun prepared(media: PlayableMedia): suspend () -> PlaybackState.Idle = { Idle(media, mediaManager) }

        override suspend fun afterOldStateIsRemoved(oldState: PlaybackState) {
            when (oldState) {
                is Uninitialized, is Stopped -> mediaManager.initialize(playableMedia)
                else -> {}
            }
        }
    }

    sealed class Prepared : Active() {

        abstract override val mediaManager: MediaManager
        abstract val playableMedia: PlayableMedia

        val stop: suspend () -> PlaybackState.Stopped = { Stopped(playableMedia, mediaManager) }
        suspend fun seekTo(duration: Duration) = mediaManager.seekTo(duration)
    }

    data class Idle(override val playableMedia: PlayableMedia, override val mediaManager: MediaManager) : Prepared(), PlaybackState.Idle {
        override fun start(playbackParameters: PlaybackState.PlaybackParameters): suspend () -> PlaybackState.Started = {
            Started(playbackParameters, playableMedia, mediaManager)
        }
    }

    data class Started(override val playbackParameters: PlaybackState.PlaybackParameters, override val playableMedia: PlayableMedia, override val mediaManager: MediaManager) : Prepared(), PlaybackState.Started, HandleBeforeOldStateIsRemoved<PlaybackState> {

        override val completed: suspend () -> PlaybackState.Completed = {
            val newLoopMode = when (val loopMode = playbackParameters.loopMode) {
                is PlaybackState.LoopMode.NotLooping -> PlaybackState.LoopMode.NotLooping
                is PlaybackState.LoopMode.LoopingForever -> PlaybackState.LoopMode.LoopingForever
                is PlaybackState.LoopMode.LoopingForFixedNumber -> {
                    val remainingLoops = loopMode.loops.toInt() - 1
                    if (remainingLoops > 0) PlaybackState.LoopMode.LoopingForFixedNumber(remainingLoops.toUInt()) else PlaybackState.LoopMode.NotLooping
                }
            }
            Completed(playbackParameters.copy(loopMode = newLoopMode), playableMedia, mediaManager)
        }
        override val pause: suspend () -> PlaybackState.Paused = { Paused(playbackParameters, playableMedia, mediaManager) }
        override fun updatePlaybackParameters(new: PlaybackState.PlaybackParameters): suspend () -> PlaybackState.Playing = {
            copy(playbackParameters = playbackParameters)
        }

        override suspend fun beforeOldStateIsRemoved(oldState: PlaybackState) {
            mediaManager.play(playbackParameters.rate)
        }
    }

    data class Paused(override val playbackParameters: PlaybackState.PlaybackParameters, override val playableMedia: PlayableMedia, override val mediaManager: MediaManager) : Prepared(), PlaybackState.Paused, HandleAfterOldStateIsRemoved<PlaybackState> {

        override val start: suspend () -> PlaybackState.Started = { Started(playbackParameters, playableMedia, mediaManager) }
        override fun updatePlaybackParameters(new: PlaybackState.PlaybackParameters): suspend () -> PlaybackState.Playing = { copy(playbackParameters = playbackParameters) }
        override suspend fun afterOldStateIsRemoved(oldState: PlaybackState) {
            when (oldState) {
                is PlaybackState.Started -> mediaManager.pause()
                else -> {}
            }
        }
    }

    data class Stopped(private val playableMedia: PlayableMedia, override val mediaManager: MediaManager) : Active(), PlaybackState.Stopped, HandleBeforeOldStateIsRemoved<PlaybackState> {

        override val reinitialize: suspend () -> PlaybackState.Initialized = { Initialized(playableMedia, mediaManager) }

        override suspend fun beforeOldStateIsRemoved(oldState: PlaybackState) {
            when (oldState) {
                is PlaybackState.Prepared -> mediaManager.stop()
                else -> {}
            }
        }
    }

    data class Completed(private val playbackParameters: PlaybackState.PlaybackParameters, override val playableMedia: PlayableMedia, override val mediaManager: MediaManager) : Prepared(), PlaybackState.Completed {

        override val willLoop = when (val loopMode = playbackParameters.loopMode) {
            is PlaybackState.LoopMode.NotLooping -> false
            is PlaybackState.LoopMode.LoopingForever -> true
            is PlaybackState.LoopMode.LoopingForFixedNumber -> loopMode.loops > 0U
        }

        override fun start(playbackParameters: PlaybackState.PlaybackParameters): suspend () -> PlaybackState.Started = {
            mediaManager.seekTo(Duration.ZERO)
            Started(playbackParameters, playableMedia, mediaManager)
        }

        override val restartIfLooping: suspend () -> PlaybackState.Prepared get() = when {
            willLoop -> start(playbackParameters)
            else -> remain()
        }
    }

    data class Error(override val error: PlaybackError, override val mediaManager: MediaManager) : Active(), PlaybackState.Error

    object Ended : PlaybackStateImpl(), PlaybackState.Ended
}
