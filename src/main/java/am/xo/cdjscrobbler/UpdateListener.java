package am.xo.cdjscrobbler;
import org.deepsymmetry.beatlink.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class UpdateListener implements DeviceUpdateListener {

    final Logger logger = LoggerFactory.getLogger(Application.class);

    private static final int NUM_CDJS = 4;

    protected LinkedBlockingQueue<SongEvent> songEventQueue;
    protected SongModel[] model;

    public UpdateListener(LinkedBlockingQueue<SongEvent> q) {
        this.songEventQueue = q;
        model = new SongModel[NUM_CDJS];
        for (int i = 0; i < NUM_CDJS; i++) {
            model[i] = new SongModel(i);
        }
    }

    @Override
    public void received(DeviceUpdate deviceUpdate) {
        // TODO: update the relevant SongModel
        if(deviceUpdate instanceof CdjStatus) {
            CdjStatus s = (CdjStatus) deviceUpdate;

            // CDJs are usually numbered 1-4
            int deviceNumber = s.getDeviceNumber();
            if(deviceNumber <= NUM_CDJS) {
                ArrayList<SongEvent> el = model[deviceNumber-1].update(s);
                el.forEach((e) -> {
                    try {
                        // we only want to put now playing and scrobble events here…
                        logger.info("Device " + deviceNumber + " event " + e.getClass().toString());
                        songEventQueue.put(e);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                });
            }
        }
    }
}
