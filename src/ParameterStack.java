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

import java.util.Deque;
import java.util.ArrayDeque;

public class ParameterStack
{
	private FractalParameters current = new FractalParameters();
	private Deque<FractalParameters> undo = new ArrayDeque<FractalParameters>();
	private Deque<FractalParameters> redo = new ArrayDeque<FractalParameters>();

	public FractalParameters get()
	{
		//dump();

		return current;
	}

	public void push()
	{
		// Save current element
		undo.offerFirst(current);

		// Clear redo stack, when the user makes a change
		redo.clear();

		// Create a copy of it and set this copy as the current element
		current = new FractalParameters(current);

		dump();
	}

	public void pop()
	{
		if (undo.isEmpty())
			return;

		// Save the current element onto the redo stack
		redo.offerFirst(current);

		// Restore the last saved element
		current = undo.pollFirst();

		dump();
	}

	public void unpop()
	{
		if (redo.isEmpty())
			return;

		// Re-place the current item on the undo-stack
		undo.offerFirst(current);
		
		// Re-activate first redo-item
		current = redo.pollFirst();

		dump();
	}

	public void clear(FractalParameters top)
	{
		undo.clear();
		redo.clear();
		current = top;

		dump();
	}

	private void dump()
	{
		System.out.println("UNDO: " + undo.size());
		System.out.println("REDO: " + redo.size());

		/*
		System.out.println("CURRENT:" + current);
		System.out.println("UNDO STACK:");
		System.out.println(undo);
		System.out.println("REDO STACK:");
		System.out.println(redo);
		System.out.println();
		*/
	}
}
