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

import am.xo.cdjscrobbler.Plugins.LastFmClientConfig;
import am.xo.cdjscrobbler.Plugins.TwitterClientConfig;

public class OrchestratorConfig {

    private CDJScrobblerConfig config;
    private boolean dmcaAccountantEnabled = true;
    private boolean twitterEnabled = true;
    private boolean lfmEnabled = true;
    private String version;

    public OrchestratorConfig(CDJScrobblerConfig config) {
        this.config = config;
    }

    public OrchestratorConfig setFromProperties(CDJScrobblerConfig c) {
        this.config = c;
        dmcaAccountantEnabled = Boolean.parseBoolean(c.getProperty("dmcaaccountant.onairwarning.enabled", "true"));
        lfmEnabled = Boolean.parseBoolean(c.getProperty("cdjscrobbler.enable.lastfm", "false"));
        twitterEnabled = Boolean.parseBoolean(c.getProperty("cdjscrobbler.enable.twitter", "false"));
        version = config.getProperty("cdjscrobbler.version", "");
        return this;
    }

    public boolean isDmcaAccountantEnabled() {
        return dmcaAccountantEnabled;
    }

    public void setDmcaAccountantEnabled(boolean dmcaAccounantEnabled) {
        this.dmcaAccountantEnabled = dmcaAccounantEnabled;
    }

    public boolean isTwitterEnabled() {
        return twitterEnabled;
    }

    public void setTwitterEnabled(boolean twitterEnabled) {
        this.twitterEnabled = twitterEnabled;
    }

    public boolean isLfmEnabled() {
        return lfmEnabled;
    }

    public void setLfmEnabled(boolean lfmEnabled) {
        this.lfmEnabled = lfmEnabled;
    }

    public LastFmClientConfig getLastFmClientConfig() {
        return new LastFmClientConfig(this.config);
    }

    public TwitterClientConfig getTwitterClientConfig() {
        return new TwitterClientConfig(this.config);
    }

    public String getVersion() {
        return version;
    }

}
