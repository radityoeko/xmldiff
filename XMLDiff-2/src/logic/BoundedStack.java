package logic;

import java.util.Collections;
import java.util.Stack;

public class BoundedStack<T> extends Stack<T> {
    
    /**
     * 
     */
    private static final long serialVersionUID = -6843033316038441204L;
    private int maxSize;
    
    public BoundedStack(int maxSize) {
	super();
	this.maxSize = maxSize;
    }
    
    public T push(T item) {
	if(this.size() < maxSize)
	    super.push(item);
	else {
	    Stack<T> temp = new Stack<T>();
	    while(this.size() > 0) {
		temp.push(this.pop());
	    }
	    
	    temp.pop();
	    while(temp.size() > 0) {
		this.push(temp.pop());
	    }
	    this.push(item);
	}
	    
	return item;
    }
    
    public T[] toRevertArray(T[] a) {
	Stack<T> temp = this;
	Collections.reverse(temp);
	return temp.toArray(a);
    }

}
