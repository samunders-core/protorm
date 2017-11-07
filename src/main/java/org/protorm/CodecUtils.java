package org.protorm;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Iterator;

public class CodecUtils {
	/** @param primitiveType cannot be null
	 * @param value null treated as 0
	 * @return null if value is not assignable/convertible to given type */
	public static Object toPrimitive(Class<?> primitiveType, Number value) {
		if (primitiveType == null) {
			throw new NullPointerException("primitive type required to convert " + value);
		} else if (value == null) {
			value = 0;
		}
		switch (primitiveType.getSimpleName()) {
			case "int": return value.intValue();
			case "long": return value instanceof Long ? value : value.longValue();
			case "boolean": return value.doubleValue() != 0.0;
			case "byte": return value.byteValue();
			case "char": return (char) value.intValue();
			case "short": return value.shortValue();
			case "double": return value instanceof Double ? value : value.doubleValue();
			case "float": return value.floatValue();
			default: return primitiveType.isAssignableFrom(value.getClass()) ? value : null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T proxy(InvocationHandler h, Class<?>... interfaces) {
		return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), interfaces, h);
	}
	
	public static <T> Iterable<T> iterable(final ResetableIterator<T> iterator) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				iterator.reset();
				return new Iterator<T>() {
					private T next = null;
					
					@Override
					public boolean hasNext() {
						try {
							next = iterator.call();
							return true;
						} catch (Exception noMoreElements) {}
						return false;
					}

					@Override
					public T next() {
						return next;
					}
					
					@Override
					public void remove() {}
				};
			}
		};
	}
}
