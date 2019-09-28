CDJ Scrobbler v1.0
------------------

CDJ Scrobbler is a Last.fm Scrobbler for Pioneer CDJ / XDJ (e.g. CDJ 2000) based on beat-link.

Build
=====

You will need maven installed.

  mvn package

Run
===

You will need Java 1.8 or above installed. The computer you run CDJ Scrobbler from must be on the same network as the CDJs! So, plug them into an ethernet hub or switch, and plug the computer into the same switch.

  java -jar cdjscrobbler-1.0.jar 




What's happening?
-----------------

CDJ Scrobbler follows along events from the CDJs, and provides the following state machine to react to them. 


        +---------+
    +-->| STARTED |
    |   +----+----+
    |        |
    |        | Pressing play starts the timer. (Pausing, at any stage, stops the timer.)
    |        |
    |        v
    |   +----+----+
    |   | CUEING  |
    |   +----+----+
    |        |
    |        | After 10 seconds of playback, the song is officially "Playing" and we 
    |        | send a Now Playing update to Last.fm and Twitter. This does not include 
    |        | time when the track is paused, but it does take the tempo slider into account.
    |        |
    |        v
    |   +----+----+
    |   | PLAYING |
    |   +----+----+
    |        |
    |        | After half the song length, we consider the song to be "Scrobbling" which 
    |        | means that it will be scrobbled to Last.fm when the song ends.
    |        |
    |        v
    |   +----+-------+
    |   | SCROBBLING |
    |   +----+-------+
    |        |
    |        | When the song ends (naturally, or because a new song started on the deck),
    |        | this is when we send a scrobble to Last.fm.
    |        |
    |        v
    |   +----+----+
    |   | STOPPED |
    |   +----+----+
    |        |
    |        | After the song has stopped, we reset back to Started for a new song.
    +--------+

