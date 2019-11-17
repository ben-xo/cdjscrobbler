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

public class TwitterClientConfig {

    private CDJScrobblerConfig config;

    public TwitterClientConfig(CDJScrobblerConfig config) {
        this.config = config;
    }

    public String getUserAgent() {
        return config.getProperty("cdjscrobbler.useragent", "CDJ Scrobbler");
    }

    public String getOAuthConsumerKey() {
        return config.getProperty("twitter4j.oauth.consumerKey","");
    }

    public String getOAuthConsumerSecret() {
        return config.getProperty("twitter4j.oauth.consumerSecret", "");
    }

    public String getOAuthAccessToken() {
        return config.getProperty("twitter4j.oauth.accessToken","");
    }

    public String getOAuthAccessTokenSecret() {
        return config.getProperty("twitter4j.oauth.accessTokenSecret", "");
    }

    public String getTweetTemplate() {
        return config.getProperty("cdjscrobbler.tweet.template", "Now Playing: {}");
    }

    public void assertConfigured() throws IOException {
        if (getOAuthConsumerKey().isEmpty() || getOAuthConsumerSecret().isEmpty()) {
            String msg = "You need to put a Twitter OAuth key and secret into your config. https://developer.twitter.com";
            throw new IOException(msg);
        }
    }

    public void setOauthAccessToken(String oauthAccessToken) {
        config.setProperty("twitter4j.oauth.accessToken", oauthAccessToken);
    }

    public void setOauthAccessTokenSecret(String oauthAccessTokenSecret) {
        config.setProperty("twitter4j.oauth.accessTokenSecret", oauthAccessTokenSecret);
    }

    public void save() throws IOException {
        config.save();
    }
}
