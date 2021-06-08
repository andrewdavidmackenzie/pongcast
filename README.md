Custom Chromecast Receiver
=

The main file that loads the custom receiver code on the chromecast is index.html.

That loads the chromecast Javascript library script 
(from http://www.gstatic.com/cast/sdk/libs/receiver/2.0.0/cast_receiver.js)
and then pong.js has the main logic script of the game.

castReceiverController.js
=
This is the javascript library to talk over the chromecast channel to connected `sender` mobile apps
that connect there.