package net.mackenzie_serres.pongcast;

import android.util.Log;
import net.mackenzie_serres.chromecast.ChromecastInteractor;
import net.mackenzie_serres.chromecast.ChromecastInteractor.CHROMECAST_EVENT;
import net.mackenzie_serres.chromecast.GameController;

/**
 * This class implements control of the Pong game, implementing the GameController Interface that permits it to
 * work in conjunction with the ChromecastInteractor.
 *
 * It holds the logic of what actions can be taken and how the games responds to events, according to the current
 * state of the game.
 *
 * User: andrew
 * Date: 11/01/15
 * Time: 02:15
 * <p/>
 * Copyright Andrew Mackenzie, 2013
 */
public class PongController implements GameController {
    // ENUMS
    public static enum COURT_STATE {
        UNKNOWN, FOUND_WAITING, CREATING_COURT, ON_COURT, READY_FOR_GAME, GAME_IN_PLAY, GAME_PAUSED, GAME_OVER
    }

    public static enum GAME_EVENT {
        GOT_PADDLE, NO_PADDLE, GAME_STARTED, GAME_PAUSED, GAME_WON_LOST
    }

    // CONSTANTS
    private static final String TAG = PongController.class.getSimpleName();
    private static final String START_GAME_MESSAGE = "StartPlay";
    private static final String PAUSE_PLAY_MESSAGE = "PausePlay";
    private static final String PADDLE_UP_MESSAGE = "MoveUp";
    private static final String PADDLE_DOWN_MESSAGE = "MoveDown";
    private static final String PADDLE_YES_PREFIX = "PADDLE YES";

    // MUTABLES
    // Set initial states
    private COURT_STATE courtState;
    private ChromecastInteractor chromecast;
    private PongControllerView gameView;

    public PongController() {
        setCourtState(COURT_STATE.UNKNOWN);
    }

    public void setGameView(PongControllerView gameView) {
        this.gameView = gameView;
    }

    /**
     * Setter for the ChromeInteractor - as they have a mutual dependancy
     *
     * @param chromecastInteractor to use for controlling game
     */
    @Override
    public void setChromecastInteractor(final ChromecastInteractor chromecastInteractor) {
        this.chromecast = chromecastInteractor;
    }

    /**
     * Request from the view to start the game
     */
    public void startGame() {
        // TODO check state
        chromecast.sendMessage(START_GAME_MESSAGE);
    }

    /**
     * Request from the view to move paddle
     */
    public void paddleUp() {
        // TODO check state
        chromecast.sendMessage(PADDLE_UP_MESSAGE);
    }

    /**
     * Request from the view to move paddle
     */
    public void paddleDown() {
        // TODO check state
        chromecast.sendMessage(PADDLE_DOWN_MESSAGE);
    }

    /**
     * A request from the UI to pause the game
     */
    public void pause() {
        if ((courtState == COURT_STATE.GAME_IN_PLAY)) {
            chromecast.sendMessage(PAUSE_PLAY_MESSAGE);
        }
    }

    /**
     * Handle events coming from the chromecast
     *
     * @param event ChromecastInteractor.CHROMECAST_EVENT to process
     */
    @Override
    public void event(final CHROMECAST_EVENT event) {
        Log.d(TAG, "Chromecast event: " + event.toString());
        // TODO proper state machine
        switch (event) {
            case CONNECTING:
            case CONNECTION_SUSPENDED:
                setCourtState(COURT_STATE.FOUND_WAITING);
                break;

            case CONNECTED:
                setCourtState(COURT_STATE.CREATING_COURT);
                break;

            case DISCONNECTED:
                setCourtState(COURT_STATE.UNKNOWN);
                break;

            case READY:
                setCourtState(COURT_STATE.ON_COURT);
                break;
        }
    }

    /**
     * Parse a message from the chromecast controller and modify game state and react accordingly
     * @param message from chromecast to parse
     */
    @Override
    public void message(final String message) {
        if (message.startsWith("PADDLE")) {
            parsePaddleMessage(message);
        } else if (message.startsWith("GAME")) {
            parseGameMessage(message);
        } else {
            Log.w(TAG, "Unknown message: " + message);
        }
    }

    /**
     * Keep track of changes in the state of the court and make the appropriate changes to other states and views
     * when it changes.
     *
     * @param newCourtState - the new state
     */
    private void setCourtState(final COURT_STATE newCourtState) {
        Log.d(TAG, "Previous Court State = " + this.courtState.toString() + ", New state = " + newCourtState.toString());
        this.courtState = newCourtState;

        if (gameView != null) {
            gameView.setViewGameState(this.courtState);
        }
    }

    /**
     * Handle events coming from game itself
     *
     * @param event GAME_EVENT to process
     */
    private void event(final GAME_EVENT event) {
        event(event, null);
    }

    /**
     * Handle events coming from game itself
     *
     * @param event GAME_EVENT to process
     * @param message options message string with event
     */
    private void event(final GAME_EVENT event, String message) {
        Log.d(TAG, "Game event: " + event.toString());
        // TODO proper state machine
        switch (event) {
            case GAME_STARTED:
                setCourtState(COURT_STATE.GAME_IN_PLAY);
                break;

            case GAME_PAUSED:
                if (courtState == COURT_STATE.GAME_IN_PLAY) {
                    setCourtState(COURT_STATE.GAME_PAUSED);
                }
                break;

            case GAME_WON_LOST:
                if (courtState == COURT_STATE.GAME_IN_PLAY) {
                    gameView.message(message);
                    setCourtState(COURT_STATE.GAME_OVER);
                }
                break;

            case GOT_PADDLE:
                gameView.message("You got " + message.split(PADDLE_YES_PREFIX)[1] + " paddle");
                setCourtState(COURT_STATE.READY_FOR_GAME);
                break;

            case NO_PADDLE:
                gameView.message("Sorry, no paddle for you!");
                break;

            default:
                Log.e(TAG, "Unknown GAME_EVENT: " + event.toString());
        }
    }

    /**
     * Parse messages that affect the paddle and make appropriate changes
     *
     * @param message - from receiver to parse
     */
    private void parsePaddleMessage(final String message) {
        Log.i(TAG, "Paddle Message: " + message);
        if (message.startsWith(PADDLE_YES_PREFIX)) {
            if ((courtState == COURT_STATE.ON_COURT)) {
                event(GAME_EVENT.GOT_PADDLE, message);
            }
        } else {
            event(GAME_EVENT.NO_PADDLE);
        }
    }

    /**
     * Parse a message from the Receiver about the game
     *
     * @param message - from the receiver
     */
    private void parseGameMessage(final String message) {
        Log.i(TAG, "Game Message: " + message);
        if (message.startsWith("GAME WON") || message.startsWith("GAME LOST")) {
            event(GAME_EVENT.GAME_WON_LOST, message);
        } else if (message.equals("GAME STARTED")) {
            event(GAME_EVENT.GAME_STARTED);
        } else if (message.equals("GAME PAUSED")) {
            event(GAME_EVENT.GAME_PAUSED);
        }
    }
}

