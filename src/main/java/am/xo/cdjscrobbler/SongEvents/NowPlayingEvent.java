package am.xo.cdjscrobbler.SongEvents;

import am.xo.cdjscrobbler.SongEvent;
import am.xo.cdjscrobbler.SongModel;
import org.deepsymmetry.beatlink.CdjStatus;

public class NowPlayingEvent implements SongEvent {

    public SongModel model;
    public CdjStatus cdjStatus;

    public NowPlayingEvent(SongModel m, CdjStatus s) {
        model = m;
        cdjStatus = s;
    }

    @Override
    public String toString() {
        return String.format("** NOW PLAYING **");
    }
}
