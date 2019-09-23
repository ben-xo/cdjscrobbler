package am.xo.cdjscrobbler;

import am.xo.cdjscrobbler.SongEvents.NowPlayingEvent;
import am.xo.cdjscrobbler.SongEvents.ResetEvent;
import am.xo.cdjscrobbler.SongEvents.ScrobbleEvent;
import org.deepsymmetry.beatlink.CdjStatus;
import org.deepsymmetry.beatlink.DeviceUpdate;
import org.deepsymmetry.beatlink.Util;

public class SongModel {

    public enum SongStateTransition {

        /**
         * Song Transitions
         * 	Started -> Ignored (happens when no metadata available)
         * 	Started -> Cueing
         * 	Ignored -> Stopped
         * 	Cueing -> Started
         * 	Cueing -> Playing               <- raise a now playing (get metadata now)
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

        NONE,
        TO_STARTED {
            @Override
            public SongState nextState(CdjStatus update, SongModel model) {
                return SongState.STARTED;
            }
            @Override
            public SongEvent yieldEvent(CdjStatus update, SongModel model){

                return new ResetEvent(); //TODO finish me
            }
        },
        TO_IGNORED {
            @Override
            public SongState nextState(CdjStatus update, SongModel model) {
                return SongState.IGNORED;
            }
        },
        TO_CUEING {
            @Override
            public SongState nextState(CdjStatus update, SongModel model) {
                return SongState.CUEING;
            }
        },
        TO_PLAYING {
            @Override
            public SongState nextState(CdjStatus update, SongModel model) {
                return SongState.PLAYING;
            }

            @Override
            public SongEvent yieldEvent(CdjStatus update, SongModel model){
                return new NowPlayingEvent(); //TODO finish me
            }
        },
        TO_SCROBBLING {
            @Override
            public SongState nextState(CdjStatus update, SongModel model) {
                return SongState.SCROBBLING;
            }
        },
        STILL_CUEING {
            @Override
            public SongState nextState(CdjStatus update, SongModel model) {
                return SongState.CUEING;
            }
        },
        STILL_PLAYING {
            @Override
            public SongState nextState(CdjStatus update, SongModel model) {
                return SongState.PLAYING;
            }
        },
        STILL_SCROBBLING {
            @Override
            public SongState nextState(CdjStatus update, SongModel model) {
                return SongState.SCROBBLING;
            }
        },
        SCROBBLING_TO_STOPPED {
            @Override
            public SongState nextState(CdjStatus update, SongModel model) {
                return SongState.STOPPED;
            }
            @Override
            public SongEvent yieldEvent(CdjStatus update, SongModel model){
                return new ScrobbleEvent(); //TODO finish me
            }
        },
        OTHER_TO_STOPPED,
        STILL_STOPPED;

        public SongState nextState(CdjStatus update, SongModel model) {
            return SongState.STOPPED;
        }

        public SongEvent yieldEvent(CdjStatus update, SongModel model){
            return null;
        }

    }

    public enum SongState {
        STARTED {
            @Override
            public SongStateTransition next(CdjStatus update, SongModel model) {
                if(update.isPlayingForwards()) {
                    model.addPlaytimeFrom(update);
                    return SongStateTransition.TO_CUEING;
                }
                return SongStateTransition.NONE;
            }
        },

        IGNORED {
            @Override
            public SongStateTransition next(CdjStatus update, SongModel model) {
                return SongStateTransition.NONE;
            }
        },

        CUEING {
            @Override
            public SongStateTransition next(CdjStatus update, SongModel model) {
                if(update.isPlayingForwards()) {
                    model.addPlaytimeFrom(update);
                    return SongStateTransition.TO_CUEING;
                }
                return SongStateTransition.TO_STARTED;
            }
        },

        PLAYING {
            @Override
            public SongStateTransition next(CdjStatus update, SongModel model) {
                return SongStateTransition.NONE;
            }
        },

        PLAYINGPAUSED {
            @Override
            public SongStateTransition next(CdjStatus update, SongModel model) {
                return SongStateTransition.NONE;
            }
        },

        SCROBBLING {
            @Override
            public SongStateTransition next(CdjStatus update, SongModel model) {
                return SongStateTransition.NONE;
            }
        },

        SCROBBLINGPAUSED {
            @Override
            public SongStateTransition next(CdjStatus update, SongModel model) {
                return SongStateTransition.NONE;
            }
        },

        STOPPED {
            @Override
            public SongStateTransition next(CdjStatus update, SongModel model) {
                return SongStateTransition.NONE;
            }
        };

        abstract public SongStateTransition next(CdjStatus update, SongModel model);
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
        SongStateTransition t = currentState.next(update, this);
        SongState nextState    = t.nextState(update, this);
        SongEvent yieldedEvent = t.yieldEvent(update, this);

        lastUpdate = update.getTimestamp();
        currentState = nextState;
        return yieldedEvent;
    }

    public SongEvent yieldEvent() {
        // most situations do not yield an event
        return null;
    }

    public void addPlaytimeFrom(CdjStatus update) {
        if(lastUpdate != 0) {
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
