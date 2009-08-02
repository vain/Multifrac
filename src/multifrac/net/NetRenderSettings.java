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

package multifrac.net;

import java.io.*;

public class NetRenderSettings
{
	public int width, height;
	public int start, end;

	public NetRenderSettings()
	{
	}

	public NetRenderSettings(DataInputStream in) throws Exception
	{
		readFromStream(in);
	}

	public void writeToStream(DataOutputStream out) throws IOException
	{
		out.writeInt(width);
		out.writeInt(height);
		out.writeInt(start);
		out.writeInt(end);
	}

	public void readFromStream(DataInputStream in) throws Exception
	{
		width  = in.readInt();
		height = in.readInt();
		start  = in.readInt();
		end    = in.readInt();
	}

	public String toString()
	{
		return "\n"
			+ "\twidth : " + width + "\n"
			+ "\theight: " + height + "\n"
			+ "\tstart : " + start + "\n"
			+ "\tend   : " + end;
	}
}
