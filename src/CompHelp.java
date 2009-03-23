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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CompHelp
{
	/**
	 * Add a focusGained-listener to this textfield which selects
	 * all text once the component has gained focus.
	 */
	public static void addSelectOnFocus(JTextField one)
	{
		final JTextField text = one;
		text.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusGained(FocusEvent e)
			{
				// Yap, to select text, you'll need to queue that.
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						text.selectAll();
					}
				});
			}
		});
	}

	/**
	 * Like addSelectOnFocus(JTextField one), but for all components
	 * in the array.
	 */
	public static void addSelectOnFocus(JTextField[] av)
	{
		for (JTextField comp : av)
		{
			addSelectOnFocus(comp);
		}
	}
}
