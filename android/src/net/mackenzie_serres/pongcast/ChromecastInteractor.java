package net.mackenzie_serres.pongcast;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import net.mackenzie_serres.pongcast.GameController.GAME_EVENT;

import java.io.IOException;

/**
 * User: andrew
 * Date: 11/01/15
 * Time: 02:15
 * <p/>
 * Copyright Andrew Mackenzie, 2013
 */
public class ChromecastInteractor {
    private static final String TAG = ChromecastInteractor.class.getSimpleName();

    private static final String PADDLE_YES_PREFIX = "PADDLE YES";
    private static final String START_GAME_MESSAGE = "StartPlay";
    private static final String PAUSE_PLAY_MESSAGE = "PausePlay";
    private static final String PADDLE_UP_MESSAGE = "MoveUp";
    private static final String PADDLE_DOWN_MESSAGE = "MoveDown";

    private boolean mWaitingForReconnect;
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;
    private CastDevice mSelectedDevice;
    private GoogleApiClient mApiClient;
    private CastMessageCallbacks mPongcastCallbacks;
    private ConnectionCallbacks mConnectionCallbacks;
    private CastListener mCastListener;
    private ConnectionFailedListener mConnectionFailedListener;
    private static Activity activity;
    private GameController gameController;

    /**
     * This class is responsible for interacting with the chromecast device, and sending and receiving messages.
     * <p/>
     * When messages are received it will parse them and then update the game accordingly.
     *
     * @param activity       - the Activity running the Game
     * @param gameController - the controller that is managing the game logic
     */
    public ChromecastInteractor(Activity activity, GameController gameController) {
        this.activity = activity;
        this.gameController = gameController;

        // Configure Cast device discovery
        mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(
                CastMediaControlIntent.categoryForCast(activity.getResources()
                        .getString(R.string.app_id))).build();

        // Only create the callback once, on creation
        mMediaRouterCallback = new MyMediaRouterCallback();

        mPongcastCallbacks = new CastMessageCallbacks();
        mConnectionCallbacks = new ConnectionCallbacks();
        mCastListener = new CastListener();
        mConnectionFailedListener = new ConnectionFailedListener();

        gameController.setChromecastInteractor(this);
    }

    /**
     * Sets the selector for the chromecast device into an action in a Menu
     *
     * @param menu to add the action to
     */
    public void setMediaRouteSelector(Menu menu) {
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        // Set the MediaRouteActionProvider selector for device discovery.
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
    }

    /**
     * Request the chromecast that we pause play
     */
    public void pausePlay() {
        sendMessage(PAUSE_PLAY_MESSAGE);
    }

    /**
     * Pause communications and event handling with the chromecast
     */
    public void pause() {
        // TODO should remove callbacks here too?
        // They are added back in resume()!

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

    /**
     * Ask the chromecast to start the game
     */
    public void startGame() {
        sendMessage(START_GAME_MESSAGE);
    }

    /**
     * Ask the chromecast to move this players paddle up
     */
    public void paddleUp() {
        sendMessage(PADDLE_UP_MESSAGE);
    }

    /**
     * Ask the chromecast to move this players paddle down
     */
    public void paddleDown() {
        sendMessage(PADDLE_DOWN_MESSAGE);
    }

    /*************************************  MEDIA ROUTER RELATED METHODS  *************************************/
    /**
     * Callback for MediaRouter events
     */
    private class MyMediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            Log.d(TAG, "onRouteSelected");
            gameController.event(GAME_EVENT.CONNECTED);

            // Handle the user route selection.
            mSelectedDevice = CastDevice.getFromBundle(info.getExtras());

            connect();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            Log.d(TAG, "onRouteUnselected: info=" + info);
            disconnect();
            mSelectedDevice = null;
        }
    }

    /********************************  GOOGLE PLAY SERVICES RELATED METHODS  ********************************/
    /**
     * connect to Google Play services.
     * in the ConnectionCallbacks we will launch the chromecast receiver and create the channel to talk to it
     */
    private void connect() {
        try {
            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions.builder(mSelectedDevice, mCastListener);

            mApiClient = new GoogleApiClient.Builder(activity)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(mConnectionCallbacks)
                    .addOnConnectionFailedListener(mConnectionFailedListener)
                    .build();

            mApiClient.connect();
            // onConnected() callback should be called when successful
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

        gameController.event(GAME_EVENT.DISCONNECTED);

        mSelectedDevice = null;
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
                gameController.event(GAME_EVENT.DISCONNECTED);
                return;
            }

            if (mWaitingForReconnect) {
                mWaitingForReconnect = false;

                // Check if the receiver app is still running
                if ((connectionHint != null) && connectionHint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
                    Log.d(TAG, "App is no longer running");
                    gameController.event(GAME_EVENT.COURT_DESTROYED);
                    disconnect();
                } else {
                    gameController.event(GAME_EVENT.COURT_EXISTS);
                    createCastMessageChannel();
                }
            } else {
                // We are connected to the chromecast
                gameController.event(GAME_EVENT.CONNECTED);

                try {
                    // try and launch the receiver
                    Cast.CastApi.launchApplication(mApiClient, activity.getString(R.string.app_id), false)
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
            gameController.event(GAME_EVENT.DISCONNECTED);
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
                gameController.event(GAME_EVENT.COURT_EXISTS);

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
            gameController.event(GAME_EVENT.COURT_ACCEPTED_ME);
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
            return ChromecastInteractor.activity.getString(R.string.namespace);
        }

        /**
         * Receive message from the receiver app
         */
        @Override
        public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
            if (message.startsWith("PADDLE")) {
                parsePaddleMessage(message);
            } else if (message.startsWith("GAME")) {
                parseGameMessage(message);
            } else {
                Log.w(TAG, "Unknown message: " + message);
            }
        }
    }

    /**
     * Parse messages that affect the paddle and make appropriate changes
     *
     * @param message - from receiver to parse
     */
    private void parsePaddleMessage(String message) {
        Log.i(TAG, "Paddle Message: " + message);
        if (message.startsWith(PADDLE_YES_PREFIX)) {
            gameController.event(GAME_EVENT.GOT_PADDLE, message.split(PADDLE_YES_PREFIX)[1]);
        } else {
            gameController.event(GAME_EVENT.NO_PADDLE);
        }
    }

    /**
     * Parse a message from the Receiver about the game
     *
     * @param message - from the receiver
     */
    private void parseGameMessage(String message) {
        Log.i(TAG, "Game Message: " + message);
        if (message.startsWith("GAME WON") || message.startsWith("GAME LOST")) {
            gameController.event(GAME_EVENT.GAME_WON_LOST, message);
        } else if (message.equals("GAME STARTED")) {
            gameController.event(GAME_EVENT.GAME_STARTED);
        } else if (message.equals("GAME PAUSED")) {
            gameController.event(GAME_EVENT.GAME_PAUSED);
        }
    }

    /**
     * Send a message to the receiver
     *
     * @param message String to send to the cast device
     */
    public void sendMessage(String message) {
        if (mApiClient != null) {
            if (mPongcastCallbacks != null) {
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
                Log.e(TAG, "No Cast HelloWorld Channel");
            }
        } else {
            Log.e(TAG, "No Cast Client API");
        }
    }
}
