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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SingleColorPanel extends JLabel
{
	private ParameterStack paramStack = null;

	public SingleColorPanel(String text, final Component parent, ParameterStack p, final Runnable onChange)
	{
		super(text, SwingConstants.CENTER);

		paramStack = p;

		setOpaque(true);

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				Color temp = ColorChooser.showDialog(
					parent,
					"Edit color: \"Inside the set\"",
					getBackground());

				if (temp != null)
				{
					paramStack.push();
					paramStack.get().colorInside = temp;
					repaint();

					onChange.run();
				}
			}
		});
	}

	@Override
	public void paintComponent(Graphics g)
	{
		setBackground(paramStack.get().colorInside);
		super.paintComponent(g);
	}
}
