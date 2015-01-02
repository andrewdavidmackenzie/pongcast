//////////////////////////////////// PADDLE ////////////////////////////////
function Paddle(x, y, width, height, courtHeight, context, soundFileName, paddleColor) {
    this.defaultSpeed = Math.floor((2 * courtHeight) / 100); // 2% of the height
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.halfHeight = Math.floor(this.height / 2);
    this.context = context;
    this.speed = 0;
    this.maxY = courtHeight - this.height;
    this.bounceSound = new Audio(soundFileName);
    this.color = paddleColor;
}

Paddle.prototype.bounce = function () {
    this.bounceSound.play();
};

Paddle.prototype.clear = function () {
    this.context.clearRect(this.x, this.y, this.width, this.height);
};

Paddle.prototype.draw = function () {
    this.context.fillStyle = this.color;
    this.context.fillRect(this.x, this.y, this.width, this.height);
};

Paddle.prototype.moveUp = function () {
    this.move(-this.defaultSpeed);
};

Paddle.prototype.moveDown = function () {
    this.move(this.defaultSpeed);
};

Paddle.prototype.stop = function () {
    this.speed = 0;
};

Paddle.prototype.move = function (distance) {
    this.y += distance;
    this.speed = distance;

    // Stop at the top of the court
    if (this.y <= 0) {
        this.y = 0;
        this.speed = 0;
    } else if (this.y >= this.maxY) {
        this.y = this.maxY;
        this.speed = 0;
    }
};

//////////////////////////////////// PADDLE CONTROLLER ////////////////////////////////
function PaddleController() {
}

PaddleController.prototype.updatePaddle = function (court, paddle, ball) {
    var diff = paddle.y + paddle.halfHeight - ball.y;
    paddle.move(Math.floor(-(diff * 3) / 4));
};

//////////////////////////////////// PLAYER ////////////////////////////////
function Player(name, court, controller) {
    this.name = name;
    this.court = court;
    this.controller = controller;
}

Player.prototype.givePaddle = function (paddle) {
    this.paddle = paddle;
};

Player.prototype.updatePaddle = function (ball) {
    this.controller.updatePaddle(this.court, this.paddle, ball);
};

//////////////////////////////////// BALL ////////////////////////////////
function Ball(court, ballSize, context, courtColor) {
    this.defaultSpeed = Math.floor(court.width / 200);
    this.court = court;
    this.ballSize = ballSize;
    this.halfBallSize = Math.floor(ballSize / 2);
    this.context = context;
    this.courtColor = courtColor;
    this.x = Math.floor(this.court.width / 2);
    this.y = Math.floor(this.court.height / 2);
    this.y_speed = 0;
    this.x_speed = this.defaultSpeed;
}

Ball.prototype.render = function (color) {
    this.context.fillStyle = color;
    this.context.fillRect(this.x - this.halfBallSize, this.y - this.halfBallSize, this.ballSize, this.ballSize);
};

Ball.prototype.bounceWall = function (court) {
    this.y_speed = -this.y_speed;
    court.bounce();
};

Ball.prototype.bouncePaddle = function (paddle) {
    this.x_speed = -this.x_speed;
    this.y_speed += Math.floor(paddle.speed / 2);
    paddle.bounce();
};

