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

package am.xo.cdjscrobbler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class CDJScrobblerGui extends JFrame implements CDJScrobblerReadyListener {
    static final Logger logger = LoggerFactory.getLogger(CDJScrobblerGui.class);

    private final Orchestrator o;
    public CDJScrobblerGui(Orchestrator o) {
        this.o = o;
        initUI();
    }

    private JCheckBox lfm;
    private JCheckBox twitter;
    private JCheckBox tweetCoverArt;
    private JCheckBox dmcaWarning;
    private JTextArea readyLabel;

    private void initUI() {

        lfm = new JCheckBox();
        lfm.setSelected(o.isLfmEnabled());
        lfm.setAction(new LfmCheckBoxAction());

        twitter = new JCheckBox();
        twitter.setSelected(o.isTwitterEnabled());
        twitter.setAction(new TwitterCheckBoxAction());

        tweetCoverArt = new JCheckBox();
        tweetCoverArt.setSelected(o.getConfig().getTwitterClientConfig().getShouldAttachCoverArt());
        tweetCoverArt.setAction(new TwitterCoverArtCheckBoxAction());

        dmcaWarning = new JCheckBox();
        dmcaWarning.setSelected(o.isDmcaAccountantEnabled());
        dmcaWarning.setAction(new DmcaWarningCheckBoxAction());

        readyLabel = new JTextArea("CDJ Scrobbler starting…");

        // TODO
        // csv logger enable
        // csv logger filename
        // connect last.fm
        // connect twitter

        createLayout(lfm, twitter, tweetCoverArt, dmcaWarning, readyLabel);

        setTitle("CDJ Scrobbler");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }


    private void createLayout(JComponent... arg) {

        Container pane = getContentPane();
        GroupLayout gl = new GroupLayout(pane);
        pane.setLayout(gl);

        gl.setAutoCreateContainerGaps(true);
        gl.setAutoCreateGaps(true);

        gl.setHorizontalGroup(
                gl.createParallelGroup()
                        .addComponent(arg[0])
                        .addComponent(arg[1])
                        .addComponent(arg[2])
                        .addComponent(arg[3])
                        .addComponent(arg[4])
        );

        gl.setVerticalGroup(
                gl.createSequentialGroup()
                        .addGroup(gl.createParallelGroup()
                            .addComponent(arg[0]))
                        .addGroup(gl.createParallelGroup()
                                .addComponent(arg[1]))
                        .addGroup(gl.createParallelGroup()
                                .addComponent(arg[2]))
                        .addGroup(gl.createParallelGroup()
                                .addComponent(arg[3]))
                        .addGroup(gl.createParallelGroup()
                                .addComponent(arg[4]))
        );

        pack();
    }

    @Override
    public void cdjScrobblerReady() {
        readyLabel.setText("CDJ Scrobbler Ready");
    }

    private class LfmCheckBoxAction extends AbstractAction {

        public LfmCheckBoxAction() {
            super("Enable Last.fm Scrobbling");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            try {
                if (lfm.isSelected()) {
                    o.startLastFmClient();
                } else {
                    o.stopLastFmClient();
                }
            } catch (Exception ex) {
                logger.error("? {}", ex);
            }
        }
    }

    private class TwitterCheckBoxAction extends AbstractAction {

        public TwitterCheckBoxAction() {
            super("Enable Tweeting");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            try {
                if (twitter.isSelected()) {
                    o.startTwitterClient();
                } else {
                    o.stopTwitterClient();
                }
            } catch (Exception ex) {
                logger.error("… {}", ex);
            }
        }
    }

    private class TwitterCoverArtCheckBoxAction extends AbstractAction {

        public TwitterCoverArtCheckBoxAction() {
            super("Enable Tweeting with cover art");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            logger.info("Setting Twitter attach cover art to {}", tweetCoverArt.isSelected());
            o.getConfig().getTwitterClientConfig().setShouldAttachCoverArt(tweetCoverArt.isSelected());
        }
    }

    private class DmcaWarningCheckBoxAction extends AbstractAction {

        public DmcaWarningCheckBoxAction() {
            super("Enable Warning on CDJ when track would break DMCA streaming rules");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            try {
                if (dmcaWarning.isSelected()) {
                    o.startDmcaAccountant();
                } else {
                    o.stopDmcaAccountant();
                }
            } catch (Exception ex) {
                logger.error("? {}", ex);
            }
        }
    }
}
