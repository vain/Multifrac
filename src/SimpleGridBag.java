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

import java.awt.*;

public class SimpleGridBag extends GridBagLayout
{
	private Container cont = null;

	public SimpleGridBag(Container cont)
	{
		super();
		this.cont = cont;
	}
	
	public void add(Component c, int x, int y, int w, int h, double wx, double wy)
	{
		GridBagConstraints gbc = new GridBagConstraints(); 
		gbc.fill = GridBagConstraints.BOTH; 

		gbc.gridx = x;
		gbc.gridy = y; 
		gbc.gridwidth = w;
		gbc.gridheight = h; 

		gbc.weightx = wx;
		gbc.weighty = wy; 

		gbc.insets = new Insets(2, 2, 2, 2);

		setConstraints(c, gbc);
		cont.add(c);
	}
}
