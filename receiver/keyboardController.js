// Keyboard Controller - can introduce a new KeyboardPlayer into the court on pressing "e" or "E" for "Enter"

function KeyboardController() {
    console.log("KeyboardController starting");
    window.keyboardPlayer = new KeyboardPlayer("Keyboard");

    // Messages are different depending on the Controller being used
    window.court.setMessages("PRESS E TO ENTER", "PRESS S TO START", "PRESS SPACE TO RESTART");
}

KeyboardPlayer.prototype = new Player();

// KeyboardPlayer that controls a paddle using up and down keys on the keyboard
function KeyboardPlayer(name) {
    Player.apply(this);
    this.name = name;

    window.keysDown = [];

    window.addEventListener("keydown", function (event) {
        // Mark this key in the keysDown array as pressed
        window.keysDown[event.keyCode] = true;
    });

    window.addEventListener("keyup", function (event) {
        // remove this key from the keysDown array
        delete window.keysDown[event.keyCode];
    });

    // Control court entry, game starting etc
    window.addEventListener("keydown", function (event) {
        let value = Number(event.keyCode);

        if ((value === 83) || (value === 115)) { // 's' or 'S'
            court.startPlay();
        }

        if (value === 32) { // space
            court.togglePlay();
        }

        if ((value === 15) || (value === 68)) { // 'd' or 'D'
            window.enableDebug();
        }

        if ((value === 16) || (value === 69)) { // 'e' or 'E'
            window.court.enter(window.keyboardPlayer);
        }

        if ((value === 23) || (value === 76)) { // 'l' or 'L'
            window.court.leave(window.keyboardPlayer);
        }
    });

    console.log("Keyboard Player ready");
}

// Check what keys are pressed everytime we get asked to update our paddle position
KeyboardPlayer.prototype.updatePaddle = function () {
    if (window.keysDown.length > 0) {
        for (let key in window.keysDown) {
            let value = Number(key);
            if (value === 38) { // up arrow
                return -this.paddle.defaultSpeed;
            } else if (value === 40) { // down arrow
                return this.paddle.defaultSpeed;
            }
        }
    }
    return 0;
};

KeyboardPlayer.prototype.gameOver = function (won) {
};

