pongcast
======

I wanted to learn more about Chromecast development (the receiver that runs on the chromecast itself, 
and the mobile app "controller" and decided that a good way would be to do a simple game. 

Most of the first apps for chromecast I was seeing were video streaming and the like, 
so I thought a multi-player game with two controllers connecting and playing together on the shared
TV screen would be good.

In homage to Pong (which met some birthday near the time and was in the news) I decided to make it pong on chromecast,
taking me right back to the first ever video game I player.

Status
======
* pong.html and JavaScript to create the court and load the game logic (pong.js) and depending on where it's running start a keyboardController or a ChromecastController.
* Core game logic and graphics, this includes a very simple "Computer" player you can play against.
* A keyboard controller that you can use to control a player when the game is running in your browser. Useful for development.
* A chromecast "custom receiver" in JavaScript that acts as the Chromecast controller for the game and which creates ChromePlayers. This allows multiple players to enter the court to play by using the mobile app on their mobile as a game controller, which they connect to the chromecast.
* Android mobile app ("chromecast sender".
  - At the moment this can just connect to the chromecast to enter the court and has a button to start the game.
  - Paddle control still needs to be done.

Areas Needing work
=====
Finish basic paddle control via the Android app ("sender") and ChromecastController ("receiver").

Despite using some CSS scaling tricks, the update rate on the chromecast is still too slow, and it doesn't work smoothly, so that's one area of work.
Javascript/CSS-fu to make it all run quicker and smoother on the chromecast is very welcome!

Also, for some reason I have not been able to get the sound reproduction to synchronize with the events, nor be
reliable - despite using some tricks I found on the web... so help with good sound from JS on Chromecast is also
much needed.

Folders
======
* android - the android app to control the game, just a skeleton at the moment.
* CastCompanionLibrary - see the section below, a library for interacting with Chromecast
* receiver - custom chromecast receiver html, javascript, image and sound files

Hosting your own receiver
=====
Until now I have just been hosting a version of the custom receiver code from a folder in my Google Drive in the cloud, which I have the folder shared publicly. Google Drive is actually quite a good way to host a "statics only" web site.

Recently I have created a "gh-pages" branch on this repo and posted the contents of the ./receiver folder there, effectively hosting the latest version of the custom receiver code on that branch (which also works in your browser with the keyboard controller!) here on git! 

You can see it [here](http://andrewdavidmackenzie.github.io/pongcast/pong.html)
See Keyboard Controll below.

If you fork this repo, you will also get this branch and you should get a hosted version of the game under "your-username".github.io/pongcast/pong.html
You will need to modify the receiver identifier in the android code to refer to the URL where you host you receiver.

TODO - modify the code to use the github hosted one for production, and keep using my Google Drive version for development - testing changes before pushing them to github.

Keyboard Control
=====
Use 'e' to Enter the court.
Use 's' to Start a game once you are in the court.
Use up and down keys on keyboard to control the paddle.
Use spacebar to pause/restart the game.

Scripts
======
deployReceiver.sh - this script just copies the ./receiver folder contents into my Google Drive folder.

Cloning with git submodules
======
The android app uses a git submodule for the CastCompanionLibrary, which I forked from the official one on github,
and added some config files to.

This was to make it easier to track the original, and have a git controlled version of this library that I could
share among multiple Chomecast apps if needed.

When you clone the project initially you will get an empty directory for CastCompanionLibrary.

You must run two commands:
- "git submodule init" to initialize your local configuration file
- "git submodule update" to fetch all the data from that project and check out the appropriate commit listed in your superproject:

Then you should be able to open the project (android folder) in IntelliJ and build.
