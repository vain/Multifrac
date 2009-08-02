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

/**
 * Operations on int[]-Images which try to not be memory-intensive.
 */
public class ImageOperations
{
	/**
	 * Bilinearly interpolate between two int-colors, factor 0.5
	 */
	protected static int bilinear2(int a, int b)
	{
		int a1 = (a & 0xFF000000) >> 24;
		int r1 = (a & 0x00FF0000) >> 16;
		int g1 = (a & 0x0000FF00) >> 8;
		int b1 = (a & 0x000000FF);

		int a2 = (b & 0xFF000000) >> 24;
		int r2 = (b & 0x00FF0000) >> 16;
		int g2 = (b & 0x0000FF00) >> 8;
		int b2 = (b & 0x000000FF);

		int aT = (a1 + a2) / 2;
		int rT = (r1 + r2) / 2;
		int gT = (g1 + g2) / 2;
		int bT = (b1 + b2) / 2;

		return (aT << 24) + (rT << 16) + (gT << 8) + bT;
	}

	/**
	 * Reduce the images size in each dimension by 2 until the original
	 * size is reached.
	 *
	 * Example: origW = 8192, origH = 6144, factor = 4 results in:
	 * 		8192x6144 to 4096x3072
	 * 		4096x3072 to 2048x1536
	 * 		2048x1536 to 1024x768
	 */
	public static int[] resize2(int[] px, int origW, int origH, int factor)
	{
		// No resizing at all?
		if (factor < 2)
			return px;

		int wFrom = origW;
		int hFrom = origH;
		int wTo   = origW;
		int hTo   = origH;

		do
		{
			// Parameters for this run (keep current wFrom and hFrom)
			wTo /= 2;
			hTo /= 2;

			// Target array
			int[] to = new int[wTo * hTo];

			int tind = 0;
			for (int y = 0; y < hFrom - 1; y += 2)
			{
				for (int x = 0; x < wFrom - 1; x += 2)
				{
					// Get the four pixels for this single target pixel
					int c00 = px[(x + 0) + (y + 0) * wFrom];
					int c10 = px[(x + 1) + (y + 0) * wFrom];
					int c01 = px[(x + 0) + (y + 1) * wFrom];
					int c11 = px[(x + 1) + (y + 1) * wFrom];

					// Interpolate and save it
					int r1 = bilinear2(c00, c10);
					int r2 = bilinear2(c01, c11);
					to[tind++] = bilinear2(r1, r2);
				}
			}

			// *From parameters for next run
			wFrom = wTo;
			hFrom = hTo;
			px = to;

			// See if the picture can be divided into halves again
			factor /= 2;
		} while (factor != 1);

		return px;
	}
}
