import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.util.concurrent.*;

public class RenderPanel extends JPanel
{
	protected ConcurrentLinkedQueue<int[]> toDraw = new ConcurrentLinkedQueue<int[]>();
	protected int nmax = 1000;

	/**
	 * @param job [0]: start row, [1]: end row (exclusive), [2]: nmax
	 */
	protected void renderRows(final int[] job, final int[] size, final int[] px)
	{
		System.out.println("Dispatching new job in " + Thread.currentThread());

		// One worker thread. Runs in the Swing Pool.
		SwingWorker worker = new SwingWorker<int[], Void>()
		{
			@Override
			public int[] doInBackground()
			{
				// Anything that takes a long time has to be done here. It runs
				// in a separate background thread.
				int randRed = (int)(Math.random() * 255);

				// Mandelbrot Parameters
				double zeichX, zeichY, x, y;
				double Re_c, Im_c;
				double Re_z, Im_z, Re_z2, Im_z2;
				double sqr_abs_z;
				double resrezi = 1.0 / (double)size[0];
				double escape = 32.0;
				int n = 0;
				int nmax = job[2];

				int index = job[0] * size[0];
				for (int coord_y = job[0]; coord_y < job[1]; coord_y++)
				{
					zeichY = (-1.0 + 2.0 * (double)coord_y * resrezi);
					for (int coord_x = 0; coord_x < size[0]; coord_x++)
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
							px[index++] = 0xFF000000;
						}
						else
						{
							px[index++] = 0xFFFFFFFF;
						}
					}
				}

				return px;
			}

			@Override
			public void done()
			{
				// When this worker finishes, it calls this method.
				// Important: This part runs on the EDT!
				int[] pxres = null;
				try
				{
					pxres = get();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
					return;
				}
				catch (java.util.concurrent.ExecutionException e)
				{
					e.printStackTrace();
					return;
				}

				if (pxres != null)
				{
					System.out.println("Offered on " + Thread.currentThread());
					toDraw.offer(pxres);
					System.out.println("Dirty: 0, " + job[0] + ", " + size[0] + ", " + job[1]);

					// Make sure this part of the image is actually drawn.
					paintImmediately(0, job[0], size[0], job[1]);
				}
			}
		};

		// Run this job in the background and return.
		worker.execute();
	}

	@Override
	public void paint(Graphics g)
	{
		if (toDraw.isEmpty())
		{
			// If there's nothing to draw yet, create a bunch of background jobs.

			// px is the array, where this bunch of jobs will render into.
			// sz describes the dimensions for this set of jobs.
			int[] px = new int[getWidth() * getHeight()];
			int[] sz = new int[] { getWidth(), getHeight() };

			int numthreads = 4;

			int bunch = getHeight() / numthreads;

			// Dispatch the first set of jobs (limits floor'ed)
			int a = 0, b = bunch;
			for (int t = 0; t < numthreads - 1; t++)
			{
				int[] j = new int[] { a, b, nmax };
				renderRows(j, sz, px);

				a += bunch;
				b += bunch;
			}

			// Dispatch the last job: Simply the yet unrendered part.
			int[] j = new int[] { a, getHeight(), nmax };
			renderRows(j, sz, px);
		}
		else
		{
			// There *is* something to draw, so draw it. Use a fast MemoryImage.
			int[] drawIt = toDraw.poll();
			System.out.println("Redrawing on " + Thread.currentThread() + ", " + toDraw.size() + " left");
			Image img = createImage(
						new MemoryImageSource(
							getWidth(), getHeight(), drawIt, 0, getWidth()));
			((Graphics2D)g).drawImage(img, new java.awt.geom.AffineTransform(1f, 0f, 0f, 1f, 0, 0), null);
		}
	}
}
