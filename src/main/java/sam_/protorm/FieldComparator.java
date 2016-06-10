package sam_.protorm;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

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
	
	/** get type of members annotated with {@link Field} regardless of whether it's {@link java.lang.reflect.Field} or {@link Method}, ordered by {@link Field#value()}<br>
	 * Annotated methods need to have exactly 1 non-primitive argument returning primitive (non-void), byte array or {@link Layer}
	 * @param clazz cannot be null
	 * @return immutable, annotated members (both {@link java.lang.reflect.Field} or {@link Method}) used as keys, never null
	 * @throws Error with each constraint-violating method */
	public SortedMap<AccessibleObject, Class<?>> fieldTypes(Class<? extends Layer<?>> clazz) {
		SortedMap<AccessibleObject, Class<?>> result = new TreeMap<>(this);
		for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
			if (field.isAnnotationPresent(Field.class)) {
				field.setAccessible(true);
				result.put(field, field.getType());
			}
		}
		Error error = null;
		for (Method method : clazz.getDeclaredMethods()) {
			if (method.isAnnotationPresent(Field.class) && !method.isBridge()) {
				if (method.getParameterTypes().length == 1 && !method.getParameterTypes()[0].isPrimitive() && 
						((method.getReturnType().isPrimitive() && !void.class.equals(method.getReturnType())) || byte[].class.equals(method.getReturnType()) || Layer.class.isAssignableFrom(method.getReturnType()))) {
					method.setAccessible(true);
					result.put(method, method.getParameterTypes()[0]);
				} else {
					error =  new Error("Only methods with exactly 1 non-primitive argument returning either primitive (non-void), byte array or Layer<?> supported, check " + method, error);
				}
			}
		}
		if (error != null) {
			throw error;
		}
		return result;
	}
}
