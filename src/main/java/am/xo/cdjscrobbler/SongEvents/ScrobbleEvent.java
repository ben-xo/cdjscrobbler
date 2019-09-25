package am.xo.cdjscrobbler.SongEvents;

import am.xo.cdjscrobbler.SongDetails;
import am.xo.cdjscrobbler.SongEvent;

public class ScrobbleEvent implements SongEvent {
    public SongDetails song;
    public ScrobbleEvent(SongDetails song) {
        this.song = song;
    }

    @Override
    public String toString() {
        return String.format("** SCROBBLE %s **", song.getFullTitle());
    }
}
