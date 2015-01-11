package net.mackenzie_serres.pongcast;

import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.Cast.MessageReceivedCallback;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

/**
 * Main activity to send messages to the receiver.
 */
public class MainActivity extends ActionBarActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private MediaRouter.Callback mMediaRouterCallback;
    private CastDevice mSelectedDevice;
    private GoogleApiClient mApiClient;
    private PongcastCallbacks mPongcastCallbacks;
    private boolean mWaitingForReconnect;
    private Button startGameButton;
    private View paddleControls;
    private Button upButton, downButton;

    private static final String START_GAME_MESSAGE = "StartPlay";
    private static final String PAUSE_PLAY_MESSAGE = "PausePlay";
    private static final String PADDLE_UP_MESSAGE = "MoveUp";
    private static final String PADDLE_DOWN_MESSAGE = "MoveDown";

    private static enum COURT_STATE {
        UNKNOWN, CREATING, READY, I_AM_IN
    }

    // When in court, you may or may not have a paddle
    private static enum PADDLE_STATE {
        NO_PADDLE, HAS_PADDLE
    }

    private String paddle = "";

    // When in court, then you can see the state of play
    private static enum GAME_STATE {
        NO_GAME, GAME_READY_TO_START, GAME_IN_PLAY, GAME_PAUSED, GAME_ENDED
    }

    // Set initial states
    private COURT_STATE courtState;
    private PADDLE_STATE paddleState = PADDLE_STATE.NO_PADDLE;
    private GAME_STATE gameState = GAME_STATE.NO_GAME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));

        // When the user clicks on the button, send a message to start the game
        startGameButton = (Button) findViewById(R.id.playButton);
        startGameButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(START_GAME_MESSAGE);
            }
        });

        paddleControls = findViewById(R.id.paddleControl);
        upButton = (Button) paddleControls.findViewById(R.id.upButton);
        upButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(PADDLE_UP_MESSAGE);
            }
        });

        downButton = (Button) paddleControls.findViewById(R.id.downButton);
        downButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(PADDLE_DOWN_MESSAGE);
            }
        });

        setCourtState(COURT_STATE.UNKNOWN);

        // Configure Cast device discovery
        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
        mMediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(
                CastMediaControlIntent.categoryForCast(getResources()
                        .getString(R.string.app_id))).build();

        // Only create the callback once, on creation
        mMediaRouterCallback = new MyMediaRouterCallback();
    }

    // TODO Avoid Pause/Resume on first rotation
    @Override
    protected void onResume() {
        super.onResume();
        // Start media router discovery
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    public void onDestroy() {
        teardown();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {

    }

    @Override
    protected void onPause() {
        // If I am a player, and game under way then pause the game
        if ((paddleState == PADDLE_STATE.HAS_PADDLE) && (gameState == GAME_STATE.GAME_IN_PLAY)) {
            sendMessage(PAUSE_PLAY_MESSAGE);
        }

        if (isFinishing()) {
            // End media router discovery
            mMediaRouter.removeCallback(mMediaRouterCallback);
        }
        super.onPause();
    }

    /**
     * Add the MediaRoute ("Chromecast") button to the ActionBar at the top of the app
     *
     * @param menu - menu to add the menu items to
     * @return true if added an item
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        // Set the MediaRouteActionProvider selector for device discovery.
        mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
        return true;
    }

    /**
     * Start the custom receiver on the chromecast
     */
    private void launchReceiver() {
        try {
            Cast.Listener mCastListener = new Cast.Listener() {
                @Override
                public void onApplicationStatusChanged() {
                }

                @Override
                public void onApplicationDisconnected(int errorCode) {
                    teardown();
                }
            };

            // Connect to Google Play services
            ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks();
            ConnectionFailedListener mConnectionFailedListener = new ConnectionFailedListener();
            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions.builder(mSelectedDevice, mCastListener);

            mApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(mConnectionCallbacks)
                    .addOnConnectionFailedListener(mConnectionFailedListener)
                    .build();

            mApiClient.connect();
        } catch (Exception e) {
            Log.e(TAG, "Failed launchReceiver", e);
        }
    }

    /**
     * Tear down the connection to the receiver
     */
    private void teardown() {
        if (mApiClient != null) {
            try {
                if (mPongcastCallbacks != null) {
                    Cast.CastApi.removeMessageReceivedCallbacks(mApiClient, mPongcastCallbacks.getNamespace());
                    mPongcastCallbacks = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "Exception while removing channel", e);
            }

            if (mApiClient.isConnected()) {
                mApiClient.disconnect();
            }
            mApiClient = null;
        }

        setCourtState(COURT_STATE.UNKNOWN);

        mSelectedDevice = null;
        mWaitingForReconnect = false;
    }

    /**
     * Send a message to the receiver
     *
     * @param message String to send to the cast device
     */
    private void sendMessage(String message) {
        if (mApiClient != null) {
            if (mPongcastCallbacks != null) {
                try {
                    Cast.CastApi.sendMessage(mApiClient,
                            mPongcastCallbacks.getNamespace(), message)
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
                    Toast.makeText(MainActivity.this, "Exception while sending message", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "No Cast HelloWorld Channel", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "No Cast Client API", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Callback for MediaRouter events
     */
    private class MyMediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo info) {
            Log.d(TAG, "onRouteSelected");
            // Handle the user route selection.
            mSelectedDevice = CastDevice.getFromBundle(info.getExtras());

            launchReceiver();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo info) {
            Log.d(TAG, "onRouteUnselected: info=" + info);
            teardown();
            mSelectedDevice = null;
        }
    }

    /**
     * This is the callback class that handles the result on attempt to start the receiver
     */
    private class MyLaunchResultCallback implements ResultCallback<Cast.ApplicationConnectionResult> {
        @Override
        public void onResult(ApplicationConnectionResult result) {
            Status status = result.getStatus();
            if (status.isSuccess()) {
                // Court now exists
                setCourtState(COURT_STATE.READY);

                // Create the custom message channel to talk to the receiver
                mPongcastCallbacks = new PongcastCallbacks();
                try {
                    Cast.CastApi.setMessageReceivedCallbacks(mApiClient, mPongcastCallbacks.getNamespace(),
                            mPongcastCallbacks);

                    // Got into the court!
                    setCourtState(COURT_STATE.I_AM_IN);
                } catch (IOException e) {
                    Log.e(TAG, "Exception while creating channel", e);
                }
            } else {
                Log.e(TAG, "application could not launch");
                teardown();
            }
        }
    }

    /**
     * Google Play services callbacks
     */
    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle connectionHint) {
            Log.d(TAG, "onConnected");

            if (mApiClient == null) {
                // We got disconnected while this runnable was pending execution.
                return;
            }

            try {
                if (mWaitingForReconnect) {
                    mWaitingForReconnect = false;

                    // Check if the receiver app is still running
                    if ((connectionHint != null) && connectionHint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
                        Log.d(TAG, "App  is no longer running");
                        teardown();
                    } else {
                        // Re-create the custom message channel
                        try {
                            Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                                    mPongcastCallbacks.getNamespace(),
                                    mPongcastCallbacks);

                            // Got into the court!
                            setCourtState(COURT_STATE.I_AM_IN);
                        } catch (IOException e) {
                            Log.e(TAG, "Exception while setting message received callbacks", e);
                        }
                    }
                } else {
                    // Launch the receiver app - creating the court to play in!!
                    setCourtState(COURT_STATE.CREATING);
                    Cast.CastApi.launchApplication(mApiClient, getString(R.string.app_id), false)
                            .setResultCallback(new MyLaunchResultCallback());
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to launch application", e);
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "onConnectionSuspended");
            mWaitingForReconnect = true;
        }
    }

    /**
     * Google Play services callbacks
     */
    private class ConnectionFailedListener implements
            GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.e(TAG, "onConnectionFailed ");
            teardown();
        }
    }

    /**
     * Custom message channel to receive messages from the chromecast
     */
    class PongcastCallbacks implements MessageReceivedCallback {
        /**
         * @return custom namespace
         */
        public String getNamespace() {
            return getString(R.string.namespace);
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
     * Parse a message from the Receiver about the game
     * @param message - from the receiver
     */
    private void parseGameMessage(String message) {
        Log.i(TAG, "Game Message: " + message);
        if (message.startsWith("GAME WON") || message.startsWith("GAME LOST")) {
            setGameState(GAME_STATE.GAME_ENDED);
        } else if (message.equals("GAME STARTED")) {
            setGameState(GAME_STATE.GAME_IN_PLAY);
        } else if (message.equals("GAME PAUSED")) {
            setGameState(GAME_STATE.GAME_PAUSED);
        } else {
            setGameState(GAME_STATE.NO_GAME);
        }
    }

    /**
     * Parse messages that affect the paddle and make appropriate changes
     * @param message - from receiver to parse
     */
    private void parsePaddleMessage(String message) {
        Log.i(TAG, "Paddle Message: " + message);
        final String PADDLE_YES_PREFIX = "PADDLE YES";
        if (message.startsWith(PADDLE_YES_PREFIX)) {
            paddleState = PADDLE_STATE.HAS_PADDLE;

            if ((courtState != COURT_STATE.I_AM_IN) || (gameState != GAME_STATE.GAME_IN_PLAY)) {
                paddle = message.split(PADDLE_YES_PREFIX)[1];
                Toast.makeText(MainActivity.this, "You got " + paddle + " paddle", Toast.LENGTH_LONG).show();
                setGameState(GAME_STATE.GAME_READY_TO_START);
            }
        } else {
            paddleState = PADDLE_STATE.NO_PADDLE;
            Toast.makeText(MainActivity.this, "Sorry, " + message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Keep track of the game state and make the appropriate changes to views on changes in it
     *
     * @param newGameState - the new state
     */
    private void setGameState(GAME_STATE newGameState) {
        this.gameState = newGameState;

        switch (newGameState) {
            case GAME_ENDED:
                startGameButton.setVisibility(View.VISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                break;

            case GAME_IN_PLAY:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.VISIBLE);
                break;

            case GAME_PAUSED:
                startGameButton.setVisibility(View.VISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                break;

            case NO_GAME:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                break;

            case GAME_READY_TO_START:
                startGameButton.setVisibility(View.VISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                break;
        }
    }

    /**
     * Keep track of changes in the state of the court and make the appropriate changes to other states and views
     * when it changes.
     *
     * @param newCourtState - the new state
     */
    private void setCourtState(COURT_STATE newCourtState) {
        this.courtState = newCourtState;
        Log.i(TAG, "Court State set to " + newCourtState.toString());

        switch (newCourtState) {
            case UNKNOWN:
            case CREATING:
            case READY:
            case I_AM_IN:
                setGameState(GAME_STATE.NO_GAME);
                break;
        }
    }
}
