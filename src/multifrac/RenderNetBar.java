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

import multifrac.net.*;

import javax.swing.*;
import java.awt.*;

public class RenderNetBar extends JPanel implements NetBarDriver
{
	protected static final int perRow = 50;
	protected JPanel[] leds = null;
	protected RenderNetConsole parent = null;
	protected SimpleGridBag sgb = null;

	protected static final Color COL_FREE = Color.white;
	protected static final Color COL_WIP  = Color.yellow;
	protected static final Color COL_DONE = Color.green;

	public RenderNetBar(RenderNetConsole parent)
	{
		super();
		this.parent = parent;

		setBorder(
				BorderFactory.createTitledBorder(
					Multifrac.commonBorder, "Progress"));

		sgb = new SimpleGridBag(this);
		setLayout(sgb);
	}

	@Override
	public void setSize(final int size)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				// Init JPanel-Array.
				leds = new JPanel[size];

				// Populate it: Fixed amount per row.
				int x = 0, y = 0;
				for (int i = 0; i < size; i++)
				{
					leds[i] = new JPanel();
					leds[i].setPreferredSize(new Dimension(5, 5));
					leds[i].setBackground(COL_FREE);
					sgb.add(leds[i], x, y, 1, 1, 1.0, 1.0);

					x++;

					if ((x % perRow) == 0)
					{
						y++;
						x = 0;
					}
				}

				parent.pack();
				parent.centerMe();
				parent.setVisible(true);
			}
		});
	}

	@Override
	public void update(final int[] coord)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				for (int i = 0; i < leds.length; i++)
				{
					if (i < coord.length)
					{
						switch (coord[i])
						{
							case NetClient.CONST_FREE:
								leds[i].setBackground(COL_FREE);
								break;
							case NetClient.CONST_DONE:
								leds[i].setBackground(COL_DONE);
								break;
							default:
								leds[i].setBackground(COL_WIP);
						}
					}
				}
			}
		});
	}
}
