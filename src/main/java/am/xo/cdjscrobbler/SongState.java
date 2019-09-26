package am.xo.cdjscrobbler;

import am.xo.cdjscrobbler.SongEvents.NowPlayingEvent;
import am.xo.cdjscrobbler.SongEvents.ResetEvent;
import am.xo.cdjscrobbler.SongEvents.ScrobbleEvent;
import am.xo.cdjscrobbler.SongEvents.TransitionEvent;
import org.deepsymmetry.beatlink.CdjStatus;

/**
 * Song Transitions
 * 	Started -> Cueing ✅
 * 	Cueing -> Started ✅
 * 	Cueing -> Playing ✅              <- raise a now playing (get metadata now)
 * 	Playing -> PlayingPaused ✅
 * 	Playing -> Stopped ✅
 * 	Playing -> Scrobbling ✅
 * 	PlayingPaused -> Playing ✅
 * 	PlayingPaused -> Stopped ✅
 * 	Scrobbling -> ScrobblingPaused ✅
 * 	Scrobbling -> Stopped         ✅  <- raise a retire-scrobbling
 * 	ScrobblingPaused -> Scrobbling ✅
 * 	ScrobblingPaused -> Stopped    ✅ <- raise a retire-scrobbling
 */

// TODO: reuse instances of TransitionEvent and ResetEvent

public enum SongState {
    STARTED {
        @Override
        public SongEvent applyNext(SongModel model, CdjStatus update) {
            if(model.isPlayingForward(update)) {
                model.rekordboxId = update.getRekordboxId();
                model.currentState = CUEING;
                // don't bother with a transition event. Nobody cares
            }
            return null;
        }
    },

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
                model.currentState = STARTED;
                // don't bother with a transition event. Nobody cares
            }
            return null;
        }

        @Override
        public boolean isMoving() {
            return true;
        }
    },

    PLAYING {
        @Override
        public SongEvent applyNext(SongModel model, CdjStatus update) {
            model.addPlaytimeFrom(update);
            if(isStopping(model, update)) {
                model.currentState = STOPPED;
                return new ResetEvent();
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

    PLAYINGPAUSED {
        @Override
        public SongEvent applyNext(SongModel model, CdjStatus update) {
            if(isStopping(model, update)) {
                model.currentState = STOPPED;
                return new ResetEvent();
            } else if(model.isPlayingForward(update)) {
                model.currentState = PLAYING;
                return new TransitionEvent(PLAYINGPAUSED, PLAYING);
            }
            return null;
        }
    },

    SCROBBLING {
        @Override
        public SongEvent applyNext(SongModel model, CdjStatus update) {
            model.addPlaytimeFrom(update);
            if(isStopping(model, update)) {
                model.currentState = STOPPED;
                return new ScrobbleEvent(model.song);
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

    SCROBBLINGPAUSED {
        @Override
        public SongEvent applyNext(SongModel model, CdjStatus update) {
            if(isStopping(model, update)) {
                model.currentState = STOPPED;
                return new ScrobbleEvent(model.song);
            } else if(model.isPlayingForward(update)) {
                model.currentState = SCROBBLING;
                return new TransitionEvent(SCROBBLINGPAUSED, SCROBBLING);
            }
            return null;
        }
    },

    STOPPED {
        @Override
        public SongEvent applyNext(SongModel model, CdjStatus update) {
            return null;
        }
    };

    /**
     * transitions the state machine, if necessary. May return an event.
     *
     * @param model
     * @param update
     * @return
     */
    abstract public SongEvent applyNext(SongModel model, CdjStatus update);

    public boolean isStopping(SongModel model, CdjStatus update) {
        return update.isAtEnd() || !update.isTrackLoaded() || update.getRekordboxId() != model.rekordboxId;
    }

    /**
     * @return true if the current state represents playback
     */
    public boolean isMoving() { return false; }
}
