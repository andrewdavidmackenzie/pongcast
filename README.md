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
I have mostly worked on the Javascript custom receiver so far. I did it in a way I could run it on my chrome browser
on my development computer and control the paddles with the keyboard....and later work more on controlling it from
the android app.

The android app is at the moment an almost unmodified chromecast sample app that just starts the custom receiver.

Despite using some CSS scaling tricks, the update rate on the chromecast is still too slow, and it doesn't work smoothly, 
so that's one area of work.

The other is evolving the mobile app into a paddle controller app, and then allowing one or two of them to connect 
and start a game.

Contributing
======
Javascript/CSS-fu to make it all run quicker and smoother on the chromecast is very welcome!

Also, for some reason I have not been able to get the sound reproduction to synchronize with the events, nor be
reliable - despite using some tricks I found on the web... so help with good sound from JS on Chromecast is also
much needed.

Folders
======
* android - the android app to control the game, just a skeleton at the moment.
* CastCompanionLibrary - see the section below, a library for interacting with Chromecast
* receiver - custom chromecast receiver html, javascript, image and sound files

Scripts
======
deployReceiver.sh - this script just copies the ./receiver folder into my Google Drive folder. This syncs the contents to Google Drive in the cloud, where I have the folder shared publicly. Google Drive is actually quite a good way to host a "statics only" web site. 

Using your own receiver
=====
If you want to modify the receiver or develop your own, you will need to setup somethings similar in order to host it so that the chromecast can download it. Then publish your receiver so that the chromecast can download and run it. You will need to modify the receiver identifier in the android code to refer to your receiver.

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
