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
import java.awt.*;

public class RenderNetConsole extends JDialog
{
	protected final JTextArea con = new JTextArea();
	protected final JButton close = new JButton("Close");

	/**
	 * Construct the dialog.
	 */
	public RenderNetConsole(JDialog parent)
	{
		super(parent, "Console", true);

		// Components
		SimpleGridBag sgbMain = new SimpleGridBag(getContentPane());
		setLayout(sgbMain);

		JScrollPane scroller = new JScrollPane(con);
		scroller.setPreferredSize(new Dimension(400, 300));
		con.setEditable(false);

		sgbMain.add(scroller, 0, 0, 1, 1, 1.0, 1.0);
		sgbMain.add(close,    0, 1, 1, 1, 1.0, 1.0);

		// Ways (not) to close this dialog
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		CompHelp.addDisposeOnAction(close, this);
		close.setEnabled(false);

		// Finish
		pack();
		CompHelp.center(this, parent);
	}

	/**
	 * Start the actual process.
	 */
	public boolean start()
	{
		setVisible(true);
		return true;
	}

	/**
	 * Print something on the console (threadsafe).
	 */
	synchronized public void println(String s)
	{
		con.append(s + "\n");
	}

	/**
	 * This has to be called when one client is done.
	 */
	public void release()
	{
	}
}
