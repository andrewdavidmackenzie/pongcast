// A subclass of PaddleController, which must implement: updatePaddle = function (court, paddle, ball)
ChromecastPlayer.prototype = new Player();

// KeyboardPlayer that controls a paddle using up and down keys on the keyboard
function ChromecastPlayer(court, name) {
    Player.apply(this, court);
    this.name = name;
}

// Check what keys are pressed everytime we get asked to update our paddle position
ChromecastPlayer.prototype.updatePaddle = function (ball) {
    // this.paddle.move, moveUp, moveDown, stop
};

ChromecastPlayer.prototype.youWin = function () {
};

function CastController(court) {
    cast.receiver.logger.setLevelValue(0);

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
            // TODO set a timer before ending
            window.close();
        }
    };

    // handler for incoming messages on the message bus
    window.messageBus.onMessage = function (event) {
        window.outputLine('Message [' + event.senderId + ']: ' + event.data);

        // handle message
        switch (event.data) {
            case "StartPlay":
                window.court.startPlay();
                window.messageBus.broadcast("PLAY STARTED");
                break;

            case "PausePlay":
                window.court.pausePlay();
                window.messageBus.broadcast("PLAY PAUSED");
                break;

            default:
                break;
        }

    };

    // start the CastReceiverManager with an application status message
    window.castReceiverManager.start({statusText: "Court is ready"});
}

// A method to set application state
CastController.prototype.setState = function (state) {
    window.castReceiverManager.setApplicationState(state);
};
