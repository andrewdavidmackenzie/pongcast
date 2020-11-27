package net.mackenzie.pongcast;

import android.annotation.SuppressLint;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.mediarouter.media.MediaRouteSelector;

import com.google.android.gms.cast.CastMediaControlIntent;

import net.mackenzie.chromeinteractor.RepeatListener;

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
    // CONSTANTS
    private static final String TAG = "PongControllerView";

    // IMMUTABLES
    private final Button startGameButton;
    private final TextView messageView;
    private final View paddleControls;
    private final AppCompatActivity activity;

    @SuppressLint("ClickableViewAccessibility")
    public PongControllerView(final AppCompatActivity activity,
                              final String receiverAppId,
                              final PongController pongController) {
        this.activity = activity; // Save activity to use when calling `Toast` to send a message
        activity.setContentView(R.layout.activity_main); // Set this `View` intro the activity

        ActionBar sab = activity.getSupportActionBar();
        if (sab != null) {
            sab.setBackgroundDrawable(new ColorDrawable(activity.getResources().getColor(android.R.color.transparent)));
        }

        // When the user clicks on the button, send a message to start the game
        startGameButton = activity.findViewById(R.id.playButton);
        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pongController.startGame();
            }
        });

        messageView = activity.findViewById(R.id.messageView);

        paddleControls = activity.findViewById(R.id.paddleControl);
        Button upButton, downButton;
        upButton = paddleControls.findViewById(R.id.upButton);
        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pongController.paddleUp();
            }
        });
        upButton.setOnTouchListener(new RepeatListener(300, 80, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pongController.paddleUp();
            }
        }));

        downButton = paddleControls.findViewById(R.id.downButton);
        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pongController.paddleDown();
            }
        });
        downButton.setOnTouchListener(new RepeatListener(300, 80, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pongController.paddleDown();
            }
        }));

        pongController.setGameView(this);
    }

    /**
     * Update the views depending on the state of the court and game
     *
     * @param courtState to use to change the views on screen
     */
    public void setViewGameState(final PongController.COURT_STATE courtState) {
        Log.d(TAG, "setViewGameState() with courtState = " + courtState);

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

            case ENTERING_COURT:
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
            case GAME_OVER:
            case GAME_PAUSED:
                startGameButton.setVisibility(View.VISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.INVISIBLE);
                break;

            case GAME_IN_PLAY:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.VISIBLE);
                messageView.setVisibility(View.INVISIBLE);
                break;
        }
    }

    /**
     * Send a message to the player
     * @param message to display
     */
    public void message(@Nullable final String message) {
        if (message != null) {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        }
    }
}