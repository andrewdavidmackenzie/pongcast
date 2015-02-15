//////////////////////////////////// PADDLE ////////////////////////////////
function Paddle(x, frontX, y, width, height, courtHeight, context, paddleColor) {
    this.defaultSpeed = Math.floor((1 * courtHeight) / 100); // 2% of the height
    this.x = x;
    this.frontX = frontX;
    this.y = y;
    this.width = width;
    this.height = height;
    this.halfHeight = Math.floor(this.height / 2);
    this.context = context;
    /* restrict movement and leave a gap at top and bottom */
    this.minY = this.halfHeight;
    this.maxY = courtHeight - this.height - this.halfHeight;
    this.color = paddleColor;
    this.topSection = this.height / 3;
    this.bottomSection = (2 * this.height) / 3;
}

Paddle.prototype.bounce = function () {
    var bounce = new Audio("paddle.mp3");
    bounce.play();
};

Paddle.prototype.clear = function () {
    this.context.clearRect(this.x, this.y, this.width, this.height);
};

Paddle.prototype.draw = function () {
    this.context.fillRect(this.x, this.y, this.width, this.height);
};

Paddle.prototype.move = function (distance) {
    this.y += distance;

    // Stop at the top of the court
    if (this.y <= this.minY) {
        this.y = this.minY;
    } else if (this.y >= this.maxY) {
        this.y = this.maxY;
    }
};

//////////////////////////////////// PLAYER ////////////////////////////////
function Player(court, name) {
    this.court = court;
    this.name = name;
}

Player.prototype.givePaddle = function (paddle) {
    this.paddle = paddle;
};

//////////////////////////////////// COMPUTER PLAYER ////////////////////////////////
ComputerPlayer.prototype = new Player();

function ComputerPlayer(court, name) {
    Player.apply(this, court);
    this.name = name;
}

ComputerPlayer.prototype.updatePaddle = function (ball) {
    var diff = this.paddle.y + this.paddle.halfHeight - ball.y;
    return Math.floor(-diff / 4);
};

ComputerPlayer.prototype.gameOver = function (won) {
};

//////////////////////////////////// BALL ////////////////////////////////
function Ball(court, ballSize, context, courtColor, ballColor) {
    this.court = court;
    this.ballSize = ballSize;
    this.halfBallSize = Math.floor(ballSize / 2);
    this.context = context;
    this.courtColor = courtColor;
    this.ballColor = ballColor;
    this.x = this.court.width / 2;
    this.y = this.court.height / 2;
    this.y_speed = 0.5;
    this.x_speed = court.width / 200;
}

Ball.prototype.bounceWall = function (court) {
    this.y_speed = -this.y_speed;
    court.bounce();
};

/*
 Top & Bottom thirds returns by inverting or increases by glancing
 Middle third reflects balls current angle
 */
Ball.prototype.bouncePaddle = function (paddle) {
    // Gain a bit of speed with every bounce
    this.x_speed = -1.01 * this.x_speed;
    if (this.y < paddle.y + paddle.topSection) {
        if (this.y_speed == 0) {
            this.y_speed = court.width / 200;
        } else if (this.y_speed > 0) {
            this.y_speed = -this.y_speed;
        } else {
            this.y_speed = this.y_speed * 2;
        }
    } else if (this.y > paddle.y + paddle.bottomSection) { // bottom section
        if (this.y_speed == 0) {
            this.y_speed = -court.width / 200;
        } else if (this.y_speed > 0) {
            this.y_speed = this.y_speed * 2;
        } else {
            this.y_speed = -this.y_speed;
        }
    }

    paddle.bounce();
};

Ball.prototype.update = function () {
    var oldX = this.x;

    // update position according to its speed
    this.x += this.x_speed;
    this.y += this.y_speed;

    // check for hitting the top wall
    var top_y = this.y - this.halfBallSize;
    if (top_y <= 0) {
        this.y = this.halfBallSize;
        this.bounceWall(this.court);
        return 0;
    }

    // check for hitting bottom wall
    var bottom_y = this.y + this.halfBallSize;
    if (bottom_y >= this.court.height) {
        this.y = this.court.height - this.halfBallSize;
        this.bounceWall(this.court);
        return 0;
    }

    if (this.x_speed < 0) { // Going left
        // touching or behind paddle
        if ((this.x <= this.court.paddles[0].frontX)) {
            // Check for exiting court left - using the middle of the ball to calculate that
            if (this.x < 0) {
                return -1;
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
                return 1;
            }

            // was in front off, and now touching or behind paddle
            if ((oldX < this.court.paddles[1].frontX) && (bottom_y > this.court.paddles[1].y) &&
                (top_y < (this.court.paddles[1].y + this.court.paddles[1].height))) {
                this.bouncePaddle(this.court.paddles[1]);
            }
        }
    }

    return 0;
};

Ball.prototype.clear = function () {
    this.context.clearRect(Math.floor(this.x) - this.halfBallSize,
        Math.floor(this.y) - this.halfBallSize,
        this.ballSize, this.ballSize);
};

Ball.prototype.draw = function () {
    this.context.fillRect(Math.floor(this.x) - this.halfBallSize,
        Math.floor(this.y) - this.halfBallSize,
        this.ballSize, this.ballSize);
};

