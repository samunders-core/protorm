package sam_.protorm;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.util.Comparator;

public enum FieldComparator implements Comparator<AccessibleObject> {
	INSTANCE;
	
	@Override
	public int compare(AccessibleObject o1, AccessibleObject o2) {
		for (int result = compareByDepth(((Member) o1).getDeclaringClass(), ((Member) o2).getDeclaringClass()); result != 0; ) {
			return result;
		}
		return Integer.compare(o1.getAnnotation(Field.class).value(), o2.getAnnotation(Field.class).value());
	}

	private int compareByDepth(Class<?> c1, Class<?> c2) {
		if (c1.getSuperclass() == null) {
			return c2.getSuperclass() == null ? 0 : -1;
		}
		return c2.getSuperclass() == null ? 1 : compareByDepth(c1.getSuperclass(), c2.getSuperclass());
	}
}
