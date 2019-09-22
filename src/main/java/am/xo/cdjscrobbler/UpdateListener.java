package am.xo.cdjscrobbler;
import org.deepsymmetry.beatlink.*;

import java.util.concurrent.LinkedBlockingQueue;

public class UpdateListener implements DeviceUpdateListener {

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
                SongEvent e = model[deviceNumber-1].update(s);
                if(e != null) {
                    try {
                        songEventQueue.put(e);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}
