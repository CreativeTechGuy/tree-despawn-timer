# Tree Despawn Timer

Demo of the plugin UI. You can also change the pie timer to show ticks as a number if you prefer.
![Demo](./demo.png)

This plugin will estimate the amount of time remaining to cut a tree before it'll be chopped down (despawn). This takes
into account the new forestry mechanics where a tree will stay standing for a set amount of time, will regain health
when not being chopped, etc. The plugin is highly accurate if you are chopping alone, and reasonably accurate for
everything else. It works best for higher level trees with longer despawn timers.

If the plugin is reasonably confident that it knows how long a tree has left, it'll show a timer on the tree. If there's
no timer, the tree has an unknown amount of time left so you should assume it'll be chopped down any moment.

Technical details gathered through testing (not official):

* Tree "health" only starts ticking down once someone has collected the first log from the tree. If you are chopping the
  tree but have yet to gather a log, the timer won't start.
* The "health" goes down by 1 every game tick (0.6s) as long as at least one person is chopping.
* The "health" goes back up by 1 every game tick as long as no one is chopping.

Shortcomings:

* The time is an estimate so sometimes will have a few ticks left and be chopped down, or will run out of time and still
  be standing until someone collects another log.
    * Unfortunately this cannot be fixed as the system is not tick-perfect. It shouldn't be off by more than a few
      seconds though.