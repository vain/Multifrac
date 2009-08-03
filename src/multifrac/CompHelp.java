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

import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

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

	/**
	 * Center one component on another.
	 */
	public static void center(Component which, Component parent)
	{
		Point loc         = parent.getLocationOnScreen();
		Dimension parsize = parent.getSize();

		loc.x += parsize.width  / 2;
		loc.y += parsize.height / 2;

		Dimension mySize = which.getSize();

		loc.x -= mySize.width  / 2;
		loc.y -= mySize.height / 2;

		which.setLocation(loc);
	}

	/**
	 * Add an action to the action map which disposes this dialog
	 * when ESCAPE is pressed.
	 */
	public static void addDisposeOnEscape(final JDialog dia)
	{
		InputMap aof = dia.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		aof.put(KeyStroke.getKeyStroke("ESCAPE"), "dispose");
		dia.getRootPane().getActionMap().put("dispose", new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				dia.dispose();
			}
		});
	}

	/**
	 * Add an action listener which causes the associated dialog
	 * to be disposed.
	 */
	public static void addDisposeOnAction(AbstractButton which, final JDialog dia)
	{
		which.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				dia.dispose();
			}
		});
	}

	/**
	 * Add an action listener which presents a common file dialog.
	 */
	public static void addFileOnAction(
			AbstractButton button,
			final JTextField textField,
			final JDialog parent)
	{
		button.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser chooser = new JFileChooser();

				// try to set the old directory and file
				File old = new File(textField.getText());
				chooser.setSelectedFile(old);

				// set up filters
				FileNameExtensionFilter tiff =
					new FileNameExtensionFilter(
						"TIFF (large images)", "tif", "tiff");
				FileNameExtensionFilter png =
					new FileNameExtensionFilter(
						"PNG & JPG (regular images)", "png", "jpg");

				chooser.addChoosableFileFilter(png);
				chooser.addChoosableFileFilter(tiff);

				// choose current filter
				if (tiff.accept(old))
					chooser.setFileFilter(tiff);
				else if (png.accept(old))
					chooser.setFileFilter(png);
				else
					chooser.setAcceptAllFileFilterUsed(true);

				// fire up the dialog
				int returnVal = chooser.showSaveDialog(parent);
				if (returnVal == JFileChooser.APPROVE_OPTION)
				{
					textField.setText(
							chooser.getSelectedFile().getAbsolutePath());
				}
			}
		});
	}
}
