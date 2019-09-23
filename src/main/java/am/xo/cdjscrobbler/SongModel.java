package am.xo.cdjscrobbler;

import am.xo.cdjscrobbler.SongEvents.NowPlayingEvent;
import org.deepsymmetry.beatlink.CdjStatus;
import org.deepsymmetry.beatlink.Util;

public class SongModel {

        /**
         * Song Transitions
         * 	Started -> Ignored (happens when no metadata available)
         * 	Started -> Cueing ✅
         * 	Ignored -> Stopped
         * 	Cueing -> Started ✅
         * 	Cueing -> Playing ✅              <- raise a now playing (get metadata now)
         * 	Playing -> PlayingPaused
         * 	Playing -> Stopped
         * 	Playing -> Scrobbling
         * 	PlayingPaused -> Playing
         * 	PlayingPaused -> Stopped
         * 	Scrobbling -> ScrobblingPaused
         * 	Scrobbling -> Stopped           <- raise a retire-scrobbling
         * 	ScrobblingPaused -> Scrobbling
         * 	ScrobblingPaused -> Stopped     <- raise a retire-scrobbling
         */

    public enum SongState {
        STARTED {
            @Override
            public SongEvent applyNext(SongModel model, CdjStatus update) {
                if(update.isPlayingForwards()) {
                    model.currentState = CUEING;
                }
                return null;
            }
        },

        IGNORED {
            @Override
            public SongEvent applyNext(SongModel model, CdjStatus update) {
                return SongStateTransition.NONE;
            }
        },

        CUEING {
            @Override
            public SongEvent applyNext(SongModel model, CdjStatus update) {
                model.addPlaytimeFrom(update);
                if(update.isPlayingForwards()) {
                    if(model.isPastNowPlaying()) {
                        model.currentState = PLAYING;
                    }
                    return new NowPlayingEvent();
                } else {
                    // whilst cueing - defined as being first 30s of play - stopping play resets the model
                    model.resetPlay();
                    model.currentState = STARTED;
                    return null;
                }
            }
        },

        PLAYING {
            @Override
            public SongEvent applyNext(SongModel model, CdjStatus update) {
                model.addPlaytimeFrom(update);
                if(update.isPlayingForwards()) {
                    if(model.isPastScrobblePoint()) {
                        model.currentState = SCROBBLING;
                    }
                } else {
                    model.currentState = PLAYINGPAUSED;
                }
                return null;
            }
        },

        PLAYINGPAUSED {
            @Override
            public SongEvent applyNext(SongModel model, CdjStatus update) {
                if(update.isPlayingForwards()) {
                    model.currentState = PLAYING;
                }
                return null;
            }
        },

        SCROBBLING {
            @Override
            public SongEvent applyNext(SongModel model, CdjStatus update) {
                model.addPlaytimeFrom(update);
                if(!update.isPlayingForwards()) {
                    model.currentState = SCROBBLINGPAUSED;
                }
                return null;
            }
        },

        SCROBBLINGPAUSED {
            @Override
            public SongEvent applyNext(SongModel model, CdjStatus update) {
                if(update.isPlayingForwards()) {
                    model.currentState = SCROBBLING;
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
    }


    /**
     *   which device 			<- CdjStatus.getTrackSourcePlayer()
     *   has metadata?     	<- true after our media query has finished
     *   song details  		<- set after ^
     *   total played time 	<- updated with each received message
     *   current state 		<- see below
     *   last update timestamp <- timestamp of last CdjStatus received.
     *   						   Needed for incrementing played time,
     */

    protected int deviceNumber = 0;
    protected boolean hasMetadata = false;
    protected SongDetails song;
    protected long totalPlayTime = 0;
    protected SongState currentState = SongState.STARTED;
    protected long lastUpdate = 0;

    public SongModel(int deviceNumber) {
        this.deviceNumber = deviceNumber;
    }

    public SongEvent update(CdjStatus update) {
        SongEvent yieldedEvent = currentState.applyNext(this, );
        lastUpdate = update.getTimestamp();
        return yieldedEvent;
    }

    public void resetPlay() {
        hasMetadata = false;
        song = null;
        totalPlayTime = 0;
    }

    public void addPlaytimeFrom(CdjStatus update) {
        if(lastUpdate != 0 && update.isPlayingForwards()) {
            long nanosToAdd = lastUpdate - update.getTimestamp();
            double tempo = Util.pitchToMultiplier(update.getPitch());
            totalPlayTime += tempo * nanosToAdd;
        }
    }

    public boolean isPastNowPlaying() {
        return totalPlayTime >= 30000000; // 30s
    }

//    public addPlayTime()
}
