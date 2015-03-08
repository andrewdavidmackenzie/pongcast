package net.mackenzie_serres.chromecast;

import android.support.annotation.NonNull;

/**
 * Interface definition that GameControllers must implement in order to be passed events and messages from
 * ChromecastInteractor
 * <p/>
 * User: andrew
 * Date: 25/01/15
 * Time: 19:46
 * <p/>
 * Copyright Andrew Mackenzie, 2013
 */
public interface GameController {
    public void setChromecastInteractor(@NonNull final ChromecastInteractor chromecastInteractor);

    public void event(@NonNull final ChromecastInteractor.CHROMECAST_EVENT event);

    public void message(@NonNull final String message);
}
