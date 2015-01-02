Custom Chromecast Receiver
=====

The main file that loads the custom receiver code on the chromecast is pong.html.

That loads the chromecast Javascript library script (from http://www.gstatic.com/cast/sdk/libs/receiver/2.0.0/cast_receiver.js)
and then pong.js the main logic script of the game and castReceiverController.js which is the javascript to talk over the
chromecast channel to controller apps that connect there.

castReceiverController.js
=====
This provides a number of functions to communicate over the channel between the chromecast and controlling apps.

stats.min.js
=====
stats.min.js - this is a minified JS code for displaying refresh frame rate stats on-screen.