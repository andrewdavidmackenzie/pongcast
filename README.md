pongcast
========

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
=======
Javascript/CSS-fu to make it all run quicker and smoother on the chromecast is very welcome!
