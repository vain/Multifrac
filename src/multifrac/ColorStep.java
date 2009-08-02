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
import java.io.*;

public class ColorStep implements Comparable<ColorStep>
{
	private static final int VERSION = 0;

	public float pos = 0.0f;
	public Color color = null;

	public ColorStep(float p, Color c)
	{
		pos = p;
		color = c;
	}

	public ColorStep(DataInputStream in) throws Exception
	{
		readFromStream(in);
	}

	public ColorStep(ColorStep c)
	{
		pos = c.pos;
		color = new Color(c.color.getRGB());
	}

	@Override
	public String toString()
	{
		return "CS[" + pos + ", " + color + "]";
	}

	@Override
	public int compareTo(ColorStep other)
	{
		if (pos < other.pos)
			return -1;
		else if (pos == other.pos)
			return 0;
		else 
			return 1;
	}

	public void writeToStream(DataOutputStream out) throws IOException
	{
		// ***************************************************
		// Do not forget to increase VERSION on major changes.
		// ***************************************************

		out.writeInt(VERSION);

		out.writeFloat(pos);
		out.writeInt(color.getRGB());
	}

	private void readFromStream(DataInputStream in) throws Exception
	{
		if (in.readInt() != VERSION)
		{
			throw new InstantiationException("ColorStep: Header mismatch.");
		}

		pos = in.readFloat();
		color = new Color(in.readInt(), true);
	}
}
