package net.mackenzie_serres.pongcast;

import android.util.Log;

/**
 * User: andrew
 * Date: 11/01/15
 * Time: 02:15
 * <p/>
 * Copyright Andrew Mackenzie, 2013
 */
public class GameController {
    private static final String TAG = GameController.class.getSimpleName();

    public static enum GAME_EVENT {
        APP_STARTED, DISCONNECTED, CONNECTED, COURT_EXISTS, COURT_ACCEPTED_ME, COURT_DESTROYED, GOT_PADDLE, NO_PADDLE,
        GAME_STARTED, GAME_PAUSED, GAME_WON_LOST
    }

    public static enum COURT_STATE {
        UNKNOWN, CREATING_COURT, COURT_READY, ON_COURT, READY_FOR_GAME, GAME_IN_PLAY, GAME_PAUSED, GAME_OVER
    }

    // Set initial states
    private COURT_STATE courtState = COURT_STATE.UNKNOWN;
    private ChromecastInteractor chromecast;
    private GameControllerView gameView;

    public GameController() {
    }

    public void setGameView(GameControllerView gameView) {
        this.gameView = gameView;

        event(GAME_EVENT.APP_STARTED);
    }

    /**
     * Setter for the ChromeInteractor - as they have a mutual dependancy
     *
     * @param chromecastInteractor to use for controlling game
     */
    public void setChromecastInteractor(ChromecastInteractor chromecastInteractor) {
        this.chromecast = chromecastInteractor;
    }

    /**
     * Request from the view to start the game
     */
    public void startGame() {
        // TODO check state
        chromecast.startGame();
    }

    /**
     * Request from the view to move paddle
     */
    public void paddleUp() {
        // TODO check state
        chromecast.paddleUp();
    }

    /**
     * Request from the view to move paddle
     */
    public void paddleDown() {
        // TODO check state
        chromecast.paddleDown();
    }

    /**
     * A request from the UI to pause the game
     */
    public void pause() {
        if ((courtState == COURT_STATE.GAME_IN_PLAY)) {
            chromecast.pausePlay();
        }
    }

    /**
     * Keep track of changes in the state of the court and make the appropriate changes to other states and views
     * when it changes.
     *
     * @param newCourtState - the new state
     */
    private void setCourtState(COURT_STATE newCourtState) {
        Log.i(TAG, "Previous Court State = " + this.courtState.toString() + ", New state = " + newCourtState.toString());
        this.courtState = newCourtState;

        gameView.setViewGameState(this.courtState);
    }

    /**
     * An event happened that affects the court/game state, so figure out the new state and take needed actions
     *
     * @param event - that just happened
     */
    public void event(GAME_EVENT event) {
        event(event, null);
    }

    public void event(GAME_EVENT event, String message) {
        Log.d(TAG, "Game event: " + event.toString());
        // TODO proper state machine
        switch (event) {
            case APP_STARTED:
                setCourtState(COURT_STATE.UNKNOWN);
                break;

            case CONNECTED:
                setCourtState(COURT_STATE.CREATING_COURT);
                break;

            case DISCONNECTED:
                setCourtState(COURT_STATE.UNKNOWN);
                break;

            case COURT_EXISTS:
                setCourtState(COURT_STATE.COURT_READY);
                break;

            case COURT_ACCEPTED_ME:
                setCourtState(COURT_STATE.ON_COURT);
                break;

            case COURT_DESTROYED:
                setCourtState(COURT_STATE.UNKNOWN);
                break;

            case GOT_PADDLE:
                if ((courtState == COURT_STATE.ON_COURT)) {
                    gameView.message("You got " + message + " paddle");
                    setCourtState(COURT_STATE.READY_FOR_GAME);
                }
                break;

            case NO_PADDLE:
                gameView.message("Sorry, no paddle for you!");
                break;

            case GAME_STARTED:
                if ((courtState == COURT_STATE.READY_FOR_GAME) || (courtState == COURT_STATE.GAME_PAUSED)){
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
                    gameView.message(message);
                    setCourtState(COURT_STATE.GAME_OVER);
                }
                break;
        }
    }
}

