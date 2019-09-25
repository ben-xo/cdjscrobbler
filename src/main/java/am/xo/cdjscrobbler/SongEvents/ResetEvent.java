package am.xo.cdjscrobbler.SongEvents;

import am.xo.cdjscrobbler.SongEvent;

public class ResetEvent implements SongEvent {

    @Override
    public String toString() {
        return String.format("** RESET (Song stopped or changed) **");
    }
}
