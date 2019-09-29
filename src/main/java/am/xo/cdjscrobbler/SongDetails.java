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

package am.xo.cdjscrobbler;

import org.deepsymmetry.beatlink.data.TrackMetadata;


/**
 * Encapsulates the data from a Track that's used by the song model, as well as basic text used for scrobbling and
 * tweeting. Most of the fields are just delegated onto the TrackMetadata object from beat-link, but the scrobble-point
 * (which is usually but not always the half-way point) is based on rules from https://www.last.fm/api/scrobbling
 */
public class SongDetails {

    protected TrackMetadata theTrack;
    long scrobblePoint;

    public SongDetails(TrackMetadata t) {
        theTrack = t;
        calculateScrobblePoint();
    }

    private void calculateScrobblePoint() {
        int d = getDuration();
        if (d < 30) {
            // min scrobble point is 15s.
            scrobblePoint = 15;
        } else if( d > 480) {
            // max scrobble point is 4 mins
            scrobblePoint = 240;
        } else {
            scrobblePoint = d / 2;
        }
    }

    public long getScrobblePoint() {
        return scrobblePoint;
    }

    public String getFullTitle() {
        return getArtist() + " – " + getTitle();
    }

    public String toString() {
        return getFullTitle();
    }

    // access delegates for the TrackMeatadata for the Song.

    public String getArtist() {
        return theTrack.getArtist().label;
    }

    public String getTitle() {
        return theTrack.getTitle();
    }

    public String getAlbum() {
        return theTrack.getAlbum().label;
    }

    public int getDuration() {
        return theTrack.getDuration();
    }
}
