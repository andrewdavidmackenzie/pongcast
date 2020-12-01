package net.mackenzie.pongcast;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.mackenzie.chromeinteractor.ChromecastInteractor;
import net.mackenzie.chromeinteractor.GameController;

/**
 * This class implements control of the Pong game, implementing the GameController Interface that permits it to
 * work in conjunction with the ChromecastInteractor.
 * <p>
 * It holds the logic of what actions can be taken and how the games responds to events, according to the current
 * state of the game.
 * <p>
 * User: andrew
 * Date: 11/01/15
 * Time: 02:15
 * <p/>
 * Copyright Andrew Mackenzie, 2013
 */
public class PongController implements GameController {
    public enum GAME_STATE {
        NO_PADDLE, GOT_PADDLE, GAME_STARTED, GAME_PAUSED, GAME_WON_LOST
    }

    // CONSTANTS
    private static final String LOG_TAG = "PongController";

    // MESSAGES receiver app may send
    private static final String START_GAME_MESSAGE = "StartPlay";
    private static final String PAUSE_PLAY_MESSAGE = "PausePlay";
    private static final String PADDLE_UP_MESSAGE = "MoveUp";
    private static final String PADDLE_DOWN_MESSAGE = "MoveDown";

    // MUTABLES
    private ChromecastInteractor.CHROMECAST_STATE chromecastState = ChromecastInteractor.CHROMECAST_STATE.NO_WIFI;
    private GAME_STATE gameState = GAME_STATE.NO_PADDLE;
    private ChromecastInteractor chromecastInteractor;
    private PongControllerView gameView;
    private final Activity activity;

    public PongController(Activity ac) {
        activity = ac;
    }

    public void setGameView(PongControllerView gameView) {
        this.gameView = gameView;
    }

    /**
     * Setter for the ChromeInteractor - as they have a mutual dependency
     *
     * @param chromecastInteractor to use for controlling game
     */
    @Override
    public void setChromecastInteractor(@NonNull final ChromecastInteractor chromecastInteractor) {
        this.chromecastInteractor = chromecastInteractor;
    }

    /**
     * Request from the view to start the game
     */
    public void startGame() {
        if (chromecastState == ChromecastInteractor.CHROMECAST_STATE.RECEIVER_READY) {
            // Ask the receiver to start the game
            chromecastInteractor.sendMessage(START_GAME_MESSAGE);
        }
    }

    /**
     * Request from the view to move paddle
     */
    public void paddleUp() {
        if (chromecastState == ChromecastInteractor.CHROMECAST_STATE.RECEIVER_READY) {
            chromecastInteractor.sendMessage(PADDLE_UP_MESSAGE);
        }
    }

    /**
     * Request from the view to move paddle
     */
    public void paddleDown() {
        if (chromecastState == ChromecastInteractor.CHROMECAST_STATE.RECEIVER_READY) {
            chromecastInteractor.sendMessage(PADDLE_DOWN_MESSAGE);
        }
    }

    /**
     * A request from the UI to pause the game
     */
    public void pause() {
        if (chromecastState == ChromecastInteractor.CHROMECAST_STATE.RECEIVER_READY) {
            chromecastInteractor.sendMessage(PAUSE_PLAY_MESSAGE);
        }
    }

    /**
     * Handle new states of the chromecast - sent by ChromeInteractor
     *
     * @param newState ChromecastInteractor.CHROMECAST_EVENT to process
     */
    @Override
    public void newChromecastState(@NonNull final ChromecastInteractor.CHROMECAST_STATE newState) {
        Log.d(LOG_TAG, "Previous Chromecast State = " + this.chromecastState + ", New state = " + newState);
        chromecastState = newState;
        gameView.setCourtState(newState);
    }

    /**
     * Parse a message from the chromecast controller into a GAME_STATE and process it
     *
     * @param message from chromecast to parse
     */
    @Override
    public void receiverMessage(@NonNull final String message) {
        Log.i(LOG_TAG, "Receiver Message: " + message);
        switch (message) {
            case "PADDLE NONE":
                newGameState(GAME_STATE.NO_PADDLE, null);
                break;
            case "PADDLE YES LEFT":
                newGameState(GAME_STATE.GOT_PADDLE, "You got left paddle");
                break;
            case "PADDLE YES RIGHT":
                newGameState(GAME_STATE.GOT_PADDLE, "You got right paddle");
                break;
            case "GAME WON":
                newGameState(GAME_STATE.GAME_WON_LOST, "Game won!");
                break;
            case "GAME LOST":
                newGameState(GAME_STATE.GAME_WON_LOST, "Game lost");
                break;
            case "GAME STARTED":
                newGameState(GAME_STATE.GAME_STARTED, null);
                break;
            case "GAME PAUSED":
                newGameState(GAME_STATE.GAME_PAUSED, null);
                break;
            default:
                break;
        }
    }

    /**
     * Handle events coming from game itself running in the receiver app on chromecast
     *
     * @param newGameState GAME_EVENT to process
     * @param message      options message string with event
     */
    private void newGameState(@NonNull final GAME_STATE newGameState, @Nullable String message) {
        if (chromecastState != ChromecastInteractor.CHROMECAST_STATE.RECEIVER_READY) {
            Log.w(LOG_TAG, "Game event with Chromecast state NOT RECEIVER_READY, ignoring");
            return;
        }

        setGameState(newGameState);

        switch (newGameState) {
            case GAME_WON_LOST:
            case GOT_PADDLE:
                gameView.message(message);
                break;

            case NO_PADDLE:
                gameView.message(activity.getResources().getString(R.string.noPaddle));
                break;
        }
    }

    /**
     * Keep track of changes in the state of the court and make the appropriate changes to other states and views
     * when it changes.
     *
     * @param newGameState - the new state of the game
     */
    private void setGameState(@NonNull final GAME_STATE newGameState) {
        Log.d(LOG_TAG, "Previous Game state: " + gameState + ", New Game State = " + newGameState);
        gameState = newGameState;
        gameView.setGameState(gameState);
    }
}

