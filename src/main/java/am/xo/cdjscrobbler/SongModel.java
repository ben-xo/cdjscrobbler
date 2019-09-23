package am.xo.cdjscrobbler;

import am.xo.cdjscrobbler.SongEvents.NowPlayingEvent;
import am.xo.cdjscrobbler.SongEvents.ResetEvent;
import am.xo.cdjscrobbler.SongEvents.ScrobbleEvent;
import am.xo.cdjscrobbler.SongEvents.TransitionEvent;
import org.deepsymmetry.beatlink.CdjStatus;
import org.deepsymmetry.beatlink.Util;

import java.util.ArrayList;

public class SongModel {

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
                if(update.isPlayingForwards()) {
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
                if(update.isPlayingForwards()) {
                    // we'll take whatever it is, at this point.
                    // we don't try resolve any metadata until after the now playing point anyway
                    model.rekordboxId = update.getRekordboxId();
                    if(model.isPastNowPlaying()) {
                        model.currentState = PLAYING;
                        return new NowPlayingEvent();
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
        },

        PLAYING {
            @Override
            public SongEvent applyNext(SongModel model, CdjStatus update) {
                model.addPlaytimeFrom(update);
                if(isStopping(model, update)) {
                    model.currentState = STOPPED;
                    return new ResetEvent();
                } else if(update.isPlayingForwards()) {
                    if (model.isPastScrobblePoint()) {
                        model.currentState = SCROBBLING;
                        return new TransitionEvent();
                    }
                } else {
                    model.currentState = PLAYINGPAUSED;
                    return new TransitionEvent();
                }
                return null;
            }
        },

        PLAYINGPAUSED {
            @Override
            public SongEvent applyNext(SongModel model, CdjStatus update) {
                if(isStopping(model, update)) {
                    model.currentState = STOPPED;
                    return new ResetEvent();
                } else if(update.isPlayingForwards()) {
                    model.currentState = PLAYING;
                    return new TransitionEvent();
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
                } else if(!update.isPlayingForwards()) {
                    model.currentState = SCROBBLINGPAUSED;
                    return new TransitionEvent();
                }
                return null;
            }
        },

        SCROBBLINGPAUSED {
            @Override
            public SongEvent applyNext(SongModel model, CdjStatus update) {
                if(isStopping(model, update)) {
                    model.currentState = STOPPED;
                    return new ScrobbleEvent(model.song);
                } else if(update.isPlayingForwards()) {
                    model.currentState = SCROBBLING;
                    return new TransitionEvent();
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
    protected SongDetails song;
    protected long totalPlayTime = 0;
    protected SongState currentState = SongState.STARTED;
    protected long lastUpdate = 0;
    protected int rekordboxId = 0;

    public SongModel(int deviceNumber) {
        this.deviceNumber = deviceNumber;
    }

    public ArrayList<SongEvent> update(CdjStatus update) {
        return update(update, new ArrayList<SongEvent>);
    }

    protected ArrayList<SongEvent> update(CdjStatus update, ArrayList<SongEvent> returnedEvents) {
        SongEvent yieldedEvent = currentState.applyNext(this, update);
        lastUpdate = update.getTimestamp();
        if(yieldedEvent != null) {
            // must apply continuously to catch all events in sequence.
            // this could happen if a single event increments time by a large amount.
            // (TODO: Yeah, this could be a loop)
            returnedEvents.add(yieldedEvent);
            return update(update, returnedEvents);
        }
        return returnedEvents;
    }

    public void resetPlay() {
        song = null;
        rekordboxId = 0;
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

    public boolean isPastScrobblePoint() {
        return song != null && totalPlayTime > song.getScrobblePointNanos();
    }

}
