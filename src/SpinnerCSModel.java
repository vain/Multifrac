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
import javax.swing.event.*;

/**
 * Spinner [C]yclic [S]ilent Model
 */
public class SpinnerCSModel extends SpinnerNumberModel
{
	private int min, max, step = 0;
	private int cur = 0;

	public SpinnerCSModel(
			int def,
			int min,
			int max,
			int step)
	{
		super(def, min, max, step);

		this.min = min;
		this.max = max;
		this.step = step;

		this.cur = def;
	}

	/**
	 * Reduce a mod m. Also supports negative values.
	 */
	private int safemod(int a, int m)
	{
		a %= m;
		if (a < 0)
			a = m + a;

		return a;
	}

	public int getIntValue()
	{
		//return getNumber().intValue();
		return cur;
	}

	public void setValueSilent(int v, ChangeListener exclude)
	{
		if (v != cur)
		{
			// Copy value
			cur = v;

			// Inform all listeners - except one.
			ChangeEvent e = null;
			ChangeListener[] list = getChangeListeners();
			for (ChangeListener c : list)
			{
				if (c != exclude)
				{
					if (e == null)
						e = new ChangeEvent(this);

					c.stateChanged(e);
				}
			}
		}
	}

	@Override
	public Object getNextValue()
	{
		return new Integer(safemod(cur + step, max));
	}

	@Override
	public Object getPreviousValue()
	{
		return new Integer(safemod(cur - step, max));
	}

	@Override
	public Object getValue()
	{
		return new Integer(cur);
	}

	@Override
	public void setValue(Object value) throws IllegalArgumentException
	{
		if (!(value instanceof Integer))
			throw new IllegalArgumentException("Not an Integer: " + value);

		int nval = (Integer)value;
		if (nval != cur)
		{
			// Copy the value and inform all listeners.
			cur = nval;
			fireStateChanged();
		}
	}
}
