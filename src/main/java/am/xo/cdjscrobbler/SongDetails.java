package am.xo.cdjscrobbler;

import org.deepsymmetry.beatlink.data.TrackMetadata;

public class SongDetails {

    protected TrackMetadata theTrack;
    long scrobblePoint;

    public SongDetails(TrackMetadata t) {
        theTrack = t;
        calculateScrobblePoint();
    }

    private void calculateScrobblePoint() {
        int d = theTrack.getDuration();
        if (d < 60) {
            // min scrobble point is 30s.
            scrobblePoint = 30;
        } else if( d > 480) {
            // max scrobble point is 4 mins
            scrobblePoint = 240;
        } else {
            scrobblePoint = d / 2;
        }
    }

    public long getScrobblePoint() {
        return scrobblePoint;
    }

    public String getFullTitle() {
        // TODO: wrong way to render artist, fix later
        return theTrack.getArtist().toString() + " – " + theTrack.getTitle();
    }
}
