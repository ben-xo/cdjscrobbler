package am.xo.cdjscrobbler.SongEvents;

import am.xo.cdjscrobbler.SongEvent;
import am.xo.cdjscrobbler.SongModel;
import org.deepsymmetry.beatlink.CdjStatus;

public class ScrobbleEvent implements SongEvent {

    public SongModel model;
    public CdjStatus cdjStatus;

    public ScrobbleEvent(SongModel m, CdjStatus s) {
        this.model = m;
        this.cdjStatus = s;
    }

    @Override
    public String toString() {
        return String.format("** SCROBBLE %s **", model.getSong().getFullTitle());
    }
}
