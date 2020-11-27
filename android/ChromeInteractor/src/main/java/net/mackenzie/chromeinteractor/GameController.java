package net.mackenzie.chromeinteractor;

import androidx.annotation.NonNull;

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
    void setChromecastInteractor(@NonNull final ChromecastInteractor chromecastInteractor);
    void chromecastEvent(@NonNull final ChromecastInteractor.CHROMECAST_EVENT event);
    void receiverMessage(@NonNull final String message);
}
