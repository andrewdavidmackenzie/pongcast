package net.mackenzie_serres.chromecast;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

/**
 * Class for handline all interactions with a Chromecast for the purposes of playing a game that an android
 * app connects to to play.
 * <p/>
 * User: andrew
 * Date: 11/01/15
 * Time: 02:15
 * <p/>
 * Copyright Andrew Mackenzie, 2013
 */
public class ChromecastInteractor {
    // ENUMS
    public static enum CHROMECAST_EVENT {
        CONNECTING,           // Trying to connect to the chromecast
        CONNECTION_SUSPENDED, // Was connected, got suspended, waiting to reconnect
        CONNECTED,            // Connected to chromecast but our receiver app not yet ready
        READY,                // Connected to chromecast and our receiver is now ready
        DISCONNECTED          // Disconnected from the chromecast
    }

    // CONSTANTS
    private static final String TAG = "ChromecastInteractor";

    // IMMUTABLES
    private final Activity activity;
    private final String receiverAppId;
    private final String nameSpace;
    private final GameController gameController;
    private final MediaRouter mediaRouter;
    private final MediaRouteSelector mediaRouteSelector;

    // INITIALIZED IMMUTABLES
    private final CastMessageCallbacks castMessageCallbacks = new CastMessageCallbacks();
    private final ConnectionCallbacks connectionCallbacks = new ConnectionCallbacks();
    private final MediaRouter.Callback mediaRouterCallback = new MyMediaRouterCallback();
    private final CastListener castListener = new CastListener();
    private final ConnectionFailedListener connectionFailedListener = new ConnectionFailedListener();

    // MUTABLES
    private boolean waitingForReconnect;
    private GoogleApiClient apiClient;

    /**
     * This class is responsible for interacting with the chromecast device, and sending and receiving messages.
     * <p/>
     * When messages are received it will parse them and then update the game accordingly.
     *
     * @param activity       - the Activity running the Game
     * @param gameController - the controller that is managing the game logic
     */
    public ChromecastInteractor(final Activity activity, final String receiverAppId, final String nameSpace,
                                final MediaRouteSelector mediaRouteSelector, final GameController gameController) {
        this.activity = activity;
        this.receiverAppId = receiverAppId;
        this.nameSpace = nameSpace;
        this.mediaRouteSelector = mediaRouteSelector;
        this.gameController = gameController;

        // Configure Cast device discovery
        mediaRouter = MediaRouter.getInstance(activity.getApplicationContext());

        gameController.setChromecastInteractor(this);
    }

    /**
     * Pause communications and event handling with the chromecast
     */
    public void pause() {
        // End media router discovery
        mediaRouter.removeCallback(mediaRouterCallback);
    }

