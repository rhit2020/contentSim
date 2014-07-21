import java.util.ArrayList;


public class TestObj {

	private ArrayList<String> list = new ArrayList<String>();
	public void init()
	{
		list.add("Roya");
		list.add("Pouya");
	}
	
	public ArrayList<String> getList()
	{
		ArrayList<String> l2 = new ArrayList<String>();
		for (String l : list)
			l2.add(l);
		return l2;
	}

	public void print() {
		for (String s : list)
			System.out.print(s+"  ");
		System.out.println();
	}
}
