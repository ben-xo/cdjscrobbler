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

import am.xo.cdjscrobbler.SongEvents.NewSongLoadedEvent;
import am.xo.cdjscrobbler.SongEvents.NowPlayingEvent;
import am.xo.cdjscrobbler.SongEvents.ResetEvent;
import am.xo.cdjscrobbler.SongEvents.ScrobbleEvent;
import am.xo.cdjscrobbler.SongEvents.TransitionEvent;
import org.deepsymmetry.beatlink.CdjStatus;
import org.deepsymmetry.beatlink.DeviceUpdate;
import org.deepsymmetry.beatlink.DeviceUpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * An instance of UpdateListener is attached to beat-link's VirtualCdj, to receive updates from the CDJs on the network.
 * As they are received, they are passed to instances of SongModel which model the current playing state of the CDJ.
 *
 * Events from the SongModels are then put onto a thread-safe queue, for handling in another thread.
 */
public class UpdateListener implements DeviceUpdateListener, SongEventVisitor {

    final Logger logger = LoggerFactory.getLogger(UpdateListener.class);

    private static final int NUM_CDJS = 16;

    protected LinkedBlockingQueue<SongEvent> songEventQueue;

    /**
     * 1-based array of SongModels.
     */
    protected SongModel[] models;

    public UpdateListener(LinkedBlockingQueue<SongEvent> q) {
        songEventQueue = q;
        models = new SongModel[NUM_CDJS+1];
        for (int i = 1; i <= NUM_CDJS; i++) {
            models[i] = new SongModel(i);
        }
    }

    /**
     * This method is invoked by the beat-link VirtualCdj, on the status receiver thread. We generate events
     * and then put them onto a queue to be processed by the main thread.
     *
     * @param deviceUpdate
     */
    @Override
    public void received(DeviceUpdate deviceUpdate) {

        if(deviceUpdate instanceof CdjStatus) {
            CdjStatus s = (CdjStatus) deviceUpdate;

//            logger.debug(deviceUpdate.toString());
//            logger.debug(((CdjStatus) deviceUpdate).getPlayState1().name());
//            logger.debug(((CdjStatus) deviceUpdate).getPlayState2().name());
//            logger.debug(Integer.toString(((CdjStatus) deviceUpdate).getPitch(1)));
//            logger.debug(Integer.toString(((CdjStatus) deviceUpdate).getPitch(2)));
//            logger.debug(Integer.toString(((CdjStatus) deviceUpdate).getPitch(3)));
//            logger.debug(Integer.toString(((CdjStatus) deviceUpdate).getPitch(4)));

            // CDJs are usually numbered 1-4, but I believe can go higher sometimes
            int deviceNumber = s.getDeviceNumber();
            if(deviceNumber <= NUM_CDJS) {

                // Apply all updates to the model.
                ArrayList<SongEvent> el = models[deviceNumber].update(s);

                el.forEach((e) -> {
                    try {
                        logger.info("Device " + deviceNumber + " sending event " + e);
                        songEventQueue.put(e);

                        // Visitor pattern.
                        // Some event types re-initialise the model (see visit() below).
                        e.accept(this);

                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                });
            }
        }
    }

    @Override
    public void visit(NewSongLoadedEvent event) {
        // noop
    }

    @Override
    public void visit(NowPlayingEvent event) {
        // noop
    }

    @Override
    public void visit(ScrobbleEvent event) {
        int deviceNumber = event.cdjStatus.getDeviceNumber();
        models[deviceNumber] = new SongModel(deviceNumber);
    }

    @Override
    public void visit(ResetEvent event) {
        int deviceNumber = event.cdjStatus.getDeviceNumber();
        models[deviceNumber] = new SongModel(deviceNumber);
    }

    @Override
    public void visit(TransitionEvent event) {
        // noop
    }
}
