#Main Components
The Main components are:

receiver - this is the app that runs on the chromecast
android  - this is the client app that a user uses to connect to the chromecast to have their mobile device act
           as a controller.


##Receiver
The receiver consists of the following Javascript files:
pong.js - implements the game logic. At startup it creates the Court for the game, with a Scoreboard, and Paddles
          ready to be used by Players. It also instantiates a keyboard controller or chromecast controller.
keyboardController.js - the code for reading the keyboard and allowing it to act as a game controller.
                        It introduces a Player called "Keyboard" into the Court
These are the commands that are sent between a PaddleController (Keyboard, Chromecast, etc) and the Game app.

##Main Objects
Court- the area where the Game is played. Holds two Paddles, two Players the Ball and the Scorecard. It gets updated
       on each animation frame.
Game - represents the game that two Players on the Court with a Ball. It holds the two scores of the two players and
       ends when one of them gets 21 points and wins the Game.
Player- gets a Paddle to play with, then can move it as they wish to play.
Paddle - represents the Players "bat" and can be moved up and down. The Ball bounces of it.
Ball- the ball moves about the Court, bouncing of top and bottom walls and the bat, at a speed determined by impacts.
Scorecard - is drawn on the Court and shows the score of the two Players.

KeyboardPlayer - subclasses Player to act as a Player, controlled by the keyboard
ComputerPlayer - subclasses Player to be an automatic player that tries to always return the ball.


##Interactions
Court
	Enter(Player)   - request to enter the court, supplying your player object
		-> If successful, player will get given a paddle via Player.givePaddle(Paddle)

	Leave(Player) TODO

Player must implement this method
	updatePaddle
		-> In this method it should request to move it's paddle using
			Paddle.move(distance)
			Paddle.moveUp()
			Paddle.moveDown()
			Paddle.stop()