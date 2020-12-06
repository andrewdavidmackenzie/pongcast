function CastController() {
    //noinspection JSUnresolvedVariable,JSUnresolvedFunction
    cast.receiver.logger.setLevelValue(cast.receiver.LoggerLevel.WARNING);

    //noinspection JSUnresolvedVariable,JSUnresolvedFunction
    window.castReceiverManager = cast.receiver.CastReceiverManager.getInstance();

    // create a CastMessageBus to handle messages for a custom namespace
    //noinspection JSUnresolvedVariable,JSUnresolvedFunction
    window.messageBus = window.castReceiverManager.getCastMessageBus('urn:x-cast:net.mackenzie.pongcast');

    // handler for the 'ready' event
    castReceiverManager.onReady = function () {
        //noinspection JSUnresolvedVariable,JSUnresolvedFunction
        window.castReceiverManager.setApplicationState("Ready for players...");
    };

    // handler for 'senderconnected' event - this player can then enter the court
    castReceiverManager.onSenderConnected = function (event) {
        //noinspection JSUnresolvedVariable
        let name = event.senderId;
        console.log("Player Connected: " + name);
        //noinspection JSUnresolvedFunction
        console.log("Players Connected: " + window.castReceiverManager.getSenders().length);

        // Keep track of all people connected, indexing them by unique name
        let player = new ChromecastPlayer(name);
        window.players = [];
        window.players[name] = player;

        // have the player enter the court
        let response = window.court.enter(player);
        // send a message to the player to tell them if they got a paddle and which one
        //noinspection JSUnresolvedFunction,JSUnresolvedVariable
        let senderChannel = window.messageBus.getCastChannel(event.senderId);
        senderChannel.send(response);
    };

    // handler for 'senderdisconnected' event
    castReceiverManager.onSenderDisconnected = function (event) {
        //noinspection JSUnresolvedVariable
        window.court.leave(window.players[event.senderId]);

        // when the last man leaves - switch out the lights
        //noinspection JSUnresolvedFunction
        if (window.castReceiverManager.getSenders().length === 0) {
            setTimeout(function() {
                window.close();
            }, 3000);
        }
    };

    // handler for incoming messages on the message bus
    window.messageBus.onMessage = function (event) {
        //noinspection JSUnresolvedVariable
        console.log('Message [' + event.senderId + ']: ' + event.data);

        // handle message
        switch (event.data) {
            case "StartPlay":
                window.court.startPlay();
                //noinspection JSUnresolvedFunction
                window.messageBus.broadcast("GAME STARTED");
                break;

            case "PausePlay":
                window.court.pausePlay();
                //noinspection JSUnresolvedFunction
                window.messageBus.broadcast("GAME PAUSED");
                break;

            case "MoveUp":
                //noinspection JSUnresolvedVariable
                window.players[event.senderId].updownCount++;
                break;

            case "MoveDown":
                //noinspection JSUnresolvedVariable
                window.players[event.senderId].updownCount--;
                break;

            default:
                break;
        }
    };

    // Messages are different depending on the Controller being used
    window.court.setMessages("CONNECT TO CHROMECAST", "CLICK PLAY ICON", "CLICK PLAY TO RESTART");

    // start the CastReceiverManager with an application status message
    window.castReceiverManager.start({statusText: "Court is ready"});
}

// A subclass of PaddleController, which must implement:
// updatePaddle(court, paddle, ball)
// gameOver(won)
ChromecastPlayer.prototype = new Player();

// ChromecastPlayer that controls a paddle using up and down keys on the keyboard
function ChromecastPlayer(name) {
    Player.apply(this, court);
    this.name = name;
    this.updownCount = 0;
}

/*
 This is called on each update of the screen. Move the paddle corresponding to the number of requests we got
 to move up/down from the sender since the last update
 */
ChromecastPlayer.prototype.updatePaddle = function () {
    let movement = -(this.paddle.defaultSpeed * this.updownCount);
    this.updownCount = 0;
    return movement;
};

ChromecastPlayer.prototype.gameOver = function (won) {
    // send a message to the player to tell them they won or lost
    try {
        //noinspection JSUnresolvedFunction
        let senderChannel = window.messageBus.getCastChannel(this.name);
        if (won) {
            senderChannel.send("GAME WON");
        } else {
            senderChannel.send("GAME LOST");
        }
    } catch (err) {
        // We might have lost the game because we lost the connection and won't be able to send message
    }
};