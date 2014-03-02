//////////////////////////////////// PADDLE ////////////////////////////////
function Paddle(x, y, width, height, courtHeight, context, courtColor) {
    this.defaultSpeed = (2 * courtHeight) / 100; // 2% of the height
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.halfHeight = this.height / 2;
    this.context = context;
    this.speed = 0;
    this.maxY = courtHeight - this.height;
    this.courtColor = courtColor;
}

Paddle.prototype.render = function (color) {
    this.context.fillStyle = color;
    this.context.fillRect(this.x, this.y, this.width, this.height);
}

Paddle.prototype.move = function (distance) {
    // Delete the old one by drawing over it in the background color
    this.render(this.courtColor);

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
}

//////////////////////////////////// BASE PLAYER ////////////////////////////////
function BasePlayer(name) {
    this.name = name;
    this.score = 0;
}

BasePlayer.prototype.givePaddle = function (paddle) {
    this.paddle = paddle;
}

//////////////////////////////////// PLAYER ////////////////////////////////
function Player(name) {
    BasePlayer.call(name);
}
Player.prototype.constructor = Player;
Player.prototype = new BasePlayer(name);

Player.prototype.updatePaddle = function (ball) {
    // NULL
}

//////////////////////////////////// COMPUTER ////////////////////////////////
function Computer(name) {
    BasePlayer.call(name);
}

Computer.prototype.constructor = Computer;
Computer.prototype = new BasePlayer(name);

Computer.prototype.updatePaddle = function (ball) {
    /*
     var ballSign = ball.x_speed ? ball.x_speed < 0 ? -1 : 1 : 0;
     var offset = this.paddle.x - ball.x;
     var offsetSign = offset ? offset < 0 ? -1 : 1 : 0;

     If coming towards me follow it. Until then tend to center.
     */

    var diff = this.paddle.y + this.paddle.halfHeight - ball.y;
    this.paddle.move(-diff);
}

//////////////////////////////////// BALL ////////////////////////////////
function Ball(court, ballSize, context, courtColor) {
    this.defaultSpeed = court.width / 200;
    this.court = court;
    this.ballSize = ballSize;
    this.halfBallSize = ballSize / 2;
    this.context = context;
    this.courtColor = courtColor;
    this.x = this.court.width / 2;
    this.y = this.court.height / 2;
    this.y_speed = 0;
    this.x_speed = this.defaultSpeed;
}

Ball.prototype.render = function (color) {
    this.context.beginPath();
    this.context.rect(this.x - this.halfBallSize, this.y - this.halfBallSize, this.ballSize, this.ballSize);
    this.context.fillStyle = color;
    this.context.fill();
}

Ball.prototype.bounceWall = function () {
    this.y_speed = -this.y_speed;
    // TODO sound here
}

Ball.prototype.bouncePaddle = function (paddleSpeed) {
    this.x_speed = -this.x_speed;
    this.y_speed += (paddleSpeed / 2);
    // TODO sound here
}

Ball.prototype.update = function () {
    // Delete the old one
    this.render(this.courtColor);

    // update position according to its speed
    this.x += this.x_speed;
    this.y += this.y_speed;

    var top_y = this.y - this.halfBallSize;
    // If hits the top wall
    if (top_y <= 0) {
        this.y = this.halfBallSize;
        this.bounceWall();
        return 0;
    }

    var bottom_y = this.y + this.halfBallSize;
    // if hits bottom wall
    if (bottom_y >= this.court.height) {
        this.y = this.court.height - this.halfBallSize;
        this.bounceWall();
        return 0;
    }

    if (this.x_speed < 0) { // Going left
        // if leaves the court at the left
        if (this.x + this.halfBallSize < 0) {
            return -1;
        }

        if ((this.x < this.court.halfWidth)) { // In left half of the court?
            var left_x = this.x - this.halfBallSize;
            if ((left_x <= this.court.paddle1.x + this.court.paddle1.width) &&
                (bottom_y > this.court.paddle1.y) &&
                (top_y < (this.court.paddle1.y + this.court.paddle1.height))) {
                this.bouncePaddle(this.court.paddle1.speed);
            }
        }
    } else { // Going right
        // if leaves the court at the right
        if (this.x - this.halfBallSize > this.court.width) {
            return 1;
        }

        if ((this.x > this.court.halfWidth)) {
            var right_x = this.x + this.halfBallSize;
            if ((right_x >= this.court.paddle2.x) &&
                (bottom_y > this.court.paddle2.y) &&
                (top_y < (this.court.paddle2.y + this.court.paddle2.height))) {
                this.bouncePaddle(this.court.paddle2.speed);
            }
        }
    }

    return 0;
}

