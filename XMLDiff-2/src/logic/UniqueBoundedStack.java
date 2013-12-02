package logic;

public class UniqueBoundedStack<T> extends BoundedStack<T> {

    public UniqueBoundedStack(int maxSize) {
	super(maxSize);
    }

    /**
     * 
     */
    private static final long serialVersionUID = 9150234454798325545L;

    public T push(T item) {
	if(this.contains(item)) {
	    this.remove(item);
	}
	
	super.push(item);
	
	return item;
	
    }
}
