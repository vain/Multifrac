/*
        This program is free software; you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation; either version 2 of the License, or
        (at your option) any later version.
        
        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.
        
        You should have received a copy of the GNU General Public License
        along with this program; if not, write to the Free Software
        Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
        MA 02110-1301, USA.
*/

import java.awt.*;
import javax.swing.*;

/**
 * Magic. :)
 */
public class FractalRenderer extends Thread
{
	private static final double logTwoBaseTen = Math.log10(2.0);

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

		public Job(FractalParameters p, int supsam, long s)
		{
			param = new FractalParameters(p);

			supersampling      = supsam;
			param.size.width  *= supsam;
			param.size.height *= supsam;

			pixels = new int[param.getWidth() * param.getHeight()];

			stamp = s;
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

		@Override
		public String toString()
		{
			return "Job[" + getWidth() + "x" + getHeight() + "]";
		}
	}

	// TODO: Generics for all those "Carriers" and "Publishers"

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
		private int ID = -1;
		private int v  = 0;
		protected void setID(int i)
		{
			ID = i;
		}
		public int getID()
		{
			return ID;
		}

		protected void setValue(int i)
		{
			v = i;
		}
		public int getValue()
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
		private String msg = "";
		protected void setMsg(String s)
		{
			msg = s;
		}
		public String getMsg()
		{
			return msg;
		}

		/**
		 * Override this method.
		 */
		@Override
		public abstract void run();
	}

	/**
	 * Describes one token of a job.
	 */
	static protected class Token
	{
		protected int start = 0;
		protected int end   = 0;
		protected Publisher pub = null;

		@Override
		public String toString()
		{
			return "Token[" + start + "," + end  + "]";
		}
	}


	// Store stuff... An object of this class will handle the pair (Job, Token)
	private Job myJob = null;
	private Token myToken = null;
	public FractalRenderer(Job j, Token t)
	{
		myJob = j;
		myToken = t;
	}
	
	/**
	 * This is where the magic happens. This class is derived from "Thread",
	 * so this is the actual part which will be executed in parallel somewhere
	 * in the background.
	 */
	@Override
	public void run()
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

		int index = myToken.start * myJob.getWidth();
		for (int coord_y = myToken.start; coord_y < myToken.end; coord_y++)
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

					// Apply an extra square root to push smaller values. This allows
					// an easier selection of colors via the colorizer.
					muh = Math.sqrt(muh);

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

			// Publish progress
			if ((coord_y % 50) == 0 && myToken.pub != null)
			{
				// Scale the current row to [0, 100], then invoke the publisher.
				myToken.pub.setValue(
						(int)(
							100.0f * (coord_y - myToken.start) / (float)(myToken.end - myToken.start)
							));
				SwingUtilities.invokeLater(myToken.pub);
			}
		}
	}

	/**
	 * Used to dispatch a job. It will calculate the fractal for the given
	 * parameters in the background and will immediately return.
	 *
	 * TODO: An Object[] for the publishers seems to be the only way
	 *       to pass them. Remember: It must be OK to use "null" instead
	 *       of a real array - and this is not possible? Whuh?
	 */
	public static void dispatchJob(final int numthreads, final Job job, final Callback whenFinished,
			final Object[] pub, final Messenger msg)
	{
		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				if (msg != null)
				{
					msg.setMsg("Rendering in progress, this may take a while.");
					SwingUtilities.invokeLater(msg);
				}

				// Manage
				FractalRenderer[] run = new FractalRenderer[numthreads];
				int bunch = job.param.getHeight() / numthreads;
				int a = 0, b = bunch;
				
				// Divide and Spawn
				// TODO: Dynamic work distribution...
				Token toktok = null;
				for (int i = 0; i < run.length - 1; i++)
				{
					Publisher tokpub = null;
					if (pub != null && i < pub.length && pub[i] instanceof Publisher)
					{
						tokpub = (Publisher)pub[i];
						tokpub.setID(i);
					}

					toktok = new Token();
					toktok.start = a;
					toktok.end   = b;
					toktok.pub   = tokpub;

					run[i] = new FractalRenderer(job, toktok);
					run[i].start();

					a += bunch;
					b += bunch;
				}

				// Last job
				Publisher tokpub = null;
				int last = run.length - 1;
				if (pub != null && last < pub.length && pub[last] instanceof Publisher)
				{
					tokpub = (Publisher)pub[last];
					tokpub.setID(last);
				}

				toktok = new Token();
				toktok.start = a;
				toktok.end   = job.param.getHeight();
				toktok.pub   = tokpub;
				run[last] = new FractalRenderer(job, toktok);
				run[last].start();

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

				// Push current status
				if (msg != null)
				{
					msg.setMsg("Resizing and saving.");
					SwingUtilities.invokeLater(msg);
				}

				// Resize back to normal size
				job.resizeBack();

				// Callback
				whenFinished.setJob(job);
				SwingUtilities.invokeLater(whenFinished);
			}
		};
		t.start();
	}
}
