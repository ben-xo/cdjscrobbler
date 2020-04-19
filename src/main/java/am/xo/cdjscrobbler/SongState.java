/*
 * Copyright (c) 2019, Ben XO.
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package am.xo.cdjscrobbler;

import am.xo.cdjscrobbler.SongEvents.NewSongLoadedEvent;
import am.xo.cdjscrobbler.SongEvents.NowPlayingEvent;
import am.xo.cdjscrobbler.SongEvents.ResetEvent;
import am.xo.cdjscrobbler.SongEvents.ScrobbleEvent;
import am.xo.cdjscrobbler.SongEvents.TransitionEvent;
import org.deepsymmetry.beatlink.CdjStatus;

// TODO: reuse instances of TransitionEvent and ResetEvent

/**
 * State machine enum. SongModel references the current state of a turntable.
 *
 * As each CdjStatus update is received, that status update is applied to the model, and the state machine run
 * accordingly. (See diagram in README.md). Most transitions will emit an event which can be handled (eventually) by
 * the QueueProcessor. The most interesting ones are NowPlayingEvent (from a CUEING to PLAYING transition) and
 * ScrobbleEvent (from SCROBBLING or SCROBBLINGPAUSED to STOPPED transition), although ResetEvent (from most other
 * states to STOPPED) is also interesting as it resets the SongModel.
 *
 */
public enum SongState {

    /**
     * State that indicates that a CDJ has recently been loaded with a new track.
     */
    STARTED {
        @Override
        public SongEvent applyNext(SongModel model, CdjStatus update) {
            if(model.isPlayingForward(update)) {
                model.rekordboxId = update.getRekordboxId();
                model.currentState = CUEING;
                model.startedAt = System.currentTimeMillis() / 1000;
                return new NewSongLoadedEvent(model, update);
            }
            return null;
        }
    },

    /**
     * Track is playing, but for < e.g. 10s. The song will alternate between CUEING and STARTED as you cue up the track.
     */
    CUEING {
        @Override
        public SongEvent applyNext(SongModel model, CdjStatus update) {
            model.addPlaytimeFrom(update);
            if(model.isPlayingForward(update)) {
                // we'll take whatever it is, at this point.
                // we don't try resolve any metadata until after the now playing point anyway
                model.rekordboxId = update.getRekordboxId();
                if(model.isPastNowPlaying()) {
                    model.currentState = PLAYING;
                    return new NowPlayingEvent(model, update);
                }
            } else {
                // whilst cueing - any sort of stopping play resets the model.
                // this includes searching, rewinding, or other events.
                model.resetPlay();
                model.currentState = CUEINGPAUSED;
                // don't bother with a transition event. Nobody cares
            }
            return null;
        }

        @Override
        public boolean isMoving() {
            return true;
        }
    },

    /**
     * Paused, but before the Now playing
     */
    CUEINGPAUSED {
        @Override
        public SongEvent applyNext(SongModel model, CdjStatus update) {
            if(isStopping(model, update)) {
                model.currentState = STOPPED;
                return new ResetEvent(update);
            } else if(model.isPlayingForward(update)) {
                model.currentState = CUEING;
                // don't bother with a transition event. Nobody cares
            }
            return null;
        }
    },

    /**
     * The track has been playing without being interrupted for long enough that we now really think it's playing.
     */
    PLAYING {
        @Override
        public SongEvent applyNext(SongModel model, CdjStatus update) {
            model.addPlaytimeFrom(update);
            if(isStopping(model, update)) {
                model.currentState = STOPPED;
                return new ResetEvent(update);
            } else if(model.isPlayingForward(update)) {
                if (model.isPastScrobblePoint()) {
                    model.currentState = SCROBBLING;
                    return new TransitionEvent(PLAYING, SCROBBLING);
                }
            } else {
                model.currentState = PLAYINGPAUSED;
                return new TransitionEvent(PLAYING, PLAYINGPAUSED);
            }
            return null;
        }

        @Override
        public boolean isMoving() {
            return true;
        }
    },

    /**
     * Paused, but before the scrobble point.
     */
    PLAYINGPAUSED {
        @Override
        public SongEvent applyNext(SongModel model, CdjStatus update) {
            if(isStopping(model, update)) {
                model.currentState = STOPPED;
                return new ResetEvent(update);
            } else if(model.isPlayingForward(update)) {
                model.currentState = PLAYING;
                return new TransitionEvent(PLAYINGPAUSED, PLAYING);
            }
            return null;
        }
    },

    /**
     * The track has been playing for long enough that we want to scrobble it, and is still playing.
     */
    SCROBBLING {
        @Override
        public SongEvent applyNext(SongModel model, CdjStatus update) {
            model.addPlaytimeFrom(update);
            if(isStopping(model, update)) {
                model.currentState = STOPPED;
                return new ScrobbleEvent(model, update);
            } else if(!model.isPlayingForward(update)) {
                model.currentState = SCROBBLINGPAUSED;
                return new TransitionEvent(SCROBBLING, SCROBBLINGPAUSED);
            }
            return null;
        }

        @Override
        public boolean isMoving() {
            return true;
        }
    },

    /**
     * Paused, but after the scrobble point.
     */
    SCROBBLINGPAUSED {
        @Override
        public SongEvent applyNext(SongModel model, CdjStatus update) {
            if(isStopping(model, update)) {
                model.currentState = STOPPED;
                return new ScrobbleEvent(model, update);
            } else if(model.isPlayingForward(update)) {
                model.currentState = SCROBBLING;
                return new TransitionEvent(SCROBBLINGPAUSED, SCROBBLING);
            }
            return null;
        }
    },

    /**
     * The track has ended (either ran out, ejected or a new track loaded). The song model will usually be
     * destroyed and recreated, so it won't stay in this state for very long.
     */
    STOPPED {
        @Override
        public SongEvent applyNext(SongModel model, CdjStatus update) {
            return null;
        }
    };

    /**
     * Transitions the state machine, if necessary. May return an event.
     *
     * @param model
     * @param update
     * @return
     */
    abstract public SongEvent applyNext(SongModel model, CdjStatus update);

    /**
     * Various properties of a CdjUpdate may indicate that the song has ended, been ejected or has changed.
     *
     * @param model
     * @param update
     * @return
     */
    public boolean isStopping(SongModel model, CdjStatus update) {
        return update.isAtEnd() || !update.isTrackLoaded() || update.getRekordboxId() != model.rekordboxId;
    }

    /**
     * Does the current state represent playback? (Paused and stopped states don't).
     *
     * Overridden by the states that do.
     *
     * @return true if the current state represents playback
     */
    public boolean isMoving() { return false; }
}