//////////////////////////////////// COURT ////////////////////////////////
function Court(canvas) {
    this.context = canvas.getContext('2d');
    this.context.canvas.width = window.innerWidth;
    this.context.canvas.height = window.innerHeight;
    this.courtColor = "#999999"

    // Draw court initially
    this.context.fillStyle = this.courtColor;
    this.context.fillRect(0, 0, this.width, this.height);

    this.width = canvas.width;
    this.height = canvas.height;
    this.halfWidth = this.width / 2;

    var paddleWidth = 10;
    var paddleHeight = 50;
    var paddleXOffset = 10;
    this.paddleColor = "#FFFFFF";
    var courtMiddleY = this.height / 2;
    this.paddle1 = new Paddle(paddleXOffset, courtMiddleY - (paddleHeight / 2), paddleWidth, paddleHeight, this.width, this.context, this.courtColor);
    this.paddle2 = new Paddle(this.width - paddleXOffset - paddleWidth, courtMiddleY - (paddleHeight / 2), paddleWidth, paddleHeight, this.width, this.context, this.courtColor);

    // Create a new ball in the center of the court - not moving
    this.ballSize = 10;
    this.ballColor = "#FFFFFF";
    this.ball = new Ball(this, this.ballSize, this.context, this.courtColor);

    // Start off ready for demo mode with two computer players
    this.setPlayer1(new Computer("Demo1"));
    this.setPlayer2(new Computer("Demo2"));

    // Install into the global window object
    window.court = this;
}

Court.prototype.setPlayer1 = function (player) {
    this.player1 = player;
    this.player1.givePaddle(this.paddle1);
}

Court.prototype.setPlayer2 = function (player) {
    this.player2 = player;
    this.player2.givePaddle(this.paddle2);
}

Court.prototype.update = function () {
    this.player1.updatePaddle(this.ball);
    this.player2.updatePaddle(this.ball);

    var result = this.ball.update();

    if (result != 0) {
        if (result == -1)
            this.game.point(this.game.player1);
        else
            this.game.point(this.game.player2);
    }

    this.render();
}

Court.prototype.render = function () {
    this.paddle1.render(this.paddleColor);
    this.paddle2.render(this.paddleColor);
    this.ball.render(this.ballColor);

    // TODO Render the score card from the game
}

Court.prototype.step = function () {
    if (window.court.game.playing) {
        // Update the positions of the objects
        window.court.update();

        // reschedule next animation update
        window.animater(window.court.step);
    }
}

//////////////////////////////////// GAME ////////////////////////////////
function Game(court) {
    this.court = court;
    this.playing = false;
    this.court.game = this;
}

// Only start the game if one or two players - second would be computer
// Supply maximum number of poins for the game
Game.prototype.start = function () {
    // Pick up the players from the court
    this.player1 = this.court.player1;
    this.player2 = this.court.player2;
    this.player1.score = 0;
    this.player2.score = 0;

    // Create a new ball in the center of the court - Moving
    this.court.ball = new Ball(this.court, this.court.ballSize, this.court.context, this.court.courtColor);

    this.playing = true;

    // Start the updates
    window.animater(window.court.step);
}

Game.prototype.point = function (player) {
    // TODO Point sound

    if (++player.score == 21) {
        this.end(player);
    } else {
        // Create a new ball in the center of the court - Moving
        this.court.ball = new Ball(this.court, this.court.ballSize, this.court.context, this.court.courtColor);
    }
}

Game.prototype.end = function (winner) {
    console.log(winner.name + " wins");

    // TODO End of Game Sounds
    this.playing = false;
}

animater = window.requestAnimationFrame || window.webkitRequestAnimationFrame || window.mozRequestAnimationFrame || function (callback) {
    window.setTimeout(callback, 1000 / 30)
};