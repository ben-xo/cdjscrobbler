/*
 * Copyright (c) 2020, Ben XO.
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
import org.deepsymmetry.beatlink.CdjStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SongStateTest extends TestCase {

    @Mock
    CdjStatus update;

    @Mock
    SongModel m;

    @Before
    public void setup() {
        m.rekordboxId = 100;
        m.currentState = SongState.STARTED;
    }

    @Test
    public void test_isStopping_if_at_end() {
        when(update.isAtEnd()).thenReturn(true);
        lenient().when(update.isTrackLoaded()).thenReturn(true);
        lenient().when(update.getRekordboxId()).thenReturn(100);
        assertEquals(true, SongState.STOPPED.isStopping(m, update));
    }

    @Test
    public void test_isStopping_no_track_loaded() {
        lenient().when(update.isAtEnd()).thenReturn(false);
        when(update.isTrackLoaded()).thenReturn(false);
        lenient().when(update.getRekordboxId()).thenReturn(100);
        assertEquals(true, SongState.STOPPED.isStopping(m, update));
    }

    @Test
    public void test_isStopping_different_song() {
        lenient().when(update.isAtEnd()).thenReturn(false);
        lenient().when(update.isTrackLoaded()).thenReturn(true);
        when(update.getRekordboxId()).thenReturn(999);
        assertEquals(true, SongState.STOPPED.isStopping(m, update));
    }

    @Test
    public void test_not_isStopping_if_same_song_still_hasnt_ended() {
        when(update.isAtEnd()).thenReturn(false);
        when(update.isTrackLoaded()).thenReturn(true);
        when(update.getRekordboxId()).thenReturn(100);
        assertEquals(false, SongState.STOPPED.isStopping(m, update));
    }

    @Test
    public void test_STARTED_does_not_start_until_a_song_is_playing() {
        when(m.isPlayingForward(update)).thenReturn(false);
        SongState.STARTED.applyNext(m, update);
        assertEquals(SongState.STARTED, m.currentState);
    }

    @Test
    public void test_STARTED_does_not_emit_event_when_no_song_is_playing() {
        when(m.isPlayingForward(update)).thenReturn(false);
        assertNull(SongState.STARTED.applyNext(m, update));
    }

    @Test
    public void test_STARTED_to_CUEING_when_a_song_is_playing() {
        when(m.isPlayingForward(update)).thenReturn(true);
        SongState.STARTED.applyNext(m, update);
        assertEquals(SongState.CUEING, m.currentState);
    }

    public void test_STARTED_records_basic_song_details_when_a_song_starts() {

    }

    public void test_STARTED_does_emits_event_when_song_is_playing() {

    }

    public void test_CUEING_adds_playtime_to_model() {

    }

    public void test_CUEING_emits_NowPlayingEvent_when_song_passes_now_playing_point() {

    }

    public void test_CUEING_to_PLAYING_when_song_passes_now_playing_point() {

    }

    public void test_CUEING_to_CUEINGPAUSED_when_a_song_stops_before_now_playing_point() {

    }

    public void test_CUEING_to_CUEINGPAUSED_resets_song_model() {

    }

    public void test_CUEING_to_CUEINGPAUSED_emits_no_event() {

    }

    public void test_CUEINGPAUSED_to_STOPPED_when_song_is_stopping() {

    }

    public void test_CUEINGPAUSED_emits_ResetEvent_when_song_is_stopping() {

    }

    public void test_CUEINGPAUSED_to_CUEING_when_song_is_playing_forward() {

    }

    public void test_PLAYING_adds_playtime_to_model() {

    }

    public void test_PLAYING_to_STOPPED_when_song_is_stopping() {

    }

    public void test_PLAYING_emits_ResetEvent_when_song_is_stopping() {

    }

    public void test_PLAYING_to_SCROBBLING_when_song_is_past_scrobble_point() {

    }

    public void test_PLAYING_emits_TransitionEvent_when_song_is_past_scrobble_point() {

    }

    public void test_PLAYING_to_PLAYINGPAUSED_if_not_stopping_and_not_playing_forward() {

    }

    public void test_PLAYING_to_PLAYINGPAUSED_emits_no_event() {

    }

    public void test_PLAYING_emits_TransitionEvent_if_not_stopping_and_not_playing_forward() {

    }

    public void test_PLAYING_emit_no_event_if_playing_forward_before_scrobble_point() {

    }

    public void test_PLAYINGPAUSED_to_STOPPED_if_song_is_stopping() {

    }

    public void test_PLAYINGPAUSED_to_STOPPED_emits_ResetEvent() {

    }

    public void test_PLAYINGPAUSED_to_PLAYING_if_song_is_playing_forward() {

    }

    public void test_PLAYINGPAUSED_to_PLAYING_emits_TransitionEvent() {

    }

    public void test_PLAYINGPAUSED_emits_no_event_if_not_stopping_and_not_playing_forward() {

    }

    public void test_SCROBBLING_adds_playtime_to_model() {

    }

    public void test_SCROBBLING_to_STOPPED_when_song_is_stopping() {

    }

    public void test_SCROBBLING_emits_ScrobbleEvent_when_song_is_stopping() {

    }

    public void test_SCROBBLING_to_SCROBBLINGPAUSED_if_not_stopping_and_not_playing_forward() {

    }

    public void test_SCROBBLING_to_SCROBBLINGPAUSED_emits_TransitionEvent() {

    }

    public void test_SCROBBLING_emits_no_event_if_playing_forward() {

    }

    public void test_SCROBBLINGPAUSED_to_STOPPED_if_song_is_stopping() {

    }

    public void test_SCROBBLINGPAUSED_to_STOPPED_emits_ScrobbleEvent() {

    }

    public void test_SCROBBLINGPAUSED_to_SCROBBLING_if_song_is_playing_forward() {

    }

    public void test_SCROBBLINGPAUSED_to_SCROBBLING_emits_TransitionEvent() {

    }

    public void test_SCROBBLINGPAUSED_emits_no_event_if_not_stopping_and_not_playing_forward() {

    }
}
