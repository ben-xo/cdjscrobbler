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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class OnAirWarning extends Thread {
    static final Logger logger = LoggerFactory.getLogger(OnAirWarning.class);

    static final HashSet<Integer> playersToWarn = new HashSet<>();
    static final Set<Integer> noPlayers = Collections.emptySet();
    static boolean hasChanged = false;

    public void setWarn(int cdj) {
        synchronized(playersToWarn) {
            playersToWarn.add(cdj);
            hasChanged = true;
        }
    }

    public void removeWarn(int cdj) {
        synchronized(playersToWarn) {
            playersToWarn.remove(cdj);
            hasChanged = true;
        }
    }

    @Override
    public void run()
    {
        Set<Integer> playersToWarnCopy = Collections.EMPTY_SET;
        try {
            while (true) {
                synchronized (playersToWarn) {
                    if(hasChanged) {
                        playersToWarnCopy = new HashSet<>(playersToWarn);
                    }
                }
                if (!playersToWarnCopy.isEmpty()) {
                    VirtualCdj.getInstance().sendOnAirCommand(playersToWarnCopy);
                    Thread.sleep(250);
                    VirtualCdj.getInstance().sendOnAirCommand(Collections.EMPTY_SET);
                }
                // don't accidentally put this inside the if() - 100% CPU awaits if you do
                Thread.sleep(250);
            }
        } catch(Exception e) {
            logger.warn("Interrupted");
        }
    }
}
