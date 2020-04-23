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

package am.xo.cdjscrobbler.Plugins;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Session;
import org.slf4j.helpers.MessageFormatter;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class LastFmGuiClient extends LastFmClient {

    public LastFmGuiClient(LastFmClientConfig config) {
        super(config);
    }

    @Override
    public Session authorize(String apiKey, String apiSecret) {
        String token = Authenticator.getToken(apiKey);

        Session session;
        
        String authUrlTemplate = "https://www.last.fm/api/auth/?api_key={}&token={}";
        String authUrl = MessageFormatter.format(authUrlTemplate, apiKey, token).getMessage();
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(authUrl));
            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
            }
        }

        int waitLoopCount = 0;
        while(waitLoopCount < 300) {
            // repeated in the loop because otherwise lastfm-java's logging scrolls it out of view.

            try {
                logger.info("❓ Waiting for authorization…");
                Thread.sleep(5000);
            } catch(InterruptedException e) {
                // so what.
            }

            session = Authenticator.getSession(token, apiKey, apiSecret);
            if(session != null) {
                logger.info("✅ authorized to scrobble to https://www.last.fm/user/{}", session.getUsername());
                return session;
            }

            waitLoopCount++;
        }
        throw new RuntimeException("Gave up waiting for you to authorize to Last.fm.");    }
}
