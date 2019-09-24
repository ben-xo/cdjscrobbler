package am.xo.cdjscrobbler;
import org.deepsymmetry.beatlink.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class UpdateListener implements DeviceUpdateListener {

    final Logger logger = LoggerFactory.getLogger(UpdateListener.class);

    private static final int NUM_CDJS = 16;

    protected LinkedBlockingQueue<SongEvent> songEventQueue;

    /**
     * 1-based array of SongModels.
     */
    protected SongModel[] models;

    public UpdateListener(LinkedBlockingQueue<SongEvent> q) {
        songEventQueue = q;
        models = new SongModel[NUM_CDJS+1];
        for (int i = 1; i <= NUM_CDJS; i++) {
            models[i] = new SongModel(i);
        }
    }

    @Override
    public void received(DeviceUpdate deviceUpdate) {
        // TODO: update the relevant SongModel
        if(deviceUpdate instanceof CdjStatus) {
            CdjStatus s = (CdjStatus) deviceUpdate;

            // CDJs are usually numbered 1-4, but I believe can go higher sometimes
            int deviceNumber = s.getDeviceNumber();
            if(deviceNumber <= NUM_CDJS) {

                // Apply all updates to the model.
                ArrayList<SongEvent> el = models[deviceNumber].update(s);

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
