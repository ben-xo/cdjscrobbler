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

import am.xo.cdjscrobbler.SongEventListeners.NowPlayingListener;
import am.xo.cdjscrobbler.SongEvents.NowPlayingEvent;
import org.deepsymmetry.beatlink.data.AlbumArt;
import org.deepsymmetry.beatlink.data.ArtFinder;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;

public class ArtworkPopup implements NowPlayingListener {

    JFrame editorFrame = new JFrame("Image Demo");
    JLabel jLabel = new JLabel();

    public ArtworkPopup() throws Exception {
        editorFrame.getContentPane().add(jLabel, BorderLayout.CENTER);
        ArtFinder.getInstance().start();
    }

    @Override
    public void nowPlaying(NowPlayingEvent event) {
        SwingUtilities.invokeLater(() -> {
            AlbumArt art = ArtFinder.getInstance().getLatestArtFor(event.cdjStatus);
            if(art == null) {
                editorFrame.setVisible(false);
            } else {
                BufferedImage image = art.getImage();

                ImageIcon currentImage = new ImageIcon(image);
                jLabel.setIcon(currentImage);
                jLabel.setSize(jLabel.getWidth() * 3, jLabel.getHeight() * 3);

                editorFrame.pack();
                editorFrame.setLocationRelativeTo(null);
                editorFrame.setVisible(true);
            }
        });
    }
}
