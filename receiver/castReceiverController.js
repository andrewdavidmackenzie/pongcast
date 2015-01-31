function CastController() {
    cast.receiver.logger.setLevelValue(cast.receiver.LoggerLevel.WARNING);

    window.castReceiverManager = cast.receiver.CastReceiverManager.getInstance();

    // create a CastMessageBus to handle messages for a custom namespace
    window.messageBus = window.castReceiverManager.getCastMessageBus('urn:x-cast:net.mackenzie_serres.pongcast');

    // handler for the 'ready' event
    castReceiverManager.onReady = function (event) {
        window.castReceiverManager.setApplicationState("Ready for players...");
    };

    // handler for 'senderconnected' event - this player can then enter the court
    castReceiverManager.onSenderConnected = function (event) {
        var name = event.senderId;
        window.outputLine("Player Connected: " + name);
        window.outputLine("Players Connected: " + window.castReceiverManager.getSenders().length);

        // Keep track of all people connected, indexing them by unique name
        var player = new ChromecastPlayer(window.court, name);
        window.players = new Array();
        window.players[name] = player;

        // have the player enter the court
        var response = window.court.enter(player);
        // send a message to the player to tell them if they got a paddle and which one
        var senderChannel = window.messageBus.getCastChannel(event.senderId);
        senderChannel.send(response);
    };

    // handler for 'senderdisconnected' event
    castReceiverManager.onSenderDisconnected = function (event) {
        window.court.leave(window.players[event.senderId]);

        // when the last man leaves - switch out the lights
        if (window.castReceiverManager.getSenders().length == 0) {
            setTimeout(function() {
                window.close();
            }, 3000);
        }
    };

    // handler for incoming messages on the message bus
    window.messageBus.onMessage = function (event) {
        window.outputLine('Message [' + event.senderId + ']: ' + event.data);

        // handle message
        switch (event.data) {
            case "StartPlay":
                window.court.startPlay();
                window.messageBus.broadcast("GAME STARTED");
                break;

            case "PausePlay":
                window.court.pausePlay();
                window.messageBus.broadcast("GAME PAUSED");
                break;

            case "MoveUp":
                window.players[event.senderId].updownCount++;
                break;

            case "MoveDown":
                window.players[event.senderId].updownCount--;
                break;

            default:
                break;
        }

    };

    window.court.enterMessage = "CONNECT TO CHROMECAST";
    window.court.startMessage = "CLICK PLAY ICON";

    // start the CastReceiverManager with an application status message
    window.castReceiverManager.start({statusText: "Court is ready"});
}

// A subclass of PaddleController, which must implement:
// updatePaddle(court, paddle, ball)
// gameOver(won)
ChromecastPlayer.prototype = new Player();

// ChromecastPlayer that controls a paddle using up and down keys on the keyboard
function ChromecastPlayer(court, name) {
    Player.apply(this, court);
    this.name = name;
    this.updownCount = 0;
}

/*
 This is called on each update of the screen. Move the paddle corresponding to the number of requests we got
 to move up/down from the sender since the last update
 */
ChromecastPlayer.prototype.updatePaddle = function () {
    var movement = -(this.paddle.defaultSpeed * this.updownCount);
    this.updownCount = 0;
    return movement;
};

ChromecastPlayer.prototype.gameOver = function (won) {
    // send a message to the player to tell them they won or lost
    try {
        var senderChannel = window.messageBus.getCastChannel(this.name);
        if (won) {
            senderChannel.send("GAME WON");
        } else {
            senderChannel.send("GAME LOST");
        }
    } catch (err) {
        // We might have lost the game because we lost the connection and won't be able to send message
    }
};