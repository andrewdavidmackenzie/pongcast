package net.mackenzie_serres.pongcast;

import android.graphics.drawable.ColorDrawable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.cast.CastMediaControlIntent;
import net.mackenzie_serres.chromecast.RepeatListener;

/**
 * This class provides view functionality for the controller of the game, such as joining, leaving,
 * moving paddles etc - but not a view of the game itself.
 *
 * User: andrew
 * Date: 11/01/15
 * Time: 02:50
 * <p/>
 * Copyright Andrew Mackenzie, 2013
 */
public class PongControllerView {
    // IMMUTABLES
    private final MediaRouteSelector mediaRouteSelector;
    private final Button startGameButton;
    private final TextView messageView;
    private final View paddleControls;
    private final ActionBarActivity activity;

    public PongControllerView(final ActionBarActivity activity,
                              final String receiverAppId,
                              final PongController pongController) {
        this.activity = activity;
        activity.setContentView(R.layout.activity_main);

        activity.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));

        // When the user clicks on the button, send a message to start the game
        startGameButton = (Button) activity.findViewById(R.id.playButton);
        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pongController.startGame();
            }
        });

        messageView = (TextView)activity.findViewById(R.id.messageView);

        paddleControls = activity.findViewById(R.id.paddleControl);
        Button upButton, downButton;
        upButton = (Button) paddleControls.findViewById(R.id.upButton);
        upButton.setOnTouchListener(new RepeatListener(300, 80, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pongController.paddleUp();
            }
        }));

        downButton = (Button) paddleControls.findViewById(R.id.downButton);
        downButton.setOnTouchListener(new RepeatListener(300, 80, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pongController.paddleDown();
            }
        }));

        pongController.setGameView(this);

        this.mediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(
                CastMediaControlIntent.categoryForCast(receiverAppId)).build();
    }

    /**
     * Accessor for media route selector in UI
     * @return the media route selector for the chromecast
     */
    public MediaRouteSelector getMediaSelector() {
        return mediaRouteSelector;
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
     * Update the views depending on the state of the court and game
     *
     * @param courtState to use to change the views on screen
     */
    public void setViewGameState(final PongController.COURT_STATE courtState) {
        switch (courtState) {
            case NO_WIFI:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.VISIBLE);
                messageView.setText(R.string.enableWifi);
                break;

            case NO_AVAILABLE_COURT:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.VISIBLE);
                messageView.setText(R.string.noRoute);
                break;

            case COURT_AVAILABLE:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.VISIBLE);
                messageView.setText(R.string.selectRoute);
                break;

            case WAITING_TO_ENTER_COURT:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.VISIBLE);
                messageView.setText(R.string.waiting);
                break;

            case PREPARING_COURT:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.VISIBLE);
                messageView.setText(R.string.preparing);
                break;

            case ON_COURT:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.VISIBLE);
                messageView.setText(R.string.onCourt);
                break;

            case READY_FOR_GAME:
                startGameButton.setVisibility(View.VISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.INVISIBLE);
                break;

            case GAME_OVER:
                startGameButton.setVisibility(View.VISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.INVISIBLE);
                break;

            case GAME_IN_PLAY:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.VISIBLE);
                messageView.setVisibility(View.INVISIBLE);
                break;

            case GAME_PAUSED:
                startGameButton.setVisibility(View.VISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.INVISIBLE);
                break;
        }
    }

    /**
     * Send a message to the player
     * @param message to display
     */
    public void message(final String message) {
        if (message != null) {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        }
    }
}