package am.xo.cdjscrobbler;

import org.deepsymmetry.beatlink.DeviceAnnouncement;
import org.deepsymmetry.beatlink.DeviceAnnouncementListener;
import org.deepsymmetry.beatlink.DeviceFinder;

import java.net.SocketException;

/**
 * Hello world!
 *
 */
public class App implements DeviceAnnouncementListener
{
    public static void main( String[] args ) throws SocketException
    {
        App theApp = new App();
        theApp.start(args);
    }

    public void start( String[] args ) throws SocketException
    {
        System.out.println( "Starting CDJ Scrobbler" );
        DeviceFinder.getInstance().start();
        DeviceFinder.getInstance().addDeviceAnnouncementListener(this);

        // TODO: create a scrobbling adaptor
        // TODO: create a state machine
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
