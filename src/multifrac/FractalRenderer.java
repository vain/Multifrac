/*
	Copyright 2009 Peter Hofmann

	This file is part of Multifrac.

	Multifrac is free software: you can redistribute it and/or modify it
	under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Multifrac is distributed in the hope that it will be useful, but
	WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with Multifrac. If not, see <http://www.gnu.org/licenses/>.
*/

package multifrac;

import java.awt.*;
import javax.swing.*;

/**
 * Magic. :)
 */
public class FractalRenderer extends Thread
{
	private static final double logTwoBaseTen = Math.log10(2.0);
	

	// Properties of a whole render process.
	private Job myJob = null;
	private int[] coordinator = null;
	public FractalRenderer(Job j, int[] c)
	{
		myJob = j;
		coordinator = c;
	}
	

	/**
	 * Describes a whole job, i.e. the complete image.
	 * A job can consist out of one or more tokens.
	 */
	static public class Job
	{
		public int[] pixels = null;
		public FractalParameters param = null;
		public long stamp = 0;
		public int supersampling = 1;
		public Publisher pub = null;
		public boolean isCropped = false;

		private boolean canceled = false;

		public Job(FractalParameters p, int supsam, long s, Publisher pu)
		{
			this(p, supsam, s, pu, -1);
		}

		public Job(FractalParameters p, int supsam, long s, Publisher pu,
				int cropRows)
		{
			param = new FractalParameters(p);

			supersampling      = supsam;
			param.size.width  *= supsam;
			param.size.height *= supsam;

			// The buffer can either be fullsized or cropped.
			if (cropRows == -1)
				pixels = new int[param.getWidth() * param.getHeight()];
			else
			{
				pixels = new int[param.getWidth() * cropRows];
				isCropped = true;
			}

			stamp = s;

			pub = pu;
		}

		public int[] getPixels()
		{
			return pixels;
		}
		public int getWidth()
		{
			return param.getWidth();
		}
		public int getHeight()
		{
			return param.getHeight();
		}

		public void resizeBack()
		{
			pixels = ImageOperations.resize2(
					pixels,
					getWidth(),
					getHeight(),
					supersampling);

			param.size.width  /= supersampling;
			param.size.height /= supersampling;
		}

		synchronized public void cancel()
		{
			canceled = true;
		}

		synchronized public boolean isCanceled()
		{
			return canceled;
		}

		@Override
		public String toString()
		{
			return "Job[" + getWidth() + "x" + getHeight() + "]";
		}
	}

	/**
	 * This will be executed on the EDT after the job has been completed.
	 */
	static public abstract class Callback implements Runnable
	{
		private Job job = null;
		protected void setJob(Job j)
		{
			job = j;
		}
		public Job getJob()
		{
			return job;
		}

		/**
		 * Override this method. Note: As this will run on the EDT, it is
		 * safe to call "paintImmediately()" on another Component, i.e. a
		 * DisplayPanel. So, you can assure the result of a calculation will
		 * be displayed atomically without being interrupted by other
		 * background-render-threads.
		 */
		@Override
		public abstract void run();
	}

	/**
	 * This will be executed on the EDT to publish progress.
	 */
	static public abstract class Publisher implements Runnable
	{
		private int v = 0;

		/**
		 * Only set the value if it is larger than the current value.
		 */
		synchronized protected void setValue(int i)
		{
			if (i > v)
				v = i;
		}
		synchronized public int getValue()
		{
			return v;
		}

		/**
		 * Override this method.
		 */
		@Override
		public abstract void run();
	}

	static public abstract class Messenger implements Runnable
	{
		private int state = 0;
		protected void setState(int i)
		{
			state = i;
		}
		public int getState()
		{
			return state;
		}

		/**
		 * Override this method.
		 */
		@Override
		public abstract void run();
	}


