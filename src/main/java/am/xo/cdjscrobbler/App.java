package am.xo.cdjscrobbler;

import org.deepsymmetry.beatlink.DeviceAnnouncement;
import org.deepsymmetry.beatlink.DeviceAnnouncementListener;
import org.deepsymmetry.beatlink.DeviceFinder;
import org.deepsymmetry.beatlink.data.MetadataFinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.net.SocketException;

/**
 * Hello world!
 *
 */
public class App implements DeviceAnnouncementListener
{
    final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main( String[] args ) throws Exception
    {
        App theApp = new App();
        theApp.start(args);
    }

    public void start( String[] args ) throws Exception
    {
        logger.info( "Starting CDJ Scrobbler" );
        DeviceFinder.getInstance().start();
        MetadataFinder.getInstance().start();
        DeviceFinder.getInstance().addDeviceAnnouncementListener(this);

        // TODO: create a scrobbling adaptor
        // TODO: create a state machine

        // TODO: model is playing / is not playing
        // TODO: model tempo influence on play time
        // TODO: model "more than half way" ? Adjustable? How much needs to be played to count?
    }

    public void stop() {
        // TODO: stop all state machines.
    }

    public void deviceFound(DeviceAnnouncement a) {
        // TODO: create a state machine for the device and start it
    }

    public void deviceLost(DeviceAnnouncement a) {
        // TODO: stop a state machine, if it exists
    }
}
