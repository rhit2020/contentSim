import java.util.ArrayList;



public class TestPassByReference {

	public static void main(String[] args) {
		TestPassByReference test = new TestPassByReference();
		TestObj obj = new TestObj();
		obj.init();
		System.out.println("Before change");
		obj.print();
		ArrayList<String> list = obj.getList();
		System.out.println("local list-before destroy: "+list.size());
		test.destroy(list);
		System.out.println("local list-after destroy: "+list);
		System.out.println("after change");		
		obj.print();
	}
	
	public void destroy(ArrayList<String> list)
	{
		if (list != null)
		{
			for (String s : list)
				s = null;
			list.clear();
			list = null;
		}		
	}
}
