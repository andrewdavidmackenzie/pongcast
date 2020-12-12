//////////////////////////////////// PADDLE ////////////////////////////////
function Paddle(x, frontX, y, width, height, courtHeight, context) {
    this.defaultSpeed = (courtHeight / 150) | 0;
    this.x = x | 0;
    this.frontX = frontX | 0;
    this.y = y | 0;
    this.width = width | 0;
    this.height = height | 0;
    this.halfHeight = (this.height / 2) | 0;
    this.context = context;
    /* restrict movement and leave a gap at top and bottom */
    this.minY = this.halfHeight | 0;
    this.maxY = ((courtHeight | 0) - this.height - this.halfHeight) | 0;
    this.topSection = (this.height / 3) | 0;
    this.bottomSection = ((2 * this.height) / 3) | 0;
}

Paddle.prototype.bounce = function () {
    //noinspection JSUnresolvedFunction
    window.audio.src = "paddle.mp3";
    window.audio.play();
};

Paddle.prototype.clear = function () {
    this.context.clearRect(this.x, this.y, this.width, this.height);
};

Paddle.prototype.draw = function () {
    this.context.fillRect(this.x, this.y, this.width, this.height);
};

Paddle.prototype.move = function (distance) {
    this.y += (distance | 0);

    // Stop at the top of the court
    if (this.y <= this.minY) {
        this.y = this.minY;
    } else if (this.y >= this.maxY) {
        this.y = this.maxY;
    }
};

//////////////////////////////////// PLAYER ////////////////////////////////
function Player(name) {
    this.name = name;
}

Player.prototype.givePaddle = function (paddle) {
    this.paddle = paddle;
};

//////////////////////////////////// COMPUTER PLAYER ////////////////////////////////
ComputerPlayer.prototype = new Player();

function ComputerPlayer(name) {
    Player.apply(this);
    this.name = name;
}

ComputerPlayer.prototype.updatePaddle = function (ball) {
    let diff = this.paddle.y + this.paddle.halfHeight - ball.y;
    return ((-diff / 4)  | 0);
};

ComputerPlayer.prototype.gameOver = function (won) {
};

//////////////////////////////////// BALL ////////////////////////////////
function Ball(court, ballSize, context, courtColor, ballColor) {
    this.court = court;
    this.ballSize = ballSize | 0;
    this.halfBallSize = (ballSize / 2) | 0;
    this.context = context;
    this.courtColor = courtColor;
    this.ballColor = ballColor;
    this.x = ((this.court.width  | 0) / 2) | 0;
    this.y = ((this.court.height | 0) / 2) | 0;
    this.y_speed = court.y_speed | 0;
    this.x_speed = court.x_speed | 0;
}

Ball.prototype.bounceWall = function (court) {
    this.y_speed = -this.y_speed | 0;
    court.bounce();
};

/*
 Top & Bottom thirds returns by inverting or increases by glancing
 Middle third reflects balls current angle
 */
Ball.prototype.bouncePaddle = function (paddle) {
    // Gain 10% of speed with every bounce
    this.x_speed = (-1.1 * this.x_speed) | 0;
    if (this.y < paddle.y + paddle.topSection) {
        if (this.y_speed === 0) {
            this.y_speed = (court.width / 200) | 0;
        } else if (this.y_speed > 0) {
            this.y_speed = -this.y_speed | 0;
        } else {
            this.y_speed = (this.y_speed * 2) | 0;
        }
    } else if (this.y > paddle.y + paddle.bottomSection) { // bottom section
        if (this.y_speed === 0) {
            this.y_speed = (-court.width / 200) | 0;
        } else if (this.y_speed > 0) {
            this.y_speed = (this.y_speed * 2) | 0;
        } else {
            this.y_speed = -this.y_speed | 0;
        }
    }

    paddle.bounce();
};

Ball.prototype.update = function () {
    let oldX = this.x | 0;

    // update position according to its speed
    this.x += this.x_speed;
    this.y += this.y_speed;

    // check for hitting the top wall
    let top_y = (this.y - this.halfBallSize) | 0;
    if (top_y <= 0) {
        this.y = this.halfBallSize | 0;
        this.bounceWall(this.court);
        return 0 | 0;
    }

    // check for hitting bottom wall
    let bottom_y = (this.y + this.halfBallSize) | 0;
    if (bottom_y >= this.court.height) {
        this.y = (this.court.height - this.halfBallSize) | 0;
        this.bounceWall(this.court);
        return 0 | 0;
    }

    if (this.x_speed < (0 | 0)) { // Going left
        // touching or behind paddle
        if ((this.x <= this.court.paddles[0].frontX)) {
            // Check for exiting court left - using the middle of the ball to calculate that
            if (this.x < 0) {
                return -1 | 0;
            }

            // was in front off, and now touching or behind paddle
            if ((oldX > this.court.paddles[0].frontX) && (bottom_y > this.court.paddles[0].y) &&
                (top_y < (this.court.paddles[0].y + this.court.paddles[0].height))) {
                this.bouncePaddle(this.court.paddles[0]);
            }
        }
    } else { // Going right
        // touching or behind paddle
        if ((this.x > this.court.paddles[1].frontX)) {
            // if leaves the court at the right
            if (this.x > this.court.width) {
                return 1 | 0;
            }

            // was in front off, and now touching or behind paddle
            if ((oldX < this.court.paddles[1].frontX) && (bottom_y > this.court.paddles[1].y) &&
                (top_y < (this.court.paddles[1].y + this.court.paddles[1].height))) {
                this.bouncePaddle(this.court.paddles[1]);
            }
        }
    }

    return 0 | 0;
};

