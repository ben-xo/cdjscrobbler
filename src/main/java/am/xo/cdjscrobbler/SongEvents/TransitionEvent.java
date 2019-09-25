package am.xo.cdjscrobbler.SongEvents;

import am.xo.cdjscrobbler.SongEvent;
import am.xo.cdjscrobbler.SongState;

public class TransitionEvent implements SongEvent {

    private SongState from, to;

    public TransitionEvent(SongState from, SongState to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public String toString() {
        return String.format("Transition from %s to %s", from.name(), to.name());
    }
}
