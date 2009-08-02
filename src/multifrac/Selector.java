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

import java.util.*;

/**
 * Maintain multi-selection in ColorizerPanel
 */
public class Selector
{
	private ArrayList<Integer> sel = new ArrayList<Integer>();

	public int select(int i)
	{
		if (!sel.contains(i))
		{
			sel.add(i);
			return i;
		}
		else
		{
			return -1;
		}
	}

	public void unselect(int i)
	{
		sel.remove(new Integer(i));
	}

	public void clear()
	{
		sel.clear();
	}

	public boolean isSelected(int i)
	{
		return sel.contains(i);
	}

	public boolean nothingSelected()
	{
		return sel.isEmpty();
	}

	public boolean single()
	{
		return (sel.size() == 1);
	}

	public boolean pair()
	{
		return (sel.size() == 2);
	}

	public int pair(int i)
	{
		if (!pair())
			return -1;

		if (sel.get(0) == i)
			return sel.get(1);
		else
			return sel.get(0);
	}

	public int firstSelected()
	{
		return sel.get(0);
	}

	public Integer[] getSelected()
	{
		Collections.sort(sel);
		return sel.toArray(new Integer[0]);
	}
}
