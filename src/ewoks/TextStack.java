package ewoks;

import java.util.Stack;

// TextStack is a class that allows for efficient storage of a sequence of text with runs of identical elements
// Has methods for popping, pushing and reseting
public class TextStack {
	/**We use [] to denote a sequence and assume haskell functions on lists work the same way on sequences
	   TextStack: [Text.Immutable]  */
	   
	/**ABS: TextStack = concat [repeat i txt | (i,txt) <-count.toseq 'zip' str.toseq ]  */ 
	//List of strings in the stack
	private Stack<Text.Immutable> str = new Stack<Text.Immutable>();
	//The number of times a string in str appears in TextStack 
	private Stack<Integer> count = new Stack<Integer>();
	
	
	/**Adds txt to the beginning of the sequence */
	// TextStack=[txt]++TextStack_0 
	public void push(Text.Immutable txt){
		if (str.isEmpty()||str.peek()!=txt) {
			str.push(txt); 
			count.push(1);
			
		}
		else {count.push(count.pop()+1);}
	}
	
	/**Removes and returns the head of the sequence */
	// TextStack= tail TextStack_0 && Return = head TextStack_0 
	public Text.Immutable pop() {
		assert(!str.isEmpty());
		if (count.peek()!=1) {
			count.push(count.pop()-1); 
			return str.peek();
		}
		else {
			count.pop(); 
			return str.pop();
		}
	}
	
	/** Sets the sequence to [] */
	//TextStack =[]
	public void reset() {
		str.removeAllElements(); count.removeAllElements();
	}
	

}