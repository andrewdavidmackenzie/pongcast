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
        CONNECTED, DISCONNECTED,
        RECEIVER_RUNNING, RECEIVER_DEAD, RECEIVER_CONNECTED
    }

    // CONSTANTS
    private static final String TAG = ChromecastInteractor.class.getSimpleName();

    // IMMUTABLES
    private final Activity activity;
    private final String receiverAppId;
    private final String nameSpace;
    private final GameController gameController;
    private final MediaRouter mMediaRouter;
    private final MediaRouteSelector mMediaRouteSelector;

    // INITIALIZED IMMUTABLES
    private final CastMessageCallbacks mPongcastCallbacks = new CastMessageCallbacks();
    private final ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks();
    private final MediaRouter.Callback mMediaRouterCallback = new MyMediaRouterCallback();
    private final CastListener mCastListener = new CastListener();
    private final ConnectionFailedListener mConnectionFailedListener = new ConnectionFailedListener();

    // MUTABLES
    private boolean mWaitingForReconnect;
    private GoogleApiClient mApiClient;

    /**
     * This class is responsible for interacting with the chromecast device, and sending and receiving messages.
     * <p/>
     * When messages are received it will parse them and then update the game accordingly.
     *
     * @param activity       - the Activity running the Game
     * @param gameController - the controller that is managing the game logic
     */
    public ChromecastInteractor(Activity activity, final String receiverAppId, final String nameSpace,
                                MediaRouteSelector mediaRouteSelector, GameController gameController) {
        this.activity = activity;
        this.receiverAppId = receiverAppId;
        this.nameSpace = nameSpace;
        this.mMediaRouteSelector = mediaRouteSelector;
        this.gameController = gameController;

        // Configure Cast device discovery
        mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());

        gameController.setChromecastInteractor(this);
    }

    /**
     * Pause communications and event handling with the chromecast
     */
    public void pause() {
        // End media router discovery
        mMediaRouter.removeCallback(mMediaRouterCallback);
    }

    /**
     * Resume communications and event handling with the chromecast
     */
    public void resume() {
        // Start media router discovery
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
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
     * @param selectedDevice the chromecast selected by the user to connect to
     */
    private void connect(final CastDevice selectedDevice) {
        try {
            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions.builder(selectedDevice, mCastListener);

            mApiClient = new GoogleApiClient.Builder(activity)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(mConnectionCallbacks)
                    .addOnConnectionFailedListener(mConnectionFailedListener)
                    .build();

            mApiClient.connect();
            // the onConnected() callback should be called when the connect() call succeeds
        } catch (Exception e) {
            // TODO Add event for creation failed
            Log.e(TAG, "Failed to connect", e);
        }
    }

    /**
     * remove callbacks from chromecast
     * disconnect from Google Play services
     */
    public void disconnect() {
        if (mApiClient != null) {
            try {
                if (mPongcastCallbacks != null) {
                    Cast.CastApi.removeMessageReceivedCallbacks(mApiClient, mPongcastCallbacks.getNamespace());
                }
            } catch (IOException e) {
                Log.e(TAG, "Exception while removing channel", e);
            }

            if (mApiClient.isConnected()) {
                mApiClient.disconnect();
            }
            mApiClient = null;
        }

        gameController.event(CHROMECAST_EVENT.DISCONNECTED);

        mWaitingForReconnect = false;
    }

    /**
     * Google Play services callbacks
     */
    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "onConnected() called");

            if (mApiClient == null) {
                // We got disconnected while this runnable was pending execution.
                gameController.event(CHROMECAST_EVENT.DISCONNECTED);
                return;
            }

            // We are connected to the chromecast
            gameController.event(CHROMECAST_EVENT.CONNECTED);

            if (mWaitingForReconnect) {
                mWaitingForReconnect = false;

                // Check if the receiver app is still running
                if ((connectionHint != null) && connectionHint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
                    Log.d(TAG, "App is no longer running");
                    gameController.event(CHROMECAST_EVENT.RECEIVER_DEAD);
                    disconnect();
                } else {
                    Log.d(TAG, "App is running");
                    gameController.event(CHROMECAST_EVENT.RECEIVER_RUNNING);
                    createCastMessageChannel();
                }
            } else {
                try {
                    // try and launch the receiver
                    Cast.CastApi.launchApplication(mApiClient, receiverAppId, false)
                            .setResultCallback(new CastReceiverLaunchCallback());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch application", e);
                }
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "onConnectionSuspended");
            // TODO Maybe a connection lost event - as it can be recovered, and not the same as being totally lost
            gameController.event(CHROMECAST_EVENT.DISCONNECTED);
            mWaitingForReconnect = true;
        }
    }

    /**
     * Google Play services callbacks
     */
    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.e(TAG, "onConnectionFailed ");
            // TODO maybe separate disconnect() into its differen tlayers for GPS, Chromecast, etc?
            disconnect();

            // TODO can we do something to retry?
        }
    }

    /*************************************  CHROMECAST RELATED METHODS  *************************************/
    /**
     * This is the callback class that handles the result on attempt to start the receiver
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
            Status status = result.getStatus();
            if (status.isSuccess()) {
                gameController.event(CHROMECAST_EVENT.RECEIVER_RUNNING);

                createCastMessageChannel();
            } else {
                Log.e(TAG, "application Launch failed");
                disconnect();
            }
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
        try {
            Cast.CastApi.setMessageReceivedCallbacks(mApiClient, mPongcastCallbacks.getNamespace(), mPongcastCallbacks);

            // Got into the court!
            gameController.event(CHROMECAST_EVENT.RECEIVER_CONNECTED);
        } catch (IOException e) {
            // TODO what to do here when I can't get a channel and hence on court?
            // TODO delay and then try again? Put something on UI to ask user to try again?
            Log.e(TAG, "Exception while creating channel", e);
        }
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
        if (mApiClient != null) {
            try {
                Cast.CastApi.sendMessage(mApiClient, mPongcastCallbacks.getNamespace(), message)
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
