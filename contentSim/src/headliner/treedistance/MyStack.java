package headliner.treedistance;

/**
 * 
 * @author Roya
 * This class was written for an alternative approach for non-recursive implementation of the postorder traversal
 * method used in the TreeDefinition class
 */
public class MyStack {

	
	   private int maxSize;
	   private String[] stackArray;
	   private int top;
	   public MyStack(int s) {
	      maxSize = s;
	      stackArray = new String[maxSize];
	      top = -1;
	   }
	   public void push(String j, String temp) {
		  stackArray[++top] = j;
	   }
	   public String pop() {
	      return stackArray[top--];
	   }
	   public String peek() {
	      return stackArray[top];
	   }
	   public boolean isEmpty() {
	      return (top == -1);
	   }
	   public boolean isFull() {
	      return (top == maxSize - 1);
	   }
	   public static void main(String[] args) {
	      MyStack theStack = new MyStack(10); 
//	      theStack.push("10",);
//	      theStack.push("20");
//	      theStack.push("30");
//	      theStack.push("40");
//	      theStack.push("50");
	      while (!theStack.isEmpty()) {
	         String value = theStack.pop();
	         System.out.print(value);
	         System.out.print(" ");
	      }
	      System.out.println("");
	   }
	public void clear() {
		if (stackArray!= null)
			stackArray = null;		
	}

}