    /**
     * Resume communications and event handling with the chromecast
     */
    public void resume() {
        // Start media router discovery
        mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    /*************************************  MEDIA ROUTER RELATED METHODS  *************************************/
    /**
     * Callback for MediaRouter events
     */
    private class MyMediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            Log.d(TAG, "onRouteSelected");
            // connect to cast device via the selected route
            connect(CastDevice.getFromBundle(info.getExtras()));
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            Log.d(TAG, "onRouteUnselected: info=" + info);
            disconnect();
        }
    }

    /********************************  GOOGLE PLAY SERVICES RELATED METHODS  ********************************/
    /**
     * connect to Google Play services.
     * in the ConnectionCallbacks we will launch the chromecast receiver and create the channel to talk to it
     *
     * @param selectedDevice the chromecast selected by the user to connect to
     */
    private void connect(final CastDevice selectedDevice) {
        try {
            gameController.event(CHROMECAST_EVENT.CONNECTING);
            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions.builder(selectedDevice, castListener);

            apiClient = new GoogleApiClient.Builder(activity)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(connectionCallbacks)
                    .addOnConnectionFailedListener(connectionFailedListener)
                    .build();

            apiClient.connect();
            // the onConnected() callback will be called when the connect() call succeeds
            // or onConnectionFailed() if fails.
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect", e);
        }
    }

    /**
     * remove callbacks from chromecast
     * disconnect from Google Play services
     */
    public void disconnect() {
        if (apiClient != null) {
            removeCastCallbacks();

            if (apiClient.isConnected()) {
                apiClient.disconnect();
            }
            apiClient = null;
        }

        gameController.event(CHROMECAST_EVENT.DISCONNECTED);
        waitingForReconnect = false;
    }

    /**
     * Google Play services callbacks
     */
    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "onConnected() called");

            if (apiClient == null) {
                // We got disconnected while this runnable was pending execution.
                Log.d(TAG, "We got disconnected while trying to connect");
                gameController.event(CHROMECAST_EVENT.DISCONNECTED);
                return;
            }

            if (waitingForReconnect) {
                waitingForReconnect = false;
                gameController.event(CHROMECAST_EVENT.CONNECTING); // reconnecting

                // Check if the receiver app is still running
                if ((connectionHint != null) && connectionHint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
                    // It was running when we lost the connection, now that we have reconnected - see if still running
                    Log.d(TAG, "Receiver is no longer running - try restarting it");
                    launchReceiver();
                } else {
                    Log.d(TAG, "Receiver is running - setup the message channel to it");
                    createCastMessageChannel();
                }
            } else {
                gameController.event(CHROMECAST_EVENT.CONNECTED);

                Log.d(TAG, "New connection");
                launchReceiver();
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "onConnectionSuspended");
            gameController.event(CHROMECAST_EVENT.CONNECTION_SUSPENDED);
            waitingForReconnect = true;
        }
    }

    /**
     * Google Play services callbacks
     */
    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.e(TAG, "onConnectionFailed ");
            // Make sure and remove all callbacks etc
            disconnect();
        }
    }

    /*************************************  CHROMECAST RELATED METHODS  *************************************/
    /**
     * Try and start our receiver on the chromecast
     */
    private void launchReceiver() {
        try {
            Log.d(TAG, "Trying to launch receiver on chromecast");
            // receiver is not running - try and launch the receiver
            Cast.CastApi.launchApplication(apiClient, receiverAppId, false)
                    .setResultCallback(new CastReceiverLaunchCallback());
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch application", e);
        }
    }

    /**
     * This is the callback class that handles the result on attempt to start the receiver from scratch
     * <p/>
     * ApplicationConnectionResult can be used to:
     * Status getStatus();
     * ApplicationMetadata getApplicationMetadata();
     * String getApplicationStatus();
     * String getSessionId();
     * boolean getWasLaunched();
     */
    private class CastReceiverLaunchCallback implements ResultCallback<Cast.ApplicationConnectionResult> {
        @Override
        public void onResult(Cast.ApplicationConnectionResult result) {
            if (result.getStatus().isSuccess()) {
                Log.d(TAG, "Receiver was successfully launched");
                createCastMessageChannel();
            } else {
                Log.e(TAG, "application Launch failed - retry");
                launchReceiver();
            }
        }
    }

    /**
     * Remove previously set callbacks on messages from the chromecast
     */
    private void removeCastCallbacks() {
        try {
            if (castMessageCallbacks != null) {
                Cast.CastApi.removeMessageReceivedCallbacks(apiClient, castMessageCallbacks.getNamespace());
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception while removing callbacks", e);
        }
    }

    /**
     * Setup the callbacks that will be called when a message is received
     */
    private void setCastCallbacks() {
        try {
            Cast.CastApi.setMessageReceivedCallbacks(apiClient, castMessageCallbacks.getNamespace(), castMessageCallbacks);

            gameController.event(CHROMECAST_EVENT.READY);
        } catch (IOException e) {
            Log.e(TAG, "Exception while creating channel", e);
        }
    }

    /**
     * Listener for events from the Chromecast
     * We only react to the application lost event
     */
    private class CastListener extends Cast.Listener {
        @Override
        public void onApplicationDisconnected(int errorCode) {
            disconnect();
        }
    }

    /**
     * Create a chromecast message channel between this app and the receiver to pass message about game events
     * and requests back and fore.
     */
    private void createCastMessageChannel() {
        setCastCallbacks();
    }

    /**
     * Callback receive messages from the chromecast
     */
    private class CastMessageCallbacks implements Cast.MessageReceivedCallback {
        /**
         * @return custom namespace
         */
        public String getNamespace() {
            return nameSpace;
        }

        /**
         * Receive message from the receiver app
         */
        @Override
        public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
            gameController.message(message);
        }
    }

    /**
     * Send a message to the chromecast receiver
     *
     * @param message String to send to the cast device
     */
    public void sendMessage(String message) {
        if (apiClient != null) {
            try {
                Cast.CastApi.sendMessage(apiClient, castMessageCallbacks.getNamespace(), message)
                        .setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(Status result) {
                                if (!result.isSuccess()) {
                                    Log.e(TAG, "Sending message failed");
                                }
                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "Exception while sending message", e);
            }
        } else {
            Log.e(TAG, "No Cast Client API");
        }
    }
}
