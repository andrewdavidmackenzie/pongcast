package net.mackenzie.pongcast;

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
    // ENUMS
    public enum COURT_STATE {
        INITIAL, NO_WIFI, NO_AVAILABLE_COURT, COURT_AVAILABLE, WAITING_TO_ENTER_COURT, ENTERING_COURT,
        ON_COURT, READY_FOR_GAME, GAME_IN_PLAY, GAME_PAUSED, GAME_OVER
    }

    public enum GAME_EVENT {
        GOT_PADDLE, NO_PADDLE, GAME_STARTED, GAME_PAUSED, GAME_WON_LOST
    }

    // CONSTANTS
    private static final String LOG_TAG = "PongController";

    // MESSAGES receiver app may send
    private static final String START_GAME_MESSAGE = "StartPlay";
    private static final String PAUSE_PLAY_MESSAGE = "PausePlay";
    private static final String PADDLE_UP_MESSAGE = "MoveUp";
    private static final String PADDLE_DOWN_MESSAGE = "MoveDown";

    // MUTABLES
    private COURT_STATE courtState = COURT_STATE.INITIAL;
    private ChromecastInteractor chromecastInteractor;
    private PongControllerView gameView;

    public PongController() {
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
        if (courtState == COURT_STATE.READY_FOR_GAME) {
            chromecastInteractor.sendMessage(START_GAME_MESSAGE);
        }
    }

    /**
     * Request from the view to move paddle
     */
    public void paddleUp() {
        if (courtState == COURT_STATE.GAME_IN_PLAY) {
            chromecastInteractor.sendMessage(PADDLE_UP_MESSAGE);
        }
    }

    /**
     * Request from the view to move paddle
     */
    public void paddleDown() {
        if (courtState == COURT_STATE.GAME_IN_PLAY) {
            chromecastInteractor.sendMessage(PADDLE_DOWN_MESSAGE);
        }
    }

    /**
     * A request from the UI to pause the game
     */
    public void pause() {
        if (courtState == COURT_STATE.GAME_IN_PLAY) {
            chromecastInteractor.sendMessage(PAUSE_PLAY_MESSAGE);
        }
    }

    /**
     * Parse a message from the chromecast controller into a gameEvent and process it
     *
     * @param message from chromecast to parse
     */
    @Override
    public void receiverMessage(@NonNull final String message) {
        Log.i(LOG_TAG, "Receiver Message: " + message);
        switch (message) {
            case "PADDLE NONE":
                processGameEvent(GAME_EVENT.NO_PADDLE, null);
                break;
            case "PADDLE YES LEFT":
                processGameEvent(GAME_EVENT.GOT_PADDLE, "You got left paddle");
                break;
            case "PADDLE YES RIGHT":
                processGameEvent(GAME_EVENT.GOT_PADDLE, "You got right paddle");
                break;
            case "GAME WON":
                processGameEvent(GAME_EVENT.GAME_WON_LOST, "Game won!");
                break;
            case "GAME LOST":
                processGameEvent(GAME_EVENT.GAME_WON_LOST, "Game lost");
                break;
            case "GAME STARTED":
                processGameEvent(GAME_EVENT.GAME_STARTED, null);
                break;
            case "GAME PAUSED":
                processGameEvent(GAME_EVENT.GAME_PAUSED, null);
                break;
            default:
                break;
        }
    }

    /**
     * Handle events coming from game itself
     *
     * @param event   GAME_EVENT to process
     * @param message options message string with event
     */
    private void processGameEvent(@NonNull final GAME_EVENT event, @Nullable String message) {
        Log.d(LOG_TAG, "Game event: " + event.toString());
        // TODO proper state machine
        switch (event) {
            case GAME_STARTED:
                if (courtState == COURT_STATE.READY_FOR_GAME) {
                    setCourtState(COURT_STATE.GAME_IN_PLAY);
                }
                break;

            case GAME_PAUSED:
                if (courtState == COURT_STATE.GAME_IN_PLAY) {
                    setCourtState(COURT_STATE.GAME_PAUSED);
                }
                break;

            case GAME_WON_LOST:
                if (courtState == COURT_STATE.GAME_IN_PLAY) {
                    setCourtState(COURT_STATE.GAME_OVER);
                    gameView.message(message);
                }
                break;

            case GOT_PADDLE:
                if (courtState == COURT_STATE.ON_COURT) {
                    setCourtState(COURT_STATE.READY_FOR_GAME);
                    gameView.message(message);
                }
                break;

            case NO_PADDLE:
                if (courtState == COURT_STATE.ON_COURT) {
                    setCourtState(COURT_STATE.ON_COURT);
                    gameView.message("Sorry, no paddle for you!");
                }
                break;
        }
    }

    /**
     * Handle events coming from the chromecast - sent by ChromeInteractor
     *
     * @param event ChromecastInteractor.CHROMECAST_EVENT to process
     */
    @Override
    public void chromecastEvent(@NonNull final ChromecastInteractor.CHROMECAST_EVENT event) {
        Log.d(LOG_TAG, "Chromecast event: " + event.toString());
        // TODO proper state machine
        switch (event) {
            case NO_WIFI:
                setCourtState(COURT_STATE.NO_WIFI);
                break;

            case NO_ROUTE_AVAILABLE:
                setCourtState(COURT_STATE.NO_AVAILABLE_COURT);
                break;

            case ROUTE_AVAILABLE:
                if ((courtState == COURT_STATE.NO_AVAILABLE_COURT) ||
                        (courtState == COURT_STATE.INITIAL)) {
                    setCourtState(COURT_STATE.COURT_AVAILABLE);
                }
                break;

            case CONNECTING:
            case CONNECTION_SUSPENDED:
                setCourtState(COURT_STATE.WAITING_TO_ENTER_COURT);
                break;

            case CONNECTED:
                setCourtState(COURT_STATE.ENTERING_COURT);
                break;

            case RECEIVER_READY:
                setCourtState(COURT_STATE.ON_COURT);
                break;
        }
    }

    /**
     * Keep track of changes in the state of the court and make the appropriate changes to other states and views
     * when it changes.
     *
     * @param newCourtState - the new state
     */
    private void setCourtState(@NonNull final COURT_STATE newCourtState) {
        Log.d(LOG_TAG, "Previous Court State = " + this.courtState.toString() + ", New state = " + newCourtState.toString());
        this.courtState = newCourtState;
        // Update the view to match the new state
        gameView.setViewGameState(this.courtState);
    }
}

