package am.xo.cdjscrobbler;

import org.deepsymmetry.beatlink.data.TrackMetadata;

public class SongDetails {

    protected TrackMetadata theTrack;
    long scrobblePointNanos;

    public SongDetails(TrackMetadata t) {
        theTrack = t;
        calculateScrobblePointNanos();
    }

    private void calculateScrobblePointNanos() {
        int d = theTrack.getDuration();
        if (d < 60) {
            // min scrobble point is 30s.
            scrobblePointNanos = 30000000;
        } else if( d > 480) {
            // max scrobble point is 4 mins
            scrobblePointNanos = 240000000;
        } else {
            scrobblePointNanos = (d * 1000000) / 2;
        }
    }

    public long getScrobblePointNanos() {
        return scrobblePointNanos;
    }
}
