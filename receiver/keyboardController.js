// Keyboard Controller - can introduce a new KeyboardPlayer into the court on pressing "e" or "E" for "Enter"

// TODO avoid use of the window.* and store references in local variables
function KeyboardController(court) {
    window.addEventListener("keydown", function (event) {
        var value = Number(event.keyCode);
        if ((value == 16) || (value == 69)) { // 'e' or 'E'
            window.court.enter(new KeyboardPlayer(court));
        }
    });
}

KeyboardPlayer.prototype = new Player();

// KeyboardPlayer that controls a paddle using up and down keys on the keyboard
function KeyboardPlayer(court) {
    Player.apply(this, court);

    this.name = "Keyboard";
    this.court = court;

    window.keysDown = new Array();

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
        var value = Number(event.keyCode);

        if ((value == 83) || (value == 115)) { // 's' or 'S'
            court.startPlay();
        }

        if (value == 32) { // space
            court.toggle();
        }

        if ((value == 15) || (value == 68)) { // 'd' or 'D'
            window.message.style.visibility = "visible";
            window.outputLine("Debug enabled")
        }
    });

    window.outputLine("Keyboard Player ready");
}

// Check what keys are pressed everytime we get asked to update our paddle position
KeyboardPlayer.prototype.updatePaddle = function (ball) {
    if (window.keysDown.length > 0) {
        for (var key in window.keysDown) {
            var value = Number(key);
            if (value == 38) { // up arrow
                this.paddle.moveUp();
            } else if (value == 40) { // down arrow
                this.paddle.moveDown();
            }
        }
    } else {
        this.paddle.stop();
    }
};

