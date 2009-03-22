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

import java.io.*;

/**
 * "High-performance" TIFF-Writer, saves memory and should be used for really large images.
 */
class TIFFWriter
{
	/**
	 * Write the image to the file. It'll be uncompressed.
	 */
	public static void writeRGBImage(File f, int[] img, int w, int h) throws IOException
	{
		// Try to open the file
		FileOutputStream fos = new FileOutputStream(f);
		DataOutputStream dos = new DataOutputStream(fos);


		final int SIZE_HEADER = 8;
		final int IFD_ENTRIES = 8;
		final int SIZE_IFD    = 2 + IFD_ENTRIES * 12 + 4;
		final int IMAGE_START = SIZE_HEADER + SIZE_IFD + 8 + 32;

		// First thing to do: Write the header
		// -----------------------------------

		// BigEndian and Magic Number
		dos.writeInt(0x4D4D002A);

		// First IFD (directly after header)
		dos.writeInt(SIZE_HEADER);


		// Write IFD
		// ---------

		// Number of entries entries
		dos.writeShort(IFD_ENTRIES);

		// Tags: Width and Height
		dos.writeInt(0x01000004);
		dos.writeInt(0x00000001);
		dos.writeInt(w);

		dos.writeInt(0x01010004);
		dos.writeInt(0x00000001);
		dos.writeInt(h);

		// BitsPerSample: 3 * VALUE per Channel
		dos.writeInt(0x01020003);
		dos.writeInt(0x00000003);
		dos.writeInt(SIZE_HEADER + SIZE_IFD);

		// PhotometricInterpretation: RGB
		dos.writeInt(0x01060003);
		dos.writeInt(0x00000001);
		dos.writeInt(0x00020000);

		// StripOffsets, i.e. beginning of the picture
		dos.writeInt(0x01110004);
		dos.writeInt(0x00000001);
		dos.writeInt(IMAGE_START);

		// SamplesPerPixel
		dos.writeInt(0x01150003);
		dos.writeInt(0x00000001);
		dos.writeInt(0x00030000);

		// RowsPerStrip: All rows in one strip
		dos.writeInt(0x01160004);
		dos.writeInt(0x00000001);
		dos.writeInt(h);

		// StripByteCounts: All image data
		dos.writeInt(0x01170004);
		dos.writeInt(0x00000001);
		dos.writeInt(w * h * 3);

		// End of IFD
		dos.writeInt(0x00000000);

		// VALUE for BitsPerSample
		dos.writeInt(0x00080008);
		dos.writeInt(0x00080000);

		// Padding
		for (int i = 0; i < 8; i++)
			dos.writeInt(0);


		// Image Data
		// ----------
		BufferedOutputStream buf = new BufferedOutputStream(fos);
		for (int i = 0; i < img.length; i++)
		{
			buf.write((img[i] >> 16) & 0xFF);
			buf.write((img[i] >>  8) & 0xFF);
			buf.write((img[i]      ) & 0xFF);
		}
		buf.flush();


		// Close the file and we're done.
		fos.close();
	}
}
