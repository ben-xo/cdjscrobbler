CDJ Scrobbler v1.4-SNAPSHOT
---------------------------

CDJ Scrobbler is a Last.fm Scrobbler for Pioneer CDJ / XDJ (e.g. CDJ 2000) based on beat-link.

Build
=====

You will need maven installed.

    mvn package

Run
===

* You will need Java 1.8 or above installed. 
* The computer you run CDJ Scrobbler from must be on the same network as the CDJs! So, plug them into an ethernet hub or switch, and plug the computer into the same switch.
* If you want to use the Last.fm scrobbling feature, you need to put the following into the file cdjscrobbler.properties in your home directory:

        lastfm.api.key=a key you got from https://www.last.fm/api
        lastfm.api.secret=the secret that goes with that key
        cdjscrobbler.enable.lastfm=true
    

* If you want to use the Tweet feature, you need to put the following into the file cdjscrobbler.properties in your home directory:

        twitter4j.oauth.consumerSecret=a key that you got from https://developer.twitter.com
        twitter4j.oauth.consumerKey=the secret that goes with that key
        cdjscrobbler.enable.twitter=true

* Finally, run it in Terminal or Command Prompt:

        java -jar cdjscrobbler-1.0.jar 


What's happening?
-----------------

CDJ Scrobbler follows along events from the CDJs, and provides the following state machine to react to them. 


        +---------+
    +-->| STARTED |
    |   +----+----+
    |        |
    ^        | Pressing play starts the timer. (Pausing, at any stage, stops the timer.)
    |        |
    |        v
    |   +----+----+
    +---+ CUEING  | (Pausing whilst CUEING resets us to STARTED.)
    |   +----+----+
    |        |
    |        | After 10 seconds of playback, the song is officially "Playing" and we 
    ^        | send a Now Playing update to Last.fm and Twitter. This does not include 
    |        | time when the track is paused, but it does take the tempo slider into account.
    |        |
    |        v
    |   +----+----+      (Track ends without going past half way point? -> STOPPED)
    |   | PLAYING +------------------------------------------------------------------------------+
    |   +----+----+                                                                              |
    |        |                                                                                   |
    |        | After half the song length, we consider the song to be "Scrobbling" which         |
    ^        | means that it will be scrobbled to Last.fm when the song ends.                    |
    |        |                                                                                   |
    |        v                                                                                   |
    |   +----+-------+                                                                           |
    |   | SCROBBLING |                                                                           |
    |   +----+-------+                                                                           |
    |        |                                                                                   |
    |        | When the song ends (naturally, or because a new song started on the deck),        |
    |        | this is when we send a scrobble to Last.fm.                                       |
    ^        |                                                                                   |
    |        v                                                                                   |
    |   +----+----+                                                                              |
    |   | STOPPED |<-----------------------------------------------------------------------------+
    |   +----+----+
    |        |
    |        | After the song has stopped, we reset back to Started for a new song.
    +--------+

Credits
-------

CDJ Scrobbler was written by @benxo (https://twitter.com/benxo) and uses the following components:

* beat-link          (https://github.com/Deep-Symmetry/beat-link) - Eclipse Public License
* lastfm-java        (https://github.com/jkovacs) - BSD License
* ScribeJava         (https://github.com/scribejava/scribejava) - MIT License
* Twitter4J          (http://twitter4j.org/) - Apache License 2.0
* Ordered Properties (https://github.com/etiennestuder/java-ordered-properties) - Apache License 2.0
  (This is copied into the project as it's not available on maven central)

