pongcast [![travis-ci](https://travis-ci.org/andrewdavidmackenzie/pongcast.png?branch=master)](https://travis-ci.org/andrewdavidmackenzie/pongcast)
======

I wanted to learn more about Chromecast development (the receiver that runs on the chromecast itself, 
and the mobile app "controller" and decided that a good way would be to do a simple game. 

Most of the first apps for chromecast I was seeing were video streaming and the like, but I wanted to do something multi-person, where more than once person interacted with the chromecast at the same time, and the experience was shared between people on the shared screen - your TV.

So I thought a multi-player game with two controllers connecting and playing together on the shared
TV screen would be good.

In homage to Pong (which met some birthday near the time and was in the news) I decided to make it pong on chromecast, taking me right back to the first video game I ever played.

Try out a version on your browser [here](http://andrewdavidmackenzie.github.io/pongcast/pong.html)

[Get the Android "Sender" app](https://play.google.com/store/apps/details?id=net.mackenzie_serres.pongcast) on Google Play Store. 
- Version 1.0.2 fixes some crashes on start-up, sorry folks!

Areas Needing work
=====
Despite using some CSS scaling tricks, the update rate on the chromecast is still too slow, and it doesn't work smoothly, so that's one area of work.
Javascript/CSS-fu to make it all run quicker and smoother on the chromecast is very welcome!

Also, for some reason I have not been able to get the sound reproduction to synchronize with the events, nor be
reliable - despite using some tricks I found on the web... so help with good sound from JS on Chromecast is also
much needed.

[GitHub Issues](https://github.com/andrewdavidmackenzie/pongcast/issues)

Folders
======
* android - the android app to control the game, just a skeleton at the moment.
* CastCompanionLibrary - see the section below, a library for interacting with Chromecast
* receiver - custom chromecast receiver html, javascript, image and sound files

Hosting your own receiver
=====
I have moved to hosting my production version of the receiver from GitHub Pages.

You can see it [here](http://andrewdavidmackenzie.github.io/pongcast/pong.html)

I was previously hosting the receiver on Google Drive, but they have stopped allowing you to publish html pages from drive (the previously published one still worked).
Also, it looks like they were blocking my .mp3 audio files - probably to stop people sharing soing via Google Drive folders.

See Keyboard Control below for using the browser version.

If you fork this repo, you will also get this branch and you should get a hosted version of the game under "your-username".github.io/pongcast/pong.html
You will need to modify the receiver identifier in the android code to refer to the URL where you host you receiver.

Keyboard Control
=====
* Use 'e' to Enter the court.
* Use 's' to Start a game once you are in the court.
* Use up and down keys on keyboard to control the paddle.
* Use spacebar to pause/restart the game.
* Use 'l' to leave the court and end the game

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
