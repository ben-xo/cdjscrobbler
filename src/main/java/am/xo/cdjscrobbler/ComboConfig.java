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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Class that encapsulates several config files, with "latest wins" semantics.
 *
 * This is used implement file-backed configuration with internal configuration as a fallback.
 */
public class ComboConfig extends Properties {

    private List<Properties> configs = new ArrayList<>();

    public ComboConfig() {}

    public void add(Properties p) {
        configs.add(p);
    }

    /**
     * Loads a config file and adds it to the FRONT of the list - on the assumption that the last loaded file
     * is the one most likely to contain overrides
     *
     * @param inStream
     * @throws IOException
     */
    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        Properties p = new Properties();
        p.load(inStream);
        configs.add(0, p);
    }

    /**
     * Saves the most recently loaded config file.
     *
     * @param out
     * @param comments
     * @throws IOException
     */
    @Override
    public void store(OutputStream out, String comments)
            throws IOException {
        configs.get(0).store(out, comments);
    }


    /**
     * Returns the first value found in the collection of loaded configs.
     * Ignores null or empty values.
     *
     * @param key
     * @param defaultValue
     * @return
     */
    @Override
    public String getProperty(String key, String defaultValue) {
        for(Properties p: configs) {
            String val = p.getProperty(key);
            if(val != null && !val.isEmpty()) {
                return val;
            }
        }
        return defaultValue;
    }

    @Override
    public String getProperty(String key) {
        return getProperty(key, null);
    }

    /**
     * Saves the value over the first-found instance of that key.
     *
     * @param key
     * @param value
     * @return
     */
    @Override
    public synchronized Object setProperty(String key, String value) {
        for(Properties p: configs) {
            String prevVal = p.getProperty(key);
            if(prevVal != null && !prevVal.isEmpty()) {
                return p.setProperty(key, value);
            }
        }

        // if we got here, no existing config has the element - so we add it to the first one.
        return configs.get(0).setProperty(key, value);
    }
}
