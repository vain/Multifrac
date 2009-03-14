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

		//dump();
	}

	public void pop()
	{
		if (undo.isEmpty())
			return;

		// Save the current element onto the redo stack
		redo.offerFirst(current);

		// Restore the last saved element
		current = undo.pollFirst();

		//dump();
	}

	public void unpop()
	{
		if (redo.isEmpty())
			return;

		// Re-place the current item on the undo-stack
		undo.offerFirst(current);
		
		// Re-activate first redo-item
		current = redo.pollFirst();

		//dump();
	}

	private void dump()
	{
		System.out.println("CURRENT:" + current);
		System.out.println("UNDO STACK:");
		System.out.println(undo);
		System.out.println("REDO STACK:");
		System.out.println(redo);
		System.out.println();
	}
}
