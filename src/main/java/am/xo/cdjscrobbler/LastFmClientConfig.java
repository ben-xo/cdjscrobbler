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

import java.io.IOException;
import java.util.Properties;


public class LastFmClientConfig {

    private String apiKey    = "";
    private String apiSecret = "";
    private String apiSk     = "";
    private String userAgent = "";

    public LastFmClientConfig(Properties config) {
        apiKey    = config.getProperty("lastfm.api.key", "");
        apiSecret = config.getProperty("lastfm.api.secret", "");
        apiSk     = config.getProperty("lastfm.api.sk", "");
        userAgent = config.getProperty("cdjscrobbler.useragent", "CDJ Scrobbler");
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getApiSk() {
        return apiSk;
    }

    public void setApiSk(String apiSk) {
        this.apiSk = apiSk;
    }

    public void assertConfigured() throws IOException {
        if (apiKey.isEmpty() || apiSecret.isEmpty()) {
            String msg = "You need to put a Last.fm API key and API secret into your config. https://www.last.fm/api";
            throw new IOException(msg);
        }
    }
}
