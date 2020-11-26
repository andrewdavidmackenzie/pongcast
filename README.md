PongCast [![travis-ci](https://travis-ci.org/andrewdavidmackenzie/pongcast.png?branch=master)](https://travis-ci.org/andrewdavidmackenzie/pongcast)
=

A homage to Pong allowing you to play it on your big-screen TV via chromecast.

Try out a version on your browser [here](http://andrewdavidmackenzie.github.io/pongcast/index.html)

Get the [Android "Sender" app](https://play.google.com/store/apps/details?id=net.mackenzie.pongcast) 
on the Google Play Store. 

Areas Needing work
=
Despite using some CSS scaling tricks, the update rate on the chromecast is still too slow, and it doesn't work smoothly
So Javascript/CSS skills to make it all run quicker and smoother on the chromecast is very welcome!

Now there are some smaller `cast-enabled` devices (like Google Nest Hub and other smart displays) other 
than chromecast on an HD (or bigger) TV. Some of these have smaller screen resolutions and I haven't been able
to detect them and adapt the size of the court to fit them yet.

Also, for some reason on chromecast (it works much better on Chrome) I have not been able to get the 
sound reproduction to synchronize with the events, nor be reliable, despite using some tricks I found on the web.

So help with good sound from JavaScript on Chromecast is also much needed.

Google has brought out an entirely new version of the Javascript SDK for chromecast (CAF v3) to which it
would be good to update.

[GitHub Issues](https://github.com/andrewdavidmackenzie/pongcast/issues)

Folders
=
* android - the android app to control the game, just a skeleton at the moment.
* castCompanionLibrary - see the section below, a library for interacting with Chromecast
* receiver - custom chromecast receiver html, javascript, image and sound files

Keyboard Control
=
If you open the html page in Chrome (not chromecast) it loads a keyboard controller 
instead of the Android Chromecast controller app.

These are the keyboard controls:
* Use 'e' to Enter the court.
* Use 's' to Start a game once you are in the court.
* Use up and down keys on keyboard to control the paddle.
* Use space bar to pause/restart the game.
* Use 'l' to leave the court and end the game