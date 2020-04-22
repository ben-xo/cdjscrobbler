CDJ Scrobbler v1.7-SNAPSHOT
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
* If you want to use the Last.fm scrobbling feature, you need to put the following into the file `cdjscrobbler.properties` in your home directory:


        lastfm.api.key=a key you got from https://www.last.fm/api
        lastfm.api.secret=the secret that goes with that key


* If you want to use the Tweet feature, you need to put the following into the file `cdjscrobbler.properties` in your home directory:


        twitter4j.oauth.consumerSecret=a key that you got from https://developer.twitter.com
        twitter4j.oauth.consumerKey=the secret that goes with that key

* Finally, run it in Terminal or Command Prompt:


        java -jar cdjscrobbler-1.7.jar --twitter --lfm

* If this is the first time you have run CDJ Scrobbler, you will be prompted to authorize Last.fm and Twitter in turn. The authorization secrets will be saved into the file `cdjscrobbler.properties` in your home directory. (If you want to use a different file, use the `--conf` option)
* For more information on the options,


        java -jar cdjscrobbler-1.7.jar --help


Options
-------

    -L, --lfm                 Enable Last.fm scrobbling
    -T, --twitter             Enable tweeting the tracklist
        --config=<filename>   Which config file to use. Defaults to cdjscrobbler.properties in your home directory
        --no-dmca-warning     Disable flashing the platter red if the loaded track would break DMCA rules
        --csv=<filename>      Output a CSV file compatible with https://github.com/ben-xo/prepare-podcast
    -h, --help                Show this help message and exit.
    -V, --version             Print version information and exit.


Advanced Features
=================

Saving Tracklists Offline
-------------------------

If there is no internet connect in your DJ environment, then you can use the `--csv` option to log the tracks played to a file so that they can be used later. 

DMCA Warning
------------

Some websites, such as Mixcloud, have restrictions on mixes which contain too many tracks from the same artist or album. These restrictions are a condition of having a stream qualify as a "radio broadcast" - an example of the rules can be found here: https://support.live365.com/hc/en-us/articles/115002892247-What-is-DMCA-

In practise, the limits are such that you should not play more than 3 songs from a single artist, or 4 songs from a single album.

CDJScrobbler will use the "On air" feature of the Pioneer CDJ to flash the platter ring red if you load a song which would break these rules. However, if you actually use the On Air feature with a Pioneer mixer to turn the ring red when a player is audible, then this will interfere with that. You can disable this feature by passing the option `--no-dmca-warning`.


What's happening?
=================

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
* Picocli            (https://github.com/remkop/picocli) - Apache License 2.0


