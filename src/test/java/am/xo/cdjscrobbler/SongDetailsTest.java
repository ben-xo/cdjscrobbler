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

import junit.framework.TestCase;
import org.deepsymmetry.beatlink.data.TrackMetadata;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SongDetailsTest extends TestCase {

    TrackMetadata track;

    public void setUp() {
        track = mock(TrackMetadata.class);
    }

    public void test_scrobble_point_too_short_track() {
        when(track.getDuration()).thenReturn(10);

        SongDetails s = new SongDetails(track);
        assertEquals(s.getDuration(), 10);
        assertEquals(s.getScrobblePoint(), 15);
    }

    public void test_scrobble_point_shortest_track() {
        when(track.getDuration()).thenReturn(30);

        SongDetails s = new SongDetails(track);
        assertEquals(s.getDuration(), 30);
        assertEquals(s.getScrobblePoint(), 15);
    }

    public void test_scrobble_point_normal_track() {
        when(track.getDuration()).thenReturn(100);

        SongDetails s = new SongDetails(track);
        assertEquals(s.getDuration(), 100);
        assertEquals(s.getScrobblePoint(), 50);
    }

    public void test_scrobble_point_longest_track() {
        when(track.getDuration()).thenReturn(480);

        SongDetails s = new SongDetails(track);
        assertEquals(s.getDuration(), 480);
        assertEquals(s.getScrobblePoint(), 240);
    }

    public void test_scrobble_point_too_long_track() {
        when(track.getDuration()).thenReturn(4800);

        SongDetails s = new SongDetails(track);
        assertEquals(s.getDuration(), 4800);
        assertEquals(s.getScrobblePoint(), 240);
    }
}