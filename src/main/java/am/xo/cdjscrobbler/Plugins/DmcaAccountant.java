/*
 * Copyright (c) 2019, Ben XO.
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package am.xo.cdjscrobbler.Plugins;

import am.xo.cdjscrobbler.Plugins.Helpers.OnAirWarning;
import am.xo.cdjscrobbler.SongDetails;
import am.xo.cdjscrobbler.SongEventListeners.NewSongLoadedListener;
import am.xo.cdjscrobbler.SongEventListeners.NowPlayingListener;
import am.xo.cdjscrobbler.SongEvents.NewSongLoadedEvent;
import am.xo.cdjscrobbler.SongEvents.NowPlayingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * This class keeps a log of the songs you've played, so that you can be warned if you're going to play a song that
 * would prevent your show being aired unrestricted on Mixcloud or other services which follow the "No more than 4 songs
 * by the same artist" DMCA rule
 */
public class DmcaAccountant implements NowPlayingListener, NewSongLoadedListener {
    static final Logger logger = LoggerFactory.getLogger(DmcaAccountant.class);

    ArrayList<SongDetails> played = new ArrayList<>();
    OnAirWarning warning = new OnAirWarning();

    public void start() {
        warning.start();
    }

    @Override
    public void newSongLoaded(NewSongLoadedEvent event) {
        if(!checkIsSafeToPlay(event.model.getSong())) {
            warning.setWarn(event.cdjStatus.getDeviceNumber());
        } else {
            warning.removeWarn(event.cdjStatus.getDeviceNumber());
        }
    }

    @Override
    public void nowPlaying(NowPlayingEvent event) {
        addPlayed(event.model.getSong());
    }

    public void addPlayed(SongDetails song) {
        if(song == null) {
            logger.warn("Tried to do DMCA accounting on a song but we don't know what it is!");
            return;
        }
        played.add(song);
        logger.info("Logged play for song {} - song #{}", song, played.size());
    }

    public boolean checkIsSafeToPlay(SongDetails song) {
        if(!isSafeToPlay(song)) {
            logger.warn("⚠️⚠️⚠️ Don't play this song!");
            return false;
        }
        return true;
    }

    public boolean isSafeToPlay(SongDetails song) {
        if(song == null) {
            return true;
        }
        if(getArtistPlayCount(song.getArtist()) >= 4) {
            logger.warn("❌ You have already played artist {} 4 times this show.", song.getArtist());
            return false;
        }
        if(getAlbumPlayCount(song.getAlbum()) >= 3) {
            logger.warn("❌ You have already played album {} 3 times this show.", song.getAlbum());
            return false;
        }
        return true;
    }

    private long getArtistPlayCount(String artist) {
        return played.stream().filter(
                song -> song.getArtist().toUpperCase().equals(artist.toUpperCase())
        ).count();
    }

    private long getAlbumPlayCount(String album) {
        return played.stream().filter(
                song -> song.getAlbum().toUpperCase().equals(album.toUpperCase())
        ).count();
    }
}
