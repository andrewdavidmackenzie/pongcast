//////////////////////////////////// PADDLE ////////////////////////////////
function Paddle(x, y, width, height, courtHeight, color, context) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    this.courtHeight = courtHeight;
    this.color = color;
    this.context = context;
    this.speed = 0;
}

Paddle.prototype.render = function () {
    this.context.fillStyle = this.color;
    this.context.fillRect(this.x, this.y, this.width, this.height);
}

Paddle.prototype.move = function (distance) {
    this.y += distance;
    this.speed = distance;

    if (this.y < 0) {
        this.y = 0;
        this.speed = 0;
    } else if (this.y + this.height > this.courtHeight) {
        this.x = this.courtHeight - this.height;
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


//////////////////////////////////// COMPUTER ////////////////////////////////
function Computer(name) {
    BasePlayer.call(name);
}

Computer.prototype.constructor = Computer;
Computer.prototype = new BasePlayer(name);

Computer.prototype.updatePaddle = function (ball) {
    var diff = -((this.paddle.y + (this.paddle.height / 2)) - ball.y);
    if (diff < 0 && diff < -4) {
        diff = -5;
    } else if (diff > 0 && diff > 4) {
        diff = 5;
    }

    this.paddle.move(diff);
}

//////////////////////////////////// BALL ////////////////////////////////
function Ball(width, height, ballSize, context) {
    this.width = width;
    this.height = height;
    this.ballSize = ballSize;
    this.context = context;
    this.reset(width / 2, height / 2);
}

// Reset the speed of the ball in X and Y to the default values
// and the position to the specified x and y
Ball.prototype.reset = function (x, y) {
    this.x = x;
    this.y = y;
    this.y_speed = 0;
    this.x_speed = 3;
}

Ball.prototype.render = function () {
    this.context.beginPath();
    this.context.arc(this.x, this.y, this.ballSize, 2 * Math.PI, false);
    this.context.fillStyle = this.ballColor;
    this.context.fill();
}

Ball.prototype.update = function (paddle1, paddle2) {
    // update position according to its speed
    this.x += this.x_speed;
    this.y += this.y_speed;

    // Calculate ball limits
    var top_x = this.x - this.ballSize;
    var top_y = this.y - this.ballSize;
    var bottom_x = this.x + this.ballSize;
    var bottom_y = this.y + this.ballSize;

    // If hits the top wall
    if (top_y < 0) {
        this.y = this.ballSize;
        // Bounce
        this.y_speed = -this.y_speed;
    } else
    // if hits bottom wall
    if (bottom_y > this.height) {
        this.y = this.height - this.ballSize;
        // Bounce
        this.y_speed = -this.y_speed;
    }

    // if leaves the court at the left
    if (this.x < 0) {
        // TODO New point for player on right
        this.reset(this.width / 2, this.height / 2);
    }

    // if leaves the court at the right
    if (this.x > this.width) {
        // TODO new point for player on left
        this.reset(this.width / 2, this.height / 2);
    }

    // Check for bounces of the paddles
    if (top_x < (paddle1.x + paddle1.width) && bottom_x > paddle1.x && top_y < (paddle1.y + paddle1.height) && bottom_y > paddle1.y) {
        // Bounce of paddle 1
        this.x_speed = -this.x_speed;
        // Pick up later speed from the paddle
        this.y_speed += (paddle1.speed / 2);
        // Advance it
        this.x += this.x_speed;
    }
    else {
        if (top_x < (paddle2.x + paddle2.width) && bottom_x > paddle2.x && top_y < (paddle2.y + paddle2.height) && bottom_y > paddle2.y) {
            // Bounce of paddle 1
            this.x_speed = -this.x_speed;
            // Pick up later speed from the paddle
            this.y_speed += (paddle2.speed / 2);
            // Advance it
            this.y += this.y_speed;
        }
    }
}

//////////////////////////////////// COURT ////////////////////////////////
function Court(canvas) {
    this.ballSize = 5;
    this.ballColor = "#000000";
    this.courtColor = "#FF00FF";

    var paddleWidth = 10;
    var paddleHeight = 50;
    var paddleXOffset = 10;
    var paddleColor = "#0000FF";

    this.context = canvas.getContext('2d');
    this.context.canvas.width  = window.innerWidth;
    this.context.canvas.height = window.innerHeight;

    this.width = canvas.width;
    this.height = canvas.height;

    var courtMiddleY = (this.height / 2) - (paddleHeight / 2);
    this.paddle1 = new Paddle(paddleXOffset, courtMiddleY, paddleWidth, paddleHeight, this.width, paddleColor, this.context);
    this.paddle2 = new Paddle(this.width - (paddleXOffset + paddleWidth), courtMiddleY, paddleWidth, paddleHeight, this.width, paddleColor, this.context);

    // Create a new ball in the center of the court - not moving
    this.ball = new Ball(this.width, this.height, this.ballSize, this.context);

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
    this.player2.updatePaddle(this.ball);
    this.ball.update(this.paddle1, this.paddle2);
}

Court.prototype.clean = function () {
    this.context.fillStyle = this.courtColor;
    this.context.fillRect(0, 0, this.width, this.height);
}

Court.prototype.render = function () {
    this.clean();
    this.paddle1.render();
    this.paddle2.render();
    this.ball.render();
    // TODO Render the score card
}

Court.prototype.step = function () {
    // Update the positions of the objects
    window.court.update();

    // draw them
    window.court.render();

    // reschedule next animation update
    window.animater(window.court.step);
}

//////////////////////////////////// GAME ////////////////////////////////
function Game(court) {
    this.court = court;
    this.court.player1.score = 0;
    this.court.player2.score = 0;
    this.court.game = this;

    // Create a new ball in the center of the court - Moving
    this.court.ball = new Ball(this.court.width, this.court.height, this.court.ballSize, this.court.context);
}

// Only start the game if one or two players - second would be computer
// Supply maximum number of poins for the game
Game.prototype.start = function () {
    // Start the updates
    window.animater(window.court.step);
}

Game.prototype.point = function (player) {
}

Game.prototype.end = function () {
}

animater = window.requestAnimationFrame || window.webkitRequestAnimationFrame || window.mozRequestAnimationFrame || function (callback) {
    window.setTimeout(callback, 1000 / 60)
};

