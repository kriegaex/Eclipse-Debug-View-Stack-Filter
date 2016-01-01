Eclipse-Debug-View-Stack-Filter
===============================

Patch for Eclipse JDT debug UI plugin applying step filters to stacktrace view in debug window

## History

I found some patched JARs at the [Intersult website](https://www.intersult.com/wiki/page/Eclipse%20stackfilter%20plugin),
but only for old Eclipse versions up to Indigo, not Kepler or Luna. So I downloaded the Indigo plugin, compared it with
the plugin from the original release, found only one changed class, decompiled both versions of that class and reviewed
the changes in a diff viewer. They were pretty straightforward.

Then I cloned the plugin from the original repo at `git://git.eclipse.org/gitroot/jdt/eclipse.jdt.debug.git`, checked out
tag `R4_4_1` and branched off of it. I applied the changes, recompiled the plugin, tested and exported it, then deployed it
locally to my *ECLIPSE_DIR/dropins* folder. It seems to work nicely.

## Download and installation

You can download the patched plugin for Eclipse Luna 4.4.1 from http://scrum-master.de/download/eclipse_debug_stackfilter/org.eclipse.jdt.debug.ui_3.6.300.201412061413.jar.

You can download the patched plugin for Eclipse Mars 4.5.1 from http://scrum-master.de/download/eclipse_debug_stackfilter/org.eclipse.jdt.debug.ui_3.7.0.v20150505-1916.jar (untested!).

Installation is simple: Just copy the JAR into your local *ECLIPSE_DIR/dropins* folder.

## Future

Because I am no Eclipse committer and this way my first contact with an Eclipse plugin project anyway, I cannot commit
the patch. It is quick'n'dirty anyway because the new feature of filtering the stacktrace view is not optional via the UI.
Thus, I am have cloned/forked the repo here on GitHub so as to make the patch available to anyone interested.

Theoretically I can pull from the original remote and merge the patch from the GitHub repo into new Eclipse release 
versions. As Git makes it easy to deal with multiple remotes, it should not be much work.
