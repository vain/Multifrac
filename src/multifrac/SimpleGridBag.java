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
import javax.swing.JLabel;

public class SimpleGridBag extends GridBagLayout
{
	private GridBagConstraints gbc = new GridBagConstraints(); 
	private Container cont = null;

	public SimpleGridBag(Container cont)
	{
		super();
		this.cont = cont;

		gbc.insets = new Insets(2, 2, 2, 2);
	}
	
	public void add(Component c, int x, int y, int w, int h, double wx, double wy)
	{
		gbc.fill = GridBagConstraints.BOTH; 

		gbc.gridx = x;
		gbc.gridy = y; 
		gbc.gridwidth = w;
		gbc.gridheight = h; 

		gbc.weightx = wx;
		gbc.weighty = wy; 

		setConstraints(c, gbc);
		cont.add(c);
	}

	public void addLabel(String title, int x, int y, int w, int h, double wx, double wy)
	{
		add(new JLabel(title), x, y, w, h, wx, wy);
	}

	public void setInsets(Insets i)
	{
		gbc.insets = i;
	}

	public Insets getInsets()
	{
		return gbc.insets;
	}
}
