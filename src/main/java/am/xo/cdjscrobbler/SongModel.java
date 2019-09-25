package am.xo.cdjscrobbler;

import am.xo.cdjscrobbler.SongDetails;

import org.deepsymmetry.beatlink.CdjStatus;
import org.deepsymmetry.beatlink.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SongModel {

    static final int NOW_PLAYING_POINT = 10000; // milliseconds (e.g. 30000 = 30 seconds

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
    protected SongDetails song = null;
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
        SongState prevState = currentState;
        SongEvent yieldedEvent = currentState.applyNext(this, update);

        logger.info(String.format("Device %d rekordbox ID %d %s -> %s for %s",
                deviceNumber, rekordboxId, prevState.name(), currentState.name(), this));

        lastUpdate = update.getTimestamp();
        if(yieldedEvent != null) {
            logger.debug("yielded " + yieldedEvent.getClass().getSimpleName());
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
        if(lastUpdate != 0 && isPlayingForward(update)) {
            long nanosToAdd = update.getTimestamp() - lastUpdate;
            double tempo = Util.pitchToMultiplier(update.getPitch());
            totalPlayTime += tempo * (nanosToAdd / 1000000);
        }
    }

    public boolean isPastNowPlaying() {
        return totalPlayTime >= NOW_PLAYING_POINT; // 30s
    }

    public boolean isPastScrobblePoint() {
        return song != null && (totalPlayTime / 1000) > song.getScrobblePoint();
    }

    public boolean isPlayingForward(CdjStatus update) {
        return update.isPlaying() && update.getPlayState2() == CdjStatus.PlayState2.MOVING;
    }

    public String toString() {
        return "Device " + Integer.toString(deviceNumber) + " " + currentState.name()
                + " song: " + (song == null ? "<unknown>" : song.toString())
                + " playtime: " + Long.toString(totalPlayTime) + " ms";
    }

}
