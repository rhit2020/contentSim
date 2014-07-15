import java.util.TreeSet;
import java.util.Set;

public class TestSetOperations {
	
	public static void main(String[] args)
 {
		TreeSet<Character> set1 = new TreeSet<Character>();
		TreeSet<Character> set2 = new TreeSet<Character>();

		set1.add('A');
		set1.add('B');
		set1.add('C');
		set1.add('D');

		set2.add('C');
		set2.add('D');
		set2.add('E');
		set2.add('F');

		System.out.println("set1: " + set1);
		System.out.println("set2: " + set2);

		System.out.println("Union: " + union(set1, set2));
		System.out.println("Intersection: " + intersection(set1, set2));
		System.out.println("Difference (set1 - set2): "
				+ difference(set1, set2));
		System.out
				.println("Symmetric Difference: " + symDifference(set1, set2));

	}

	public static <T> Set<T> union(Set<T> setA, Set<T> setB) {
		Set<T> tmp = new TreeSet<T>(setA);
		tmp.addAll(setB);
		return tmp;
	}

	public static <T> Set<T> intersection(Set<T> setA, Set<T> setB) {
		Set<T> tmp = new TreeSet<T>();
		for (T x : setA)
			if (setB.contains(x))
				tmp.add(x);
		return tmp;
	}

	public static <T> Set<T> difference(Set<T> setA, Set<T> setB) {
		Set<T> tmp = new TreeSet<T>(setA);
		tmp.removeAll(setB);
		return tmp;
	}

	public static <T> Set<T> symDifference(Set<T> setA, Set<T> setB) {
		Set<T> tmpA;
		Set<T> tmpB;

		tmpA = union(setA, setB);
		tmpB = intersection(setA, setB);
		return difference(tmpA, tmpB);
	}


}
