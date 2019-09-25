package am.xo.cdjscrobbler.SongEvents;

import am.xo.cdjscrobbler.SongEvent;

public class NowPlayingEvent implements SongEvent {

    @Override
    public String toString() {
        return String.format("** NOW PLAYING **");
    }
}