Ball.prototype.clear = function () {
    this.context.clearRect(this.x - this.halfBallSize,
        this.y - this.halfBallSize,
        this.ballSize, this.ballSize);
};

Ball.prototype.draw = function () {
    this.context.fillRect(this.x - this.halfBallSize,
        this.y - this.halfBallSize,
        this.ballSize, this.ballSize);
};

//////////////////////////////////// GAME ////////////////////////////////
function Game(court) {
    console.log("New Game");
    this.court = court;
    this.court.game = this;
    this.pointsToWin = 21 | 0;

    // Create a new ball in the center of the court - Moving
    this.court.ball = new Ball(this.court, this.court.ballSize, this.court.context, this.court.courtColor, this.court.ballColor);

    // add players until enough for a game (2)
    while (this.court.numPlayers < (2 | 0)) {
        this.court.enter(new ComputerPlayer("Computer"));
    }

    this.court.players[0].score = 0 | 0;
    this.court.players[1].score = 0 | 0;

    // draw initial scoreboard
    this.court.scoreboard.draw();
}

Game.prototype.point = function (player, opponent) {
    console.log("Point for player: " + player.name);

    this.court.scoreboard.pointWon(player);

    if (player.score === this.pointsToWin) {
        this.end(player, opponent);
    } else {
        this.court.newBall();
    }
};

Game.prototype.end = function (winner, looser) {
    console.log("End of game. '" + winner.name + "' wins");
    winner.gameOver(true);
    looser.gameOver(false);

    // TODO find this sound then enable
//    this.gameWon.play();

    this.court.pausePlay();
    this.court.game = null;

    window.message("GAME OVER");
};

//////////////////////////////////// SCOREBOARD ////////////////////////////////
function ScoreBoard(court, scoreboardElement) {
    this.court = court;
    this.leftScore = scoreboardElement.getElementsByClassName("left")[0];
    this.rightScore = scoreboardElement.getElementsByClassName("right")[0];
}

ScoreBoard.prototype.pointWon = function (player) {
    // Play point won sound
    //noinspection JSUnresolvedFunction
    window.audio.src = "point.mp3";
    window.audio.play();

    // increment score of that player
    player.score++;

    // draw new score
    this.draw();
};

ScoreBoard.prototype.draw = function () {
    this.leftScore.innerHTML = this.court.players[0].score.toString();
    this.rightScore.innerHTML = this.court.players[1].score.toString();
};

//////////////////////////////////// COURT ////////////////////////////////
function Court(canvas, speed) {
    this.context = canvas.getContext('2d');
    this.courtColor = "#999999";

    canvas.width = canvas.clientWidth | 0;
    canvas.height = canvas.clientHeight | 0;

    this.width = canvas.width | 0;
    this.height = canvas.height | 0;

    this.y_speed = (speed * this.height / 400) | 0;
    this.x_speed = (speed * this.width / 200) | 0;

    // Draw court initially
    this.context.fillStyle = this.courtColor;
    this.context.fillRect(0, 0, this.width, this.height);
    // set the fill for ball and paddles from now on
    this.context.fillStyle = "#FFFFFF";

    let paddleWidth = 10 | 0;
    let paddleHeight = 50 | 0;
    let paddleXOffset = 60 | 0;
    let courtMiddleY = ((this.height - paddleHeight) / 2) | 0;

    this.paddles = new Array(2);
    this.paddles[0] = new Paddle(paddleXOffset, paddleXOffset + paddleWidth, courtMiddleY, paddleWidth, paddleHeight, this.height, this.context);
    let front = this.width - paddleXOffset - paddleWidth;
    this.paddles[1] = new Paddle(front, front, courtMiddleY, paddleWidth, paddleHeight, this.height, this.context);

    // Create a new ball in the center of the court - not moving
    this.ballSize = 10 | 0;
    this.ballColor = "#FFFFFF";

    this.scoreboard = new ScoreBoard(this, document.getElementById("scoreboard"));

    this.paused = false;

    this.players = new Array(2);
    this.players[0] = null;
    this.players[1] = null;
    this.numPlayers = 0 | 0;
    this.game = null;

    this.courtMessage();

    // Install into the global window object
    window.court = this;
}

Court.prototype.courtMessage = function () {
    if (this.numPlayers > (0 | 0)) {
        window.message(window.court.startMessage);
    } else {
        window.message(this.enterMessage);
    }
};