Ball.prototype.update = function () {
    this.render(this.courtColor);

    // update position according to its speed
    this.x += this.x_speed;
    this.y += this.y_speed;

    var top_y = this.y - this.halfBallSize;
    // If hits the top wall
    if (top_y <= 0) {
        this.y = this.halfBallSize;
        this.bounceWall(this.court);
        return 0;
    }

    var bottom_y = this.y + this.halfBallSize;
    // if hits bottom wall
    if (bottom_y >= this.court.height) {
        this.y = this.court.height - this.halfBallSize;
        this.bounceWall(this.court);
        return 0;
    }

    if (this.x_speed < 0) { // Going left
        if ((this.x < this.court.halfWidth)) { // In left half of the court?
            // if leaves the court at the left
            if (this.x < 0) {
                return -1;
            }

            if ((this.x <= this.court.paddles[0].x + this.court.paddles[0].width) &&
                (bottom_y > this.court.paddles[0].y) &&
                (top_y < (this.court.paddles[0].y + this.court.paddles[0].height))) {
                this.bouncePaddle(this.court.paddles[0]);
            }
        }
    } else { // Going right
        if ((this.x > this.court.halfWidth)) {
            // if leaves the court at the right
            if (this.x > this.court.width) {
                return 1;
            }

            if ((this.x >= this.court.paddles[1].x) &&
                (bottom_y > this.court.paddles[1].y) &&
                (top_y < (this.court.paddles[1].y + this.court.paddles[1].height))) {
                this.bouncePaddle(this.court.paddles[1]);
            }
        }
    }

    return 0;
};

//////////////////////////////////// GAME ////////////////////////////////
function Game(court) {
    window.outputLine("New game created on court");
    this.court = court;
    this.court.game = this;

    // Create a new ball in the center of the court - Moving
    this.court.ball = new Ball(this.court, this.court.ballSize, this.court.context, this.court.courtColor);

    // TODO find a game over sound and load it here
//    this.gameWon = new Audio('game.ogg');

    this.state = "ready";
}

Game.prototype.start = function () {
    // when requested to start, add players until enough for a game (2)
    while (this.court.numPlayers < 2) {
        this.court.enter(new Player("Computer", this, new PaddleController()));
    }

    this.court.players[0].score = 0;
    this.court.players[1].score = 0;

    // draw initial scoreboard
    this.court.scoreboard.render('ffffff');

    this.state = "in play";
};

Game.prototype.point = function (player) {
    window.outputLine("Point for player: " + player.name);

    this.court.scoreboard.pointWon(player);

    if (player.score == 21) {
        this.end(player);
    } else {
        // Create a new ball in the center of the court - Moving
        this.court.ball = new Ball(this.court, this.court.ballSize, this.court.context, this.court.courtColor);
    }
};

Game.prototype.end = function (winner) {
    window.outputLine("End of game. " + winner.name + " wins");
    // TODO find this sound then enable
//    this.gameWon.play();
    this.court.game = null;
    this.court.pause();
};

//////////////////////////////////// SCOREBOARD ////////////////////////////////
function ScoreBoard(court, textColor) {
    this.court = court;
    this.context = court.context;
    this.textColor = textColor;
    this.fontHeight = Math.floor((court.height * 10) / 100); // 10% of court height
    this.fontBaseline = 20 + this.fontHeight; // 20 down from the top
    // This should be gotten from font metrics!
    this.fontWidth = 50;
    this.score1X = Math.floor((court.width / 2) - (2 * this.fontWidth));
    this.score2X = Math.floor((court.width / 2) + (2 * this.fontWidth));
    this.pointWonSound = new Audio('point.ogg');
}

ScoreBoard.prototype.pointWon = function (player) {
    // Play point won sound
    this.pointWonSound.play();

    // clear the scoreboard
    this.context.clearRect(this.score1X - (2 * this.fontWidth), 20, 8 * this.fontWidth, this.fontHeight +1);

    // increment score of that player
    player.score++;

    // draw new score
    this.render(this.textColor);
};

ScoreBoard.prototype.render = function (color) {
    // Set text font
    this.context.font = this.fontHeight + 'px Pong';
    this.context.color = color;
    this.context.fillStyle = color;
    this.context.textAlign = 'right';
    this.context.fillText(this.court.players[0].score.toString(), this.score1X, this.fontBaseline);
    this.context.textAlign = 'left';
    this.context.fillText(this.court.players[1].score.toString(), this.score2X, this.fontBaseline);
};

