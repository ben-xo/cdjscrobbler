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

public class TwitterClientConfig {

    private String oauthConsumerKey;
    private String oauthConsumerSecret;
    private String oauthAccessToken;
    private String oauthAccessTokenSecret;
    private String tweetTemplate;
    private String userAgent;

    public TwitterClientConfig(Properties config) {
        oauthConsumerKey       = config.getProperty("twitter4j.oauth.consumerKey","");
        oauthConsumerSecret    = config.getProperty("twitter4j.oauth.consumerSecret", "");
        oauthAccessToken       = config.getProperty("twitter4j.oauth.accessToken","");
        oauthAccessTokenSecret = config.getProperty("twitter4j.oauth.accessTokenSecret", "");
        tweetTemplate          = config.getProperty("cdjscrobbler.tweet.template", "Now Playing: {}");
        userAgent              = config.getProperty("cdjscrobbler.useragent", "CDJ Scrobbler");
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getOAuthConsumerKey() {
        return oauthConsumerKey;
    }

    public String getOAuthConsumerSecret() {
        return oauthConsumerSecret;
    }

    public String getOAuthAccessToken() {
        return oauthAccessToken;
    }

    public String getOAuthAccessTokenSecret() {
        return oauthAccessTokenSecret;
    }

    public String getTweetTemplate() {
        return tweetTemplate;
    }

    public void assertConfigured() throws IOException {
        if (oauthConsumerKey.isEmpty() || oauthConsumerSecret.isEmpty()) {
            String msg = "You need to put a Twitter OAuth key and secret into your config. https://developer.twitter.com";
            throw new IOException(msg);
        }
    }

    public void setOauthAccessToken(String oauthAccessToken) {
        this.oauthAccessToken = oauthAccessToken;
    }

    public void setOauthAccessTokenSecret(String oauthAccessTokenSecret) {
        this.oauthAccessTokenSecret = oauthAccessTokenSecret;
    }
}
