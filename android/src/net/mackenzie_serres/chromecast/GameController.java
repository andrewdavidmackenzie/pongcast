package net.mackenzie_serres.chromecast;

import net.mackenzie_serres.chromecast.ChromecastInteractor;

/**
 * Interface definition that GameControllers must implement in order to be passed events and messages from
 * ChromecastInteractor
 *
 * User: andrew
 * Date: 25/01/15
 * Time: 19:46
 * <p/>
 * Copyright Andrew Mackenzie, 2013
 */
public interface GameController {
    public void setChromecastInteractor(ChromecastInteractor chromecastInteractor);

    public void event(ChromecastInteractor.CHROMECAST_EVENT event);

    public void message(final String message);
}
