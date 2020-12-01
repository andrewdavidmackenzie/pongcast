package net.mackenzie.chromeinteractor;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.core.view.MenuItemCompat;
import androidx.mediarouter.app.MediaRouteActionProvider;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;
import java.util.List;

/**
 * Class for handling all interactions with a Chromecast for the purposes of playing a game that an android
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
    public enum CHROMECAST_EVENT {
        NO_WIFI,              // Wifi is needed to connect to a Chromecast
        NO_ROUTE_AVAILABLE,   // Cannot connect to a chromecast
        ROUTE_AVAILABLE,      // A chromecast route can now be selected
        CONNECTING,           // Trying to connect to the chromecast
        CONNECTION_SUSPENDED, // Was connected, got suspended, waiting to reconnect
        CONNECTED,            // Connected to chromecast but our receiver app not yet ready
        RECEIVER_READY,       // Connected to chromecast and our receiver is now ready
    }

    // CONSTANTS
    private static final String LOG_TAG = "ChromecastInteractor";

    // IMMUTABLES
    private final MediaRouteSelector mediaRouteSelector;
    private final String receiverAppId;
    private final String nameSpace;
    private final GameController gameController;
    private final MediaRouter mediaRouter;
    private final Activity activity;

    // INITIALIZED IMMUTABLES
    private final CastMessageCallbacks castMessageCallbacks = new CastMessageCallbacks();
    private final ConnectionCallbacks connectionCallbacks = new ConnectionCallbacks();
    private final MediaRouter.Callback mediaRouterCallback = new MyMediaRouterCallback();
    private final CastListener castListener = new CastListener();
    private final ConnectionFailedListener connectionFailedListener = new ConnectionFailedListener();

    // MUTABLES
    private boolean waitingForReconnect;
    private GoogleApiClient apiClient;

    private class WiFiChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(LOG_TAG, "Wifi state change");
            checkRouteStatus();
        }
    }

    /**
     * This class is responsible for interacting with the chromecast device, and sending and receiving messages.
     * <p/>
     * When messages are received it will parse them and then update the game accordingly.
     */
    public ChromecastInteractor(@NonNull final Activity a, @NonNull final String receiverId,
                                @NonNull final String ns, @NonNull final GameController gc) {
        activity = a;
        receiverAppId = receiverId;
        nameSpace = ns;
        gameController = gc;

        // Configure Cast device discovery
        mediaRouter = MediaRouter.getInstance(activity.getApplicationContext());

        mediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(
                CastMediaControlIntent.categoryForCast(receiverAppId)).build();

        gameController.setChromecastInteractor(this);

        // install the broadcast receiver to listen for wifi changes
        WiFiChangeReceiver wiFiChangeReceiver = new WiFiChangeReceiver();
        IntentFilter wifiChangeIntentFilter = new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED");
        activity.registerReceiver(wiFiChangeReceiver, wifiChangeIntentFilter);
    }

    /**
     * Called on start and on every resume
     * Check the wifi state, if on check there is a chromecast route.
     * <p/>
     * reinstall the callback for route changes
     */
    public void resume() {
        Log.i(LOG_TAG, "resume() called");

        checkRouteStatus();

        mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    /**
     * remove the callback for route changes
     */
    public void pause() {
        Log.i(LOG_TAG, "pause() called");
        // End media router discovery
        mediaRouter.removeCallback(mediaRouterCallback);
    }

    /**
     * Sets the selector for the chromecast device into an action in a Menu
     *
     * @param menu to add the action to
     */
    public void setMediaRouteSelector(final Menu menu) {
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        // Set the MediaRouteActionProvider selector for device discovery.
        mediaRouteActionProvider.setRouteSelector(mediaRouteSelector);
    }


    /**
     * Check if it is possible to find a chromecast
     * - no wifi
     * - wifi, but no chromecasts found
     * - wifi, and chromecast(s) found
     */
    private void checkRouteStatus() {
        Log.i(LOG_TAG, "Checking Route Status");

        ConnectivityManager connManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (wifi.isConnectedOrConnecting()) {
            List<MediaRouter.RouteInfo> routes = mediaRouter.getRoutes();

            if (routes.isEmpty()) {
                Log.i(LOG_TAG, "No Routes found ");
                gameController.chromecastEvent(CHROMECAST_EVENT.NO_ROUTE_AVAILABLE);
            } else {
                Log.i(LOG_TAG, "Routes found ");
                gameController.chromecastEvent(CHROMECAST_EVENT.ROUTE_AVAILABLE);
            }
        } else {
            Log.i(LOG_TAG, "WiFi is switched off");
            gameController.chromecastEvent(CHROMECAST_EVENT.NO_WIFI);
        }
    }

    /**
     * Callback for MediaRouter events
     */
    private class MyMediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
            Log.d(LOG_TAG, "Route Added: " + route);
            checkRouteStatus();
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
            Log.d(LOG_TAG, "Route Removed: " + route);
            checkRouteStatus();
        }

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            Log.d(LOG_TAG, "onRouteSelected");
            // connect to cast device via the selected route
            connect(CastDevice.getFromBundle(info.getExtras()));
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            Log.d(LOG_TAG, "onRouteUnselected: info=" + info);
            disconnect();
        }
    }

    /**
     * connect to Google Play services.
     * in the ConnectionCallbacks we will launch the chromecast receiver and create the channel to talk to it
     *
     * @param selectedDevice the chromecast selected by the user to connect to
     */
    private void connect(final CastDevice selectedDevice) {
        try {
            gameController.chromecastEvent(CHROMECAST_EVENT.CONNECTING);
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
            Log.e(LOG_TAG, "Failed to connect", e);
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

        checkRouteStatus();
        waitingForReconnect = false;
    }

    /**
     * Google Play services callbacks
     */
    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(LOG_TAG, "onConnected() called");

            if (apiClient == null) {
                // We got disconnected while this runnable was pending execution.
                Log.d(LOG_TAG, "We got disconnected while trying to connect");
                checkRouteStatus();
                return;
            }

            if (waitingForReconnect) {
                waitingForReconnect = false;
                gameController.chromecastEvent(CHROMECAST_EVENT.CONNECTING); // reconnecting

                // Check if the receiver app is still running
                if ((connectionHint != null) && connectionHint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
                    // It was running when we lost the connection, now that we have reconnected - see if still running
                    Log.d(LOG_TAG, "Receiver is no longer running - try restarting it");
                    launchReceiver();
                } else {
                    Log.d(LOG_TAG, "Receiver is running - setup the message channel to it");
                    createCastMessageChannel();
                }
            } else {
                gameController.chromecastEvent(CHROMECAST_EVENT.CONNECTED);

                Log.d(LOG_TAG, "New connection");
                launchReceiver();
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(LOG_TAG, "onConnectionSuspended");
            gameController.chromecastEvent(CHROMECAST_EVENT.CONNECTION_SUSPENDED);
            waitingForReconnect = true;
        }
    }

    /**
     * Google Play services callbacks
     */
    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult result) {
            Log.e(LOG_TAG, "onConnectionFailed ");
            // Make sure and remove all callbacks etc
            disconnect();
        }
    }

    /**
     * Try and start our receiver on the chromecast
     */
    private void launchReceiver() {
        try {
            Log.d(LOG_TAG, "Trying to launch receiver on chromecast");
            // receiver is not running - try and launch the receiver
            Cast.CastApi.launchApplication(apiClient, receiverAppId, false)
                    .setResultCallback(new CastReceiverLaunchCallback());
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to launch application", e);
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
                Log.d(LOG_TAG, "Receiver was successfully launched");
                createCastMessageChannel();
            } else {
                Log.e(LOG_TAG, "application Launch failed - retry");
                launchReceiver();
            }
        }
    }

    /**
     * Remove previously set callbacks on messages from the chromecast
     */
    private void removeCastCallbacks() {
        try {
            Cast.CastApi.removeMessageReceivedCallbacks(apiClient, castMessageCallbacks.getNamespace());
        } catch (IOException e) {
            Log.e(LOG_TAG, "Exception while removing callbacks", e);
        }
    }

    /**
     * Setup the callbacks that will be called when a message is received
     */
    private void setCastCallbacks() {
        try {
            Cast.CastApi.setMessageReceivedCallbacks(apiClient, castMessageCallbacks.getNamespace(), castMessageCallbacks);

            gameController.chromecastEvent(CHROMECAST_EVENT.RECEIVER_READY);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Exception while creating channel", e);
        }
    }

    /**
     * Listener for events from the Chromecast device
     * We only react to the application disconnected event
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
         * Receive message from the receiver app that is running on a Chromecast
         */
        @Override
        public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
            gameController.receiverMessage(message);
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
                            public void onResult(@NonNull Status result) {
                                if (!result.isSuccess()) {
                                    Log.e(LOG_TAG, "Sending message failed");
                                }
                            }
                        });
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception while sending message", e);
            }
        } else {
            Log.e(LOG_TAG, "No Cast Client API");
        }
    }
}