	/**
	 * This is where the magic happens. This class is derived from "Thread",
	 * so this is the actual part which will be executed in parallel somewhere
	 * in the background.
	 */
	public void renderPass(int tstart, int tend)
	{
		// This is where the magic happens.
		//System.out.println("Executing " + myJob + " (" + myToken + ") on " + Thread.currentThread());
		
		// Mandelbrot Parameters
		double x, y;
		double Re_c, Im_c;
		double Re_z, Im_z, Re_z2, Im_z2;
		double sqr_abs_z;
		double escape = myJob.param.escape;
		int n = 0;
		int nmax = myJob.param.nmax;
		double w = myJob.getWidth();
		double muh = 0.0;

		// Choose starting index depending on buffer type
		int index = 0;
		if (!myJob.isCropped)
			index = tstart * myJob.getWidth();

		for (int coord_y = tstart; coord_y < tend; coord_y++)
		{
			//zeichY = (-1.0 + 2.0 * (double)coord_y * resrezi);
			y = myJob.param.YtoWorld(coord_y);
			for (int coord_x = 0; coord_x < w; coord_x++)
			{
				//zeichX = (-1.0 + 2.0 * (double)coord_x * resrezi);
				x = myJob.param.XtoWorld(coord_x);

				// Prerequisites
				Re_c = x;
				Im_c = y;
				Re_z = Im_z = Re_z2 = Im_z2 = sqr_abs_z = 0.0;

				switch (myJob.param.type)
				{
					case FractalParameters.TYPE_MANDELBROT:
						// z_{n+1} = z_n^2 + c ,  z_0 = 0 ,  c die Koordinate
						Re_c = x;
						Im_c = y;
						break;
					case FractalParameters.TYPE_JULIA:
						// z_{n+1} = z_n^2 + k ,  z_0 = c ,  c Koord., k Julia-Param.
						Re_c = myJob.param.julia_re;
						Im_c = myJob.param.julia_im;
						Re_z = x;
						Im_z = y;
						break;
				}

				n = 0;

				// Loop
				Re_z2 = Re_z * Re_z;
				Im_z2 = Im_z * Im_z;
				while (sqr_abs_z < escape && n < nmax)
				{
					Im_z = 2.0 * Re_z * Im_z + Im_c;
					Re_z = Re_z2 - Im_z2 + Re_c;

					Re_z2 = Re_z * Re_z;
					Im_z2 = Im_z * Im_z;

					sqr_abs_z = Re_z2 + Im_z2;
					n++;
				}

				// Decision
				if (n == nmax)
				{
					// Inside
					myJob.pixels[index++] = myJob.param.colorInside.getRGB();
				}
				else
				{
					// Outside
					// Idea: http://linas.org/art-gallery/escape/smooth.html
					muh = (double)n + 1.0f - Math.log10(Math.log10(Math.sqrt(sqr_abs_z))) / logTwoBaseTen;
					muh /= nmax;

					// Linear interpolation between marks
					if (muh >= 1.0)
					{
						// If muh is greater than or equal to 1, just use the last color.
						myJob.pixels[index++] = myJob.param.gradient.get(myJob.param.gradient.size() - 1).color.getRGB();
					}
					else
					{
						int i = 1;

						// Find the first index where muh will be less than get(i).
						// This will be (i + 1), so decrease i afterwards.
						while (i < myJob.param.gradient.size() && muh > myJob.param.gradient.get(i).pos)
							i++;

						i--;

						// Scale muh from 0 to 1 in the given interval.
						double span = myJob.param.gradient.get(i + 1).pos - myJob.param.gradient.get(i).pos;
						muh -= myJob.param.gradient.get(i).pos;
						muh /= span;

						// Get the 2 colors and interpolate them linearly.
						Color c1 = myJob.param.gradient.get(i).color;
						Color c2 = myJob.param.gradient.get(i + 1).color;

						int r = (int)(c1.getRed() * (1.0 - muh))   + (int)(c2.getRed() * muh);
						int g = (int)(c1.getGreen() * (1.0 - muh)) + (int)(c2.getGreen() * muh);
						int b = (int)(c1.getBlue() * (1.0 - muh))  + (int)(c2.getBlue() * muh);

						// Convert it back to an RGB-integer.
						myJob.pixels[index++] = 0xFF000000 + (r << 16) + (g << 8) + b;
					}
				}
			}
		}
	}

	/**
	 * Starting point of one render thread.
	 */
	@Override
	public void run()
	{
		int bunch = 6;
		int max = myJob.getHeight();

		int start = 0;
		int end   = 0;

		while (true)
		{
			// Only continue if this job is not marked as "canceled"
			if (myJob.isCanceled())
				return;

			// The coordinator is used as follows:
			//  - It is an object, so threads can synchronize on this object
			//  - coordinator[0] always holds the next possible start row
			//  - After a thread has fetched its own new start row, it will
			//    increase the global next start row.
			synchronized (coordinator)
			{
				if (coordinator[0] >= max)
					return;

				start = coordinator[0];
				coordinator[0] += bunch;
				end = coordinator[0];
			}

			// Do not exceed image boundaries. :-)
			if (end >= max)
				end = max;

			// Now render this particular token.
			renderPass(start, end);

			// Update progress
			if (myJob.pub != null)
			{
				myJob.pub.setValue((int)(100.0f * end / (float)max));
				SwingUtilities.invokeLater(myJob.pub);
			}
		}
	}

	/**
	 * Used to dispatch a job. It will calculate the fractal for the given
	 * parameters in the background and will immediately return.
	 */
	public static Job dispatchJob(final int numthreads, final Job job, final Callback whenFinished,
								   final Messenger msg)
	{
		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				if (msg != null)
				{
					msg.setState(0);
					SwingUtilities.invokeLater(msg);
				}

				// Manage
				FractalRenderer[] run = new FractalRenderer[numthreads];
				
				// Divide and Spawn
				int[] coordinator = new int[1];
				for (int i = 0; i < run.length; i++)
				{
					run[i] = new FractalRenderer(job, coordinator);
					run[i].start();
				}

				// Join
				for (int i = 0; i < run.length; i++)
				{
					try
					{
						run[i].join();
					}
					catch (InterruptedException ignore)
					{}
				}

				// Check if the job has been marked as "canceled"
				if (!job.isCanceled())
				{
					// Push current status
					if (msg != null)
					{
						msg.setState(1);
						SwingUtilities.invokeLater(msg);
					}

					// Resize back to normal size
					job.resizeBack();
				}

				// Callback
				whenFinished.setJob(job);
				SwingUtilities.invokeLater(whenFinished);
			}
		};
		t.start();

		return job;
	}
}
