// Create a keyboard controller for a paddle
function KeyboardController() {
    PaddleController.call();

    window.keysDown = new Array();

    window.addEventListener("keydown", function (event) {
        // Mark this key in the keysDown array as pressed
        window.keysDown[event.keyCode] = true;
    });

    window.addEventListener("keyup", function (event) {
        // remove this key from the keysDown array
        delete window.keysDown[event.keyCode];
    });
}

// Check what keys are pressed everytime we get asked to update our paddle position
KeyboardController.prototype.updatePaddle = function (court, paddle, ball) {
    if (window.keysDown.length > 0) {
        for (var key in window.keysDown) {
            var value = Number(key);
            if (value == 38) { // up arrow
                console.log("Keyboard arrow up");
                paddle.moveUp();
            } else if (value == 40) { // down arrow
                console.log("Keyboard arrow down");
                paddle.moveDown();
            }
        }
    } else {
        paddle.stop();
    }
}

// Control court entry, game starting etc
window.addEventListener("keydown", function (event) {
    var value = Number(event.keyCode);
    if ((value == 16) || (value == 69)) { // 'e' or 'E'
        // Request to enter court
        //         var response = window.court.enter(new Player("Keyboard", court, new KeyboardController()));
        //         if (response != "Court full") {
        window.court.enter(new Player("Keyboard", court, new KeyboardController()));
    }

    if ((value == 83) || (value == 115)) { // 's' or 'S'
        window.court.startPlay();
    }

    if (value == 32) { // space
        court.toggle();
    }

    if ((value == 15) || (value == 68)) { // 'd' or 'D'
        window.message.style.visibility = "visible";
        window.outputLine("Debug enabled")
    }
});