//////////////////////////////////// GAME ////////////////////////////////
function Game(court) {
    console.log("New Game");
    this.court = court;
    this.court.game = this;
    this.pointsToWin = 21;

    // Create a new ball in the center of the court - Moving
    this.court.ball = new Ball(this.court, this.court.ballSize, this.court.context, this.court.courtColor, this.court.ballColor);

    // add players until enough for a game (2)
    while (this.court.numPlayers < 2) {
        this.court.enter(new ComputerPlayer(this.court, "Computer"));
    }

    this.court.players[0].score = 0;
    this.court.players[1].score = 0;

    // draw initial scoreboard
    this.court.scoreboard.draw();
}

Game.prototype.point = function (player, opponent) {
    console.log("Point for player: " + player.name);

    this.court.scoreboard.pointWon(player);

    if (player.score == this.pointsToWin) {
        this.end(player, opponent);
    } else {
        // Create a new ball in the center of the court - Moving
        this.court.ball = new Ball(this.court, this.court.ballSize, this.court.context, this.court.courtColor, this.court.ballColor);
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
    var pointWon = new Audio('point.mp3');
    pointWon.play();

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
function Court(canvas) {
    this.context = canvas.getContext('2d');
    this.courtColor = "#999999";

    this.width = canvas.width;
    this.height = canvas.height;

    // Draw court initially
    this.context.fillStyle = this.courtColor;
    this.context.fillRect(0, 0, this.width, this.height);
    // set the fill for ball and paddles from now on
    this.context.fillStyle = "#FFFFFF";

    var paddleWidth = 10;
    var paddleHeight = 50;
    var paddleXOffset = 60;
    var courtMiddleY = Math.floor((this.height - paddleHeight) / 2);

    this.paddles = new Array(2);
    this.paddles[0] = new Paddle(paddleXOffset, paddleXOffset + paddleWidth, courtMiddleY, paddleWidth, paddleHeight, this.height, this.context, "#FFFFFF");
    var front = this.width - paddleXOffset - paddleWidth;
    this.paddles[1] = new Paddle(front, front, courtMiddleY, paddleWidth, paddleHeight, this.height, this.context, "#FFFFFF");

    // Create a new ball in the center of the court - not moving
    this.ballSize = 10;
    this.ballColor = "#FFFFFF";

    this.scoreboard = new ScoreBoard(this, document.getElementById("scoreboard"));

    this.paused = false;

    this.players = new Array(2);
    this.players[0] = null;
    this.players[1] = null;
    this.numPlayers = 0;
    this.game = null;

    this.courtMessage();

    // Install into the global window object
    window.court = this;
}

Court.prototype.courtMessage = function () {
    if (this.numPlayers > 0) {
        window.message(window.court.startMessage);
    } else {
        window.message(this.enterMessage);
    }
};

Court.prototype.bounce = function () {
    var out = new Audio('out.mp3');
    out.play();
};

Court.prototype.enter = function (player) {
    var response;

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

Court.prototype.leave = function (looser) {
    console.log("Player '" + looser.name + "' has left the court");

    if (looser == this.players[0]) {
        winner = this.players[1];
        this.players[0] = null;
    } else {
        winner = this.players[0];
        this.players[1] = null;
    }

    if (this.game) {
        this.game.end(winner, looser);
    } else {
        window.message(this.enterMessage);
    }

    // and remove from the court
    this.numPlayers--;

    this.ball.clear();

    // Reclaim his paddle
    looser.paddle.clear();
    looser.paddle = null;
};

Court.prototype.draw = function () {
    // update paddle positions
    if (this.players[0]) {
        var move = this.players[0].updatePaddle(this.ball);
        if (move != 0) {
            this.paddles[0].clear();
            this.paddles[0].move(move);
        }
    }

    if (this.players[1]) {
        var move = this.players[1].updatePaddle(this.ball);
        if (move != 0) {
            this.paddles[1].clear();
            this.paddles[1].move(move);
        }
    }

    if (window.debug > 1) {
        console.log('    to move paddle took ' + (performance.now() - window.start) + ' ms');
    }

    if (this.ball) {
        this.ball.clear();

        // Update the ball position and detect if it has exited one end of the court or another
        var result = this.ball.update();

        if (result != 0) {
            if (result == -1)
                this.game.point(this.players[1], this.players[0]);
            else
                this.game.point(this.players[0], this.players[1]);
        }

        // Draw the ball at the new position
        this.ball.draw();
        if (window.debug > 1) {
            console.log('    to draw ball took ' + (performance.now() - window.start) + ' ms');
        }

        // Draw them after the ball may have deleted a part of them
        this.paddles[0].draw();
        this.paddles[1].draw();
        if (window.debug > 1) {
            console.log('    to draw paddles took ' + (performance.now() - window.start) + ' ms');
        }
    }
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

// This will be called from window on refresh
Court.prototype.update = function () {
    if (!window.court.paused) {
        if (window.debug) {
            console.log('time since last update ' + (performance.now() - window.start) + ' ms');
        }
        window.start = performance.now();

        window.court.draw(); // my drawing routing

        var end = performance.now();
        if (window.debug) {
            console.log('to exit court.draw() took ' + (end - window.start) + ' ms');
        }

        // reschedule next animation update
        window.requestAnimationFrame(window.court.update);
    }
};

window.enableDebug = function enableDebug() {
    window.debug = 1;
};

window.enableStats = function enableStats() {
    window.stats = true;
};