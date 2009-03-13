import java.awt.*;
import javax.swing.*;

/**
 * Magic. :)
 */
public class FractalRenderer extends Thread
{
	/**
	 * Describes a whole job, i.e. the complete image.
	 * A job can consist out of one or more tokens.
	 */
	static public class Job
	{
		public int[] pixels = null;
		public FractalParameters param = null;
		public long stamp = 0;

		public Job(FractalParameters p, long s)
		{
			param = p;
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
		public void setJob(Job j)
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
	 * Describes one token of a job.
	 */
	static protected class Token
	{
		protected int start = 0;
		protected int end   = 0;

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
					myJob.pixels[index++] = 0xFF000000;
				}
				else
				{
					myJob.pixels[index++] = 0xFFFFFFFF;
				}
			}
		}
	}

	/**
	 * Used to dispatch a job. It will calculate the fractal for the given
	 * parameters in the background and will immediately return.
	 */
	public static void dispatchJob(final int numthreads, final Job job, final Callback whenFinished)
	{
		Thread t = new Thread()
		{
			@Override
			public void run()
			{
				// Manage
				FractalRenderer[] run = new FractalRenderer[numthreads];
				int bunch = job.param.getHeight() / numthreads;
				int a = 0, b = bunch;
				
				// Divide and Spawn
				Token toktok = null;
				for (int i = 0; i < run.length - 1; i++)
				{
					toktok = new Token();
					toktok.start = a;
					toktok.end   = b;

					run[i] = new FractalRenderer(job, toktok);
					run[i].start();

					a += bunch;
					b += bunch;
				}

				// Last job
				toktok = new Token();
				toktok.start = a;
				toktok.end   = job.param.getHeight();
				run[run.length - 1] = new FractalRenderer(job, toktok);
				run[run.length - 1].start();

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

				// Callback
				whenFinished.setJob(job);
				SwingUtilities.invokeLater(whenFinished);
			}
		};
		t.start();
	}
}
