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
        int d = getDuration();
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
        return getArtist() + " – " + getTitle();
    }

    public String toString() {
        return getFullTitle();
    }

    // access delegates for the TrackMeatadata for the Song.

    public String getArtist() {
        return theTrack.getArtist().label;
    }

    public String getTitle() {
        return theTrack.getTitle();
    }

    public String getAlbum() {
        return theTrack.getAlbum().label;
    }

    public int getDuration() {
        return theTrack.getDuration();
    }
}
