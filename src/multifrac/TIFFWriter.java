package multifrac;

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

import java.io.*;

/**
 * "High-performance" TIFF-Writer, saves memory and should be used for really large images.
 */
public class TIFFWriter
{
	public static final int SIZE_HEADER = 8;
	public static final int IFD_ENTRIES = 8;
	public static final int SIZE_IFD    = 2 + IFD_ENTRIES * 12 + 4;
	public static final int IMAGE_START = SIZE_HEADER + SIZE_IFD + 8 + 32;

	/**
	 * Write the image to the file. It'll be uncompressed.
	 */
	public static void writeRGBImage(File f, int[] img, int w, int h) throws IOException
	{
		TIFFWriter stream = new TIFFWriter(f, w, h);
		stream.seek(0);
		stream.writeRGBData(img);
		stream.close();
	}


	private FileOutputStream fos = null;
	private DataOutputStream dos = null;
	private BufferedOutputStream bos = null;
	private int w = 0;
	private int h = 0;

	/**
	 * Create a new TIFF-Streamer.
	 */
	public TIFFWriter(File f, int w, int h) throws IOException
	{
		this.w = w;
		this.h = h;

		// Try to open the file
		fos = new FileOutputStream(f);
		dos = new DataOutputStream(fos);
		bos = new BufferedOutputStream(fos);

		// Init the target: Write the header.
		writeHeader();
	}

	/**
	 * Seek to the given position relative to image start.
	 */
	public void seek(long pos) throws IOException
	{
		fos.getChannel().position(IMAGE_START + pos);
	}

	/**
	 * Seek to the given row relative to image start.
	 */
	public void seekRow(int row) throws IOException
	{
		seek(w * row * 3);
	}

	/**
	 * Close the file stream.
	 */
	public void close() throws IOException
	{
		fos.close();
	}

	/**
	 * Flush the buffered stream.
	 */
	public void flush() throws IOException
	{
		bos.flush();
	}

	/**
	 * Write data of an image (may be partial) using a buffered stream.
	 */
	public void writeRGBData(int[] img) throws IOException
	{
		// Image Data
		// ----------
		for (int i = 0; i < img.length; i++)
		{
			bos.write((img[i] >> 16) & 0xFF);
			bos.write((img[i] >>  8) & 0xFF);
			bos.write((img[i]      ) & 0xFF);
		}
		bos.flush();
	}

	/**
	 * Write a single pixel.
	 */
	public void writePixel(int p) throws IOException
	{
		bos.write((p >> 16) & 0xFF);
		bos.write((p >>  8) & 0xFF);
		bos.write((p      ) & 0xFF);
	}


	/**
	 * Internal use: Write TIFF header.
	 */
	private void writeHeader() throws IOException
	{
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
	}
}
