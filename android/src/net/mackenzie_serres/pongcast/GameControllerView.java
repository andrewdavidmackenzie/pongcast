package net.mackenzie_serres.pongcast;

import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * User: andrew
 * Date: 11/01/15
 * Time: 02:50
 * <p/>
 * Copyright Andrew Mackenzie, 2013
 */
public class GameControllerView {
    private Button startGameButton;
    private View paddleControls;
    private ActionBarActivity activity;

    public GameControllerView(final ActionBarActivity activity, final PongController pongController) {
        Button upButton, downButton;

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

        paddleControls = activity.findViewById(R.id.paddleControl);
        upButton = (Button) paddleControls.findViewById(R.id.upButton);
        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pongController.paddleUp();
            }
        });

        downButton = (Button) paddleControls.findViewById(R.id.downButton);
        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pongController.paddleDown();
            }
        });

        pongController.setGameView(this);
    }

    /**
     * Update the views depending on the state of the court and game
     *
     * @param courtState to use to change the views on screen
     */
    public void setViewGameState(PongController.COURT_STATE courtState) {
        switch (courtState) {
            case UNKNOWN:
                break;

            case CREATING_COURT:
                break;

            case COURT_READY:
                break;

            case ON_COURT:
                break;

            case GAME_OVER:
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

            case READY_FOR_GAME:
                startGameButton.setVisibility(View.VISIBLE);
                paddleControls.setVisibility(View.INVISIBLE);
                break;
        }
    }

    /**
     * Send a message to the player
     * @param message to display
     */
    public void message(final String message) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
    }
}