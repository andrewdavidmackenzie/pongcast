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

##Chromecast Events
The following is the list of events that can be handled by the chromecast receiver and what I do in each case:

* Ready
	+ Set the application state as "Ready for players..."

* SenderConnected
	+ Have the player enter the court immediately on connection.
	+ Respond to them if they got a paddle to play with and if so which one

* SenderDisconnected
	+ Have the player leave the court, forfeiting any game in progress.
	+ If no-one is left on the court, close the window and end the session.

* VisibilityChanged
	+ Nothing. Doesn't seem to trigger on my TV when I change input source.

* OnMessage
	+ Handle it in the game logic.
	Messages Possible:
		+ Start play
		+ Pause play
		+ Move Paddle

## Court and Game control
The pong.html file of the receiver finds the canvas element and draws the Court with the two paddles in it
and prepares the Scoreboard elements.

It then starts one or more of the Controllers (KeyboardController or CastController).

The Court object is ready and and waits for someone to request to enter the court. When someone requests to enter, if
there is space (2 players max) then they are allowed to enter.

A player requests the Court to start play using "court.startPlay()".
If there is no Game prepared on the Court, then one is created with a new Ball.
If there is only one Player on court then a ComputerPlayer is added.
The scores are reset to zero, and the Scoreboard drawn.
The Game is then started, and updating is started.

##Player Requests (to the receiver from clients) and possible responses
- Enter court (Connecting a client to the receiver)
	-> OK
	-> Court full
- Leave the Courts - forfeits the game if in play
- Start a Game (if they are on court and it's not started already)
	-> OK
	-> Already started
- Pause Play
- Restart Play
- Toggle Play
- Move Paddle (the new position of the paddle will get read on next refresh)

##Other Events
- Loss of a Player (connection to a client) - forfeits the game if in play

##Objects and calls
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