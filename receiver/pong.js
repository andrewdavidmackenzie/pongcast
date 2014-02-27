/**
 * Created by andrew on 24/02/14.
 *
 * See http://robots.thoughtbot.com/pong-clone-in-javascript
 */

//////////////////////////////////// PADDLE ////////////////////////////////
function Paddle(x, y, width, height, courtWidth, context) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.courtWidth = courtWidth;
    this.context = context;
    this.x_speed = 0;
    this.y_speed = 0;
}

Paddle.prototype.render = function () {
    this.context.fillStyle = paddleColor;
    this.context.fillRect(this.x, this.y, this.width, this.height);
}

Paddle.prototype.move = function (x, y) {
    this.x += x;
    this.y += y;
    this.x_speed = x;
    this.y_speed = y;

    if (this.x < 0) {
        this.x = 0;
        this.x_speed = 0;
    } else if (this.x + this.width > this.courtWidth) {
        this.x = this.courtWidth - this.width;
        this.x_speed = 0;
    }
}

//////////////////////////////////// BASE PLAYER ////////////////////////////////
function BasePlayer(name) {
    this.name = name;
}

BasePlayer.prototype.givePaddle = function(paddle) {
    this.paddle = paddle;
}

//////////////////////////////////// PLAYER ////////////////////////////////
function Player(name) {
    BasePlayer.call(name);
}
Player.prototype.constructor = Player;
Player.prototype = new BasePlayer(name);


//////////////////////////////////// COMPUTER ////////////////////////////////
function Computer(name) {
    BasePlayer.call(name);
}

Computer.prototype.constructor = Computer;
Computer.prototype = new BasePlayer(name);

Computer.prototype.updatePaddle = function (ball) {
    var diff = -((this.paddle.x + (this.paddle.width / 2)) - ball.x);
    if (diff < 0 && diff < -4) {
        diff = -5;
    } else if (diff > 0 && diff > 4) {
        diff = 5;
    }

    this.paddle.move(diff, 0);
}

//////////////////////////////////// BALL ////////////////////////////////
function Ball(x, y, context) {
    this.width = x;
    this.height = y;
    this.context = context;
    this.reset(x/2, y/2);
}

// Reset the speed of the ball in X and Y to the default values
// and the position to the specified x and y
Ball.prototype.reset = function (x, y) {
    this.x = x;
    this.y = y;
    this.x_speed = 0;
    this.y_speed = 3;
}

Ball.prototype.render = function () {
    this.context.beginPath();
    this.context.arc(this.x, this.y, ballSize, 2 * Math.PI, false);
    this.context.fillStyle = ballColor;
    this.context.fill();
}

Ball.prototype.update = function (paddle1, paddle2) {
    this.x += this.x_speed;
    this.y += this.y_speed;
    var top_x = this.x - ballSize;
    var top_y = this.y - ballSize;
    var bottom_x = this.x + ballSize;
    var bottom_y = this.y + ballSize;

    // If hits the left wall
    if (this.x - ballSize < 0) {
        this.x = ballSize;
        this.x_speed = -this.x_speed;
    } else
    // if hits right wall
    if (this.x + ballSize > this.width) {
        this.x = this.width - ballSize;
        this.x_speed = -this.x_speed;
    }

    // if leaves the court at the top
    if (this.y < 0) {
        this.reset(this.width / 2, this.height / 2);
    }

    // if leaves the court at the bottom
    if (this.y > this.height) {
        this.reset(this.width / 2, this.height / 2);
    }

    if (top_y > 300) {
        if (top_y < (paddle1.y + paddle1.height) && bottom_y > paddle1.y && top_x < (paddle1.x + paddle1.width) && bottom_x > paddle1.x) {
            this.y_speed = -3;
            this.x_speed += (paddle1.x_speed / 2);
            this.y += this.y_speed;
        }
    } else {
        if (top_y < (paddle2.y + paddle2.height) && bottom_y > paddle2.y && top_x < (paddle2.x + paddle2.width) && bottom_x > paddle2.x) {
            this.y_speed = 3;
            this.x_speed += (paddle2.x_speed / 2);
            this.y += this.y_speed;
        }
    }
}

//////////////////////////////////// COURT ////////////////////////////////
// TODO

//////////////////////////////////// GAME ////////////////////////////////
function Game(canvas) {
    window.game = this;
    this.width = canvas.width;
    this.height = canvas.height;
    this.context = canvas.getContext('2d');
    this.paddle1 = new Paddle((this.width / 2) - (paddleWidth / 2), this.height - (paddleYOffset + paddleHeight), paddleWidth, paddleHeight, this.width, this.context);
    this.paddle2 = new Paddle((this.width / 2) - (paddleWidth / 2), paddleYOffset, paddleWidth, paddleHeight, this.width, this.context);

    // Start off ready for demo mode with two computer players
    this.setPlayer1(new Computer("Demo1"));
    this.setPlayer2(new Computer("Demo2"));
}

Game.prototype.setPlayer1 = function(player) {
    this.player1 = player;
    this.player1.givePaddle(this.paddle1);
}

Game.prototype.setPlayer2 = function(computer) {
    this.player2 = computer;
    this.player2.givePaddle(this.paddle2);
}

// Only start the game if one or two players - second would be computer
// Supply maximum number of poins for the game
Game.prototype.start = function () {
    this.player1.score = 0;
    this.player2.score = 0;

    // Create a new ball in the center of the court
    this.ball = new Ball(this.width, this.height, this.context);

    // Start the updates
    animate(window.game.step);
}

Game.prototype.step = function() {
    // Update the positions of the objects
    window.game.update();
    // draw them
    window.game.render();
    // reschedule next animation update
    animate(window.game.step);
}

Game.prototype.update = function () {
    this.player2.updatePaddle(this.ball);
    this.ball.update(this.paddle1, this.paddle2);
}

Game.prototype.cleanCourt = function () {
    this.context.fillStyle = courtColor;
    this.context.fillRect(0, 0, this.width, this.height);
}

Game.prototype.render = function () {
    this.cleanCourt();
    this.paddle1.render();
    this.paddle2.render();
    this.ball.render();
}

animate = window.requestAnimationFrame || window.webkitRequestAnimationFrame || window.mozRequestAnimationFrame || function (callback) {
    window.setTimeout(callback, 1000 / fps)
};