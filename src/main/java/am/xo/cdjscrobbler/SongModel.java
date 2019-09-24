package am.xo.cdjscrobbler;

import org.deepsymmetry.beatlink.CdjStatus;
import org.deepsymmetry.beatlink.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SongModel {


    /**
     *   which device 			<- CdjStatus.getTrackSourcePlayer()
     *   has metadata?     	<- true after our media query has finished
     *   song details  		<- set after ^
     *   total played time 	<- updated with each received message
     *   current state 		<- see below
     *   last update timestamp <- timestamp of last CdjStatus received.
     *   						   Needed for incrementing played time,
     */

    final Logger logger = LoggerFactory.getLogger(SongModel.class);

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
        return update(update, new ArrayList<>());
    }

    protected ArrayList<SongEvent> update(CdjStatus update, ArrayList<SongEvent> returnedEvents) {
        SongEvent yieldedEvent = currentState.applyNext(this, update);
        lastUpdate = update.getTimestamp();
        if(yieldedEvent != null) {
            logger.info(yieldedEvent.toString());
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
