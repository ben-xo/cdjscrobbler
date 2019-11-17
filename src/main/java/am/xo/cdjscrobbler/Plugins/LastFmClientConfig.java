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

import am.xo.cdjscrobbler.CDJScrobblerConfig;

import java.io.IOException;

public class LastFmClientConfig {

    private CDJScrobblerConfig config;

    public LastFmClientConfig(CDJScrobblerConfig config) {
        this.config = config;
    }

    public String getApiKey() {
        return config.getProperty("lastfm.api.key", "");
    }

    public String getApiSecret() {
        return config.getProperty("lastfm.api.secret", "");
    }

    public String getUserAgent() {
        return config.getProperty("cdjscrobbler.useragent", "CDJ Scrobbler");
    }

    public String getApiSk() {
        return config.getProperty("lastfm.api.sk", "");
    }

    public void setApiSk(String apiSk) {
        config.setProperty("lastfm.api.sk", apiSk);
    }

    public void assertConfigured() throws IOException {
        if (getApiKey().isEmpty() || getApiSecret().isEmpty()) {
            String msg = "You need to put a Last.fm API key and API secret into your config. https://www.last.fm/api";
            throw new IOException(msg);
        }
    }

    public void save() throws IOException {
        config.save();
    }
}