Court.prototype.setMessages = function(enter, start, pause) {
    this.enterMessage = enter;
    this.startMessage = start;
    this.pausedMessage = pause;

}

Court.prototype.newBall = function() {
    // Create a new ball in the center of the court - Moving
    this.ball = new Ball(this, this.ballSize, this.context, this.courtColor, this.ballColor);
}

Court.prototype.bounce = function () {
    //noinspection JSUnresolvedFunction
    window.audio.src = "wall.mp3";
    window.audio.play();
};

Court.prototype.enter = function (player) {
    let response;

    if (this.players[0] == null) {
        this.players[0] = player;
        this.players[0].givePaddle(this.paddles[0]);
        this.paddles[0].draw();
        this.numPlayers++;
        console.log("Player '" + player.name + "' enters court, gets left paddle");
        response = "PADDLE YES LEFT";
    } else if (this.players[1] == null) {
        this.players[1] = player;
        this.players[1].givePaddle(this.paddles[1]);
        this.paddles[1].draw();
        this.numPlayers++;
        console.log("Player '" + player.name + "' enters court, gets right paddle");
        response = "PADDLE YES RIGHT";
    } else {
        console.log("PADDLE NONE");
        response = "PADDLE NONE";
    }

    this.courtMessage();
    return response;
};

Court.prototype.leave = function (leaver) {
    let winner;
    console.log("Player '" + leaver.name + "' has left the court");

    if (leaver === this.players[0]) {
        winner = this.players[1];
        this.players[0] = null;
    } else {
        winner = this.players[0];
        this.players[1] = null;
    }

    if (this.game) {
        this.game.end(winner, leaver);
    } else {
        window.message(this.enterMessage);
    }

    // and remove from the court
    this.numPlayers--;

    if (this.ball) {
        this.ball.clear();
    }

    // Reclaim his paddle
    leaver.paddle.clear();
    leaver.paddle = null;
};

// TODO Control game state here
Court.prototype.startPlay = function () {
    if (this.game == null) {
        this.game = new Game(this);
    }

    window.message("");

    console.log("Starting play");
    this.paused = false;
    this.update();
};

// TODO Control game state here
Court.prototype.pausePlay = function () {
    this.paused = true;
    console.log("Play paused");
    window.message(window.court.pausedMessage);
};

// TODO Control game state here
Court.prototype.restartPlay = function () {
    this.paused = false;
    window.message("");
    console.log("Play restarted");
    this.update();
};

// TODO Control game state here
Court.prototype.togglePlay = function () {
    // TODO control Game Over state
    if (this.paused) {
        this.restartPlay();
    } else {
        this.pausePlay();
    }
};

Court.prototype.draw = function () {
    // update paddle positions
    if (this.players[0]) {
        let move_0 = this.players[0].updatePaddle(this.ball);
        if (move_0 !== (0 | 0)) {
            this.paddles[0].clear();
            this.paddles[0].move(move_0);
        }
    }

    if (this.players[1]) {
        let move_1 = this.players[1].updatePaddle(this.ball);
        if (move_1 !== (0 | 0)) {
            this.paddles[1].clear();
            this.paddles[1].move(move_1);
        }
    }

    if (window.debug) {
        //noinspection JSUnresolvedVariable
        console.log('        +--> paddles clear() & move() finished after ' + (performance.now() - window.start) + ' ms');
    }

    if (this.ball) {
        this.ball.clear();

        // Update the ball position and detect if it has exited one end of the court or another
        let result = this.ball.update();

        if (result !== (0 | 0)) {
            if (result === (-1 | 0))
                this.game.point(this.players[1], this.players[0]);
            else
                this.game.point(this.players[0], this.players[1]);
        }

        // Draw the ball at the new position
        this.ball.draw();

        if (window.debug) {
            //noinspection JSUnresolvedVariable
            console.log('        +--> ball clear() & update() && draw() finished at ' + (performance.now() - window.start) + ' ms');
        }

        // Draw them after the ball may have deleted a part of them
        this.paddles[0].draw();
        this.paddles[1].draw();

        if (window.debug) {
            //noinspection JSUnresolvedVariable
            console.log('        +--> paddles.draw() finished at ' + (performance.now() - window.start) + ' ms');
        }
    }
};

// This will be called from window on refresh
Court.prototype.update = function () {
    if (!window.court.paused) {
        if (window.debug) {
            //noinspection JSUnresolvedVariable
            console.log('time since last update ' + (performance.now() - window.start) + ' ms');

            //noinspection JSUnresolvedVariable
            window.start = performance.now();
        }

        window.court.draw(); // my drawing routing

        if (window.debug) {
            //noinspection JSUnresolvedVariable
            let end = performance.now();

            console.log('    +--> court.draw() finished after ' + (end - window.start) + ' ms');
        }

        // reschedule next animation update
        window.requestAnimationFrame(window.court.update);
    }
};

window.enableDebug = function enableDebug() {
    window.debug = 1;
};