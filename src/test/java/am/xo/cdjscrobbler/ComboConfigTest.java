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

import junit.framework.TestCase;

import java.util.Properties;

public class ComboConfigTest extends TestCase {

    public void test_getProperty_works_for_one_config() {
        ComboConfig c = new ComboConfig();
        Properties p1 = new Properties();

        p1.setProperty("test", "1");

        c.add(p1);

        assertEquals(c.getProperty("test"), "1");
        assertEquals(c.getProperty("test", "default"), "1");
    }

    public void test_getProperty_checks_sequentially() {
        ComboConfig c = new ComboConfig();
        Properties p1 = new Properties();
        Properties p2 = new Properties();

        p1.setProperty("test", "1");
        p2.setProperty("test", "2");

        c.add(p1);
        c.add(p2);

        assertEquals(c.getProperty("test"), "2");
        assertEquals(c.getProperty("test", "default"), "2");
    }

    public void test_getProperty_ignores_unset() {
        ComboConfig c = new ComboConfig();
        Properties p1 = new Properties();
        Properties p2 = new Properties();

        p1.setProperty("test", "1");

        c.add(p1);
        c.add(p2);

        assertEquals(c.getProperty("test"), "1");
        assertEquals(c.getProperty("test", "default"), "1");
    }

    public void test_getProperty_ignores_empty() {
        ComboConfig c = new ComboConfig();
        Properties p1 = new Properties();
        Properties p2 = new Properties();

        p1.setProperty("test", "1");
        p2.setProperty("test", "");

        c.add(p1);
        c.add(p2);

        assertEquals(c.getProperty("test"), "1");
        assertEquals(c.getProperty("test", "default"), "1");
    }

    public void test_getProperty_returns_default() {
        ComboConfig c = new ComboConfig();
        Properties p1 = new Properties();
        Properties p2 = new Properties();

        p1.setProperty("test", "1");
        p2.setProperty("test", "2");

        c.add(p1);
        c.add(p2);

        assertEquals(c.getProperty("test2", "default"), "default");
    }

    public void test_setProperty_updates_most_recently_added() {
        ComboConfig c = new ComboConfig();
        Properties p1 = new Properties();
        Properties p2 = new Properties();


        p1.setProperty("test", "1");
        p2.setProperty("test", "2");

        c.add(p1);
        c.add(p2);

        c.setProperty("test", "3");

        assertEquals(c.getProperty("test"), "3");
        assertEquals(p1.getProperty("test"), "1");
        assertEquals(p2.getProperty("test"), "3");
    }

    public void test_setProperty_adds_to_most_recently_added() {
        ComboConfig c = new ComboConfig();
        Properties p1 = new Properties();
        Properties p2 = new Properties();


        p1.setProperty("test", "1");
        p2.setProperty("test", "2");

        c.add(p1);
        c.add(p2);

        c.setProperty("other", "other");

        assertEquals(c.getProperty("other"), "other");
        assertNull(p1.getProperty("other"));
        assertEquals(p2.getProperty("other"), "other");
    }

    public void test_setProperty_sets_in_first_config_that_has_property() {
        ComboConfig c = new ComboConfig();
        Properties p1 = new Properties();
        Properties p2 = new Properties();


        p1.setProperty("test", "1");
        p2.setProperty("other", "other");

        c.add(p1);
        c.add(p2);

        c.setProperty("test", "3");

        assertEquals(c.getProperty("test"), "3");
        assertEquals(p1.getProperty("test"), "3");
        assertNull(p2.getProperty("test"));
    }
}