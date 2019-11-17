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

package am.xo.cdjscrobbler.Plugins.Helpers;

import org.deepsymmetry.beatlink.VirtualCdj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

public class OnAirWarning extends Thread {
    static final Logger logger = LoggerFactory.getLogger(OnAirWarning.class);

    Integer cdjToWarn = 0;

    public void setWarn(int cdj) {
        synchronized(cdjToWarn) {
            cdjToWarn = cdj;
        }
    }

    public void removeWarn() {
        synchronized(cdjToWarn) {
            cdjToWarn = 0;
        }
    }

    @Override
    public void run()
    {
        final HashSet<Integer> players = new HashSet<>();
        int cdj;
        try {
            while (true) {
                synchronized (cdjToWarn) {
                    cdj = cdjToWarn;
                }
                if(cdj > 0) {
                    players.add(cdj);
                    VirtualCdj.getInstance().sendOnAirCommand(players);
                    Thread.sleep(250);
                    players.remove(cdj);
                    VirtualCdj.getInstance().sendOnAirCommand(players);
                }
                Thread.sleep(250);
            }
        } catch(Exception e) {
            logger.warn("Interrupted");
        }
    }
}
