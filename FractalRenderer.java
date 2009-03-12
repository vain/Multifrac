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
		public int   width  = 0;
		public int   height = 0;
		public int   nmax   = 500;

		public Job(Dimension s)
		{
			width  = s.width;
			height = s.height;
			pixels = new int[width * height];
		}

		@Override
		public String toString()
		{
			return "Job[" + width + "x" + height  + "]";
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
		System.out.println("Executing " + myJob + " (" + myToken + ") on " + Thread.currentThread());
		
		// Mandelbrot Parameters
		double zeichX, zeichY, x, y;
		double Re_c, Im_c;
		double Re_z, Im_z, Re_z2, Im_z2;
		double sqr_abs_z;
		double resrezi = 1.0 / (double)myJob.width;
		double escape = 32.0;
		int n = 0;
		int nmax = myJob.nmax;

		int index = myToken.start * myJob.width;
		for (int coord_y = myToken.start; coord_y < myToken.end; coord_y++)
		{
			zeichY = (-1.0 + 2.0 * (double)coord_y * resrezi);
			for (int coord_x = 0; coord_x < myJob.width; coord_x++)
			{
				zeichX = (-1.0 + 2.0 * (double)coord_x * resrezi);

				// Prerequisites
				x = zeichX;
				y = zeichY;

				Re_c = x;
				Im_c = y;
				Re_z = Im_z = Re_z2 = Im_z2 = sqr_abs_z = 0.0;

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
				int bunch = job.height / numthreads;
				int a = 0, b = bunch;
				
				// Divide and Spawn
				Token params = null;
				for (int i = 0; i < run.length - 1; i++)
				{
					params = new Token();
					params.start = a;
					params.end   = b;

					run[i] = new FractalRenderer(job, params);
					run[i].start();

					a += bunch;
					b += bunch;
				}

				// Last job
				params = new Token();
				params.start = a;
				params.end   = job.height;
				run[run.length - 1] = new FractalRenderer(job, params);
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
