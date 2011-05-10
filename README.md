README for Multifrac
====================

Multifrac is a multithreaded fractal renderer (Mandelbrot set and Julia
sets) written in Java. It's intended to generate detailed high
resolution images in a reasonable time. It's *NOT* a realtime fractal
explorer.


Some more features:

* Support for distributed/network rendering.
* Smooth coloring without color banding (well, still 24bit RGB).
* A zoomable colorizer panel let's you easily set up your gradients.
* Mouse navigation as well as direct parameter input.
* Adaptive iteration depth depending on your current zoom level.
* 2x2, 4x4 supersampling.
* Loading/Saving "scenes" and the gradients of other scenes.
* Undo/Redo stack.
* ...


Build / Usage
=============

Apache ant should be used to create the .jar file, a build.xml is
included. This means that you can simply build the project via:

	$ cd /path/to/git/repo
	$ ant

In order to render high resolution images, you should increase memory
size used by java. Given your system has ~2GB of RAM, you could run the
application via:

	$ java -Xmx1800m -jar dist/Multifrac.jar

If you get an "out of memory" error during the *save* process, you can
try to use my TIFF writer instead of the builtin java libraries. You do
so by simply saving the file as "\*.tif" or "\*.tiff".

The application tries to warn you, in advance, if the image you want to
render doesn't fit in your memory. However, in some cases, it may still
fail. With 2GB of RAM, you should be able render images of the following
sizes:

* 20'400 x 20'400 pixels without supersampling
* 4300 x 4300 pixels with 4x4 supersampling

A "render directly to file" functionality is not yet included (but may
be added in the future).


Distributed Rendering
=====================

As each pixel in a Mandelbrot or Julia image is independent from all
other pixels, the rendering process can be split up into seperate jobs.
Internally, this is already done when running Multifrac on a
multiprocessor system -- since version 1.1, you can also distribute that
process among a network.

A render node can be started via:

	$ java -cp dist/Multifrac.jar multifrac.net.Node

Add the argument "--help" to see all available options. This is just a
simple CLI program which can be run over SSH without any GUI. Keep in
mind that a lot of data is transferred, so you may want to avoid slow
network connections.

The "Render" menu in the main program offers an item called "Distributed
rendering". The interface is pretty straightforward: Just enter the IPs
of your nodes like "192.168.0.3:7331". 7331 is the standard port and can
be omitted. You may most probably want to add "localhost" as well to
include your local machine (but keep in mind that you have to start a
render node on this machine, too).

Since commit bf217dd, Multifrac is ready for IPv6. Remember that you
have to put IPv6 addresses in square brackets. So, "[::1]:4201" is valid
and means port 4201 on your localhost.

Usually, the master node (the one on which the main Multifrac instance
is running) creates a big buffer where it stores the results of the
rendering nodes. Once all jobs are done, that buffer is saved to disk
after downscaling. So if you're about to render *very* large images, it
may happen that they don't fit into the memory of your master node.
Hence, there's a "stream to disk" option (only TIFF files for now). Be
aware that these files can grow quite a bit. ;)

For now, no downscaling is done when streaming directly to disk -- that
is, no "anti aliasing" will happen. You can use a tool like ImageMagick
to do the actual downscaling.


Building and using the C-node
=============================

When doing distributed rendering: On slow nodes, it can be worth using
the C-node. It's an almost exact clone (as far as rendering is
concerned) of the Java rendernode written in C. It runs a little faster
-- the speedup ranges from 1.05 to 2.35.

Building it with GNU autotools:

	$ ./configure
	$ make

Be aware, though, that glibc &gt;= 2.9 is required for now if building
on Linux (byte swapping functions, be32toh and similiar), so it probably
won't build on Ubuntu older than 9.04 or Fedora older than 10 (according
to distrowatch.com). However, I tested the C-node successfully on the
following platforms:

* Arch Linux i686 and x86\_64
* FreeBSD 7.1 and 7.2 (i386)
* NetBSD 5.0 (i386)

I'm going to remove that dependency in the near future (well, on my
machines, there's no need to do so...).

The C-node accepts the same command line arguments as the Java node,
"--help" gives a short overview.


Contact
=======

[uninformativ.de](http://www.uninformativ.de)