//////////////////////////////////// COURT ////////////////////////////////
function Court(canvas, stats) {
    this.context = canvas.getContext('2d');
    this.courtColor = "#999999";

    this.width = canvas.width;
    this.height = canvas.height;
    this.halfWidth = Math.floor(this.width / 2);

    // Draw court initially
    this.context.fillStyle = this.courtColor;
    this.context.fillRect(0, 0, this.width, this.height);

    var paddleWidth = 10;
    var paddleHeight = 50;
    var paddleXOffset = 60;
    var courtMiddleY = Math.floor((this.height - paddleHeight) / 2);

    this.paddles = new Array(2);
    this.paddles[0] = new Paddle(paddleXOffset, courtMiddleY, paddleWidth, paddleHeight, this.height, this.context, 'paddle.ogg', "#FFFFFF");
    this.paddles[0].draw();
    this.paddles[1] = new Paddle(this.width - paddleXOffset - paddleWidth, courtMiddleY, paddleWidth, paddleHeight, this.height, this.context, 'paddle.ogg', "#FFFFFF");
    this.paddles[1].draw();

    // Create a new ball in the center of the court - not moving
    this.ballSize = 10;
    this.ballColor = "#FFFFFF";
    this.ball = new Ball(this, this.ballSize, this.context, this.courtColor);

    this.scoreboard = new ScoreBoard(this, "#FFFFFF");

    this.bounceSound = new Audio('court.ogg');

    this.stats = stats;
    this.paused = false;

    this.players = new Array(2);
    this.players[0] = null;
    this.players[1] = null;
    this.numPlayers = 0;
    this.game = null;

    // Install into the global window object
    window.court = this;
}

Court.prototype.bounce = function () {
    this.bounceSound.play();
};

Court.prototype.enter = function (player) {
    if (this.players[0] == null) {
        this.players[0] = player;
        this.players[0].givePaddle(this.paddles[0]);
        this.numPlayers++;
        window.outputLine("Player 0 (" + player.name + ") gets left paddle");
        return "LEFT(RIGHT)";
    }

    if (this.players[1] == null) {
        this.players[1] = player;
        this.players[1].givePaddle(this.paddles[1]);
        this.numPlayers++;
        window.outputLine("Player 1 (" + player.name + ") gets right paddle");
        return "RIGHT";
    }

    window.outputLine("Court full");
    return "Court full";
};

Court.prototype.render = function () {
    this.ball.render(this.ballColor);
    this.paddles[0].draw();
    this.paddles[0].draw();
};

Court.prototype.update = function () {
    this.paddles[0].clear();
    this.paddles[1].clear();

    this.players[0].updatePaddle(this.ball);
    this.players[1].updatePaddle(this.ball);

    // draw paddles at new positions
    this.paddles[0].draw();
    this.paddles[1].draw();

    // Update the ball position and detect if it has exited one end of the court or another
    var result = this.ball.update();

    if (result != 0) {
        if (result == -1)
            this.game.point(this.players[1]);
        else
            this.game.point(this.players[0]);
    }

    this.render();
};

Court.prototype.startPlay = function () {
    if (this.game == null) {
        this.game = new Game(this);
    }

    if (this.game.state == "ready") {
        this.game.start();
    }

    window.outputLine("Starting play");
    this.play();
};

Court.prototype.play = function () {
    if (this.stats) {
        this.stats.begin();
    }

    // Update the positions of the objects - not sure why "this.update()" doesn't work
    window.court.update();

    if (this.stats) {
        this.stats.end();
    }

    // reschedule next animation update
    this.animFrame = window.requestAnimationFrame(window.court.play);
};

Court.prototype.pause = function () {
    window.cancelAnimationFrame(this.animFrame);
};

Court.prototype.toggle = function () {
    if (this.paused) {
        this.paused = false;
        window.outputLine("Play restarted");
        this.play();
    } else {
        this.paused = true;
        window.outputLine("Play paused");
        this.pause();
    }
};

window.onload = function () {
    var canvas = document.getElementById('court');
    window.court = new Court(canvas, window.stats);
    window.court.render();
};