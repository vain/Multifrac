import java.util.Deque;
import java.util.ArrayDeque;

public class ParameterStack
{
	private FractalParameters current = new FractalParameters();
	private Deque<FractalParameters> stack = new ArrayDeque<FractalParameters>();
	private Deque<FractalParameters> redo  = new ArrayDeque<FractalParameters>();

	public FractalParameters get()
	{
		//dump();

		return current;
	}

	public void push()
	{
		// Save current element
		stack.offerFirst(current);

		// Clear redo stack
		//redo.clear();

		// Create a copy of it and set this copy as the current element
		current = new FractalParameters(current);

		//dump();
	}

	public void pop()
	{
		if (stack.isEmpty())
			return;

		// Save the current element onto the redo stack
		//redo.offerFirst(current);

		// Restore the last saved element
		current = stack.pollFirst();

		dump();
	}

	public void unpop()
	{
		dump();
	}

	private void dump()
	{
		System.out.println("CURRENT:" + current);
		System.out.println("UNDO STACK:");
		System.out.println(stack);
		System.out.println("REDO STACK:");
		System.out.println(redo);
		System.out.println();
	}
}
