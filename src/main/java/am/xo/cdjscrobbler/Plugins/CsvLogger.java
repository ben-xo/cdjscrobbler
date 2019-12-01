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

import am.xo.cdjscrobbler.SongDetails;
import am.xo.cdjscrobbler.SongEventListeners.ScrobbleListener;
import am.xo.cdjscrobbler.SongEvents.ScrobbleEvent;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class CsvLogger implements ScrobbleListener {

    static final Logger logger = LoggerFactory.getLogger(CsvLogger.class);
    static final String FILENAME = "tracklist.csv";
    static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss zzz");

    // These match fields that Serato would export, making this CSV compatible with
    // https://github.com/ben-xo/prepare-podcast
    static final String[] HEADERS = { "name","artist","start time" };

    @Override
    public void scrobble(ScrobbleEvent event) {
        final SongDetails song = event.model.getSong();
        if(song != null) {
            try {
                FileWriter out = new FileWriter(FILENAME, true);
                File theFile = new File(FILENAME);
                CSVFormat csvFormat = (theFile.length() == 0)
                        ? CSVFormat.DEFAULT.withHeader(HEADERS)
                        : CSVFormat.DEFAULT;

                try (CSVPrinter printer = new CSVPrinter(out, csvFormat)) {
                    printer.printRecord(song.getTitle(), song.getArtist(),
                            convertToWallClock(event.model.getStartedAt()));
                    logger.info("Wrote {} to csv log", song);
                }
            } catch (IOException e) {
                logger.error("Could not write to CSV file {}", FILENAME);
            }
        }
    }

    private static String convertToWallClock(long epoch) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.systemDefault());
        return timeFormatter.format(zdt);
    }

}
