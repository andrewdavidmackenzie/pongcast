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

import net.mackenzie.chromeinteractor.ChromecastInteractor;
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
        upButton.setOnTouchListener(new RepeatListener(40, 20, new View.OnClickListener() {
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
        downButton.setOnTouchListener(new RepeatListener(40, 20, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pongController.paddleDown();
            }
        }));

        pongController.setGameView(this);
    }


    /**
     * Update the views depending on the state of the court
     *
     * @param chromecastState to use to change the views on screen
     */
    public void setCourtState(final ChromecastInteractor.CHROMECAST_STATE chromecastState) {
        Log.d(TAG, "setCourtState() to " + chromecastState);

        switch (chromecastState) {
            case NO_WIFI:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.VISIBLE);
                messageView.setText(R.string.enableWifi);
                break;

            case NO_ROUTE_AVAILABLE:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.VISIBLE);
                messageView.setText(R.string.noRoute);
                break;

            case ROUTE_AVAILABLE:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.VISIBLE);
                messageView.setText(R.string.selectRoute);
                break;

            case CONNECTING:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.VISIBLE);
                messageView.setText(R.string.waiting);
                break;

            case CONNECTED:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.VISIBLE);
                messageView.setText(R.string.preparing);
                break;

            case RECEIVER_READY:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.VISIBLE);
                messageView.setText(R.string.onCourt);
                break;
        }
    }

    /**
     * Update the views depending on the state of the court and game
     *
     * @param gameState to use to change the views on screen
     */
    public void setGameState(final PongController.GAME_STATE gameState) {
        Log.d(TAG, "setGameState() with courtState = " + gameState);

        switch (gameState) {
            case NO_PADDLE:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.VISIBLE);
                messageView.setText(R.string.onCourt);
                break;

            case GOT_PADDLE:
            case GAME_WON_LOST:
            case GAME_PAUSED:
                startGameButton.setVisibility(View.VISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                messageView.setVisibility(View.INVISIBLE);
                break;

            case GAME_STARTED:
                startGameButton.setVisibility(View.INVISIBLE);
                paddleControls.setVisibility(View.VISIBLE);
                messageView.setVisibility(View.INVISIBLE);
                break;
        }
    }

    /**
     * Send a Toast (pop-up) message to the player
     * @param message to display
     */
    public void message(@Nullable final String message) {
        if (message != null) {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        }
    }
}