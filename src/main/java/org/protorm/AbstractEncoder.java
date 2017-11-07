package org.protorm;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

abstract class AbstractEncoder<RO_View extends Annotation, RW_Model extends Annotation/* RO_View */ & Encoder<RO_View, RW_Model>> extends DataInputStream implements Encoder<RO_View, RW_Model>, InvocationHandler, BufferProvider {
	protected final Map<Method, Object> values = new LinkedHashMap<>();
	private Method toSet = null;
	public final RW_Model proxy;
	
	@SuppressWarnings("unchecked")
	public AbstractEncoder() {
		super(new InputStream() {
			@Override
			public int read() throws IOException {
				return 0;
			}
		});
		proxy = CodecUtils.proxy(this, (Class<RW_Model>) ((ParameterizedType) getClass().getTypeParameters()[1]).getRawType());
	}
	
	protected Object defaultValue(Method m) throws Throwable {
		for (Object result = m.getDefaultValue(); result != null; ) {
			return result;
		}
		Class<?> type = m.getReturnType();
		if (String.class.equals(type)) {
			return "";
		} else if (type.isEnum()) {
			return type.getEnumConstants()[0];
		} else if (type.isArray()) {
			return Array.newInstance(type.getComponentType(), 0);
		}	// else annotation/class/primitive
		return CodecUtils.toPrimitive(type, 0);	// null for annotation/class
	}
	
	@Override
	public String toString() {
		return values.toString();	// TODO:
	}

	@Override
	public RW_Model initFrom(RO_View init) {
		AbstractEncoder<?, ?> h = (AbstractEncoder<?, ?>) Proxy.getInvocationHandler(init);
		values.clear();
		values.putAll(h.values);
		return proxy;
	}
	
	private RW_Model set(Object value) {
		values.put(toSet, value);
		return proxy;
	}

	@Override
	public RW_Model set(boolean key, Boolean value) {
		return set(value);
	}

	@Override
	public RW_Model set(byte key, Byte value) {
		return set(value);
	}

	@Override
	public RW_Model set(char key, Character value) {
		return set(value);
	}

	@Override
	public RW_Model set(short key, Short value) {
		return set(value);
	}

	@Override
	public RW_Model set(int key, Integer value) {
		return set(value);
	}

	@Override
	public RW_Model set(long key, Long value) {
		return set(value);
	}

	@Override
	public RW_Model set(float key, Float value) {
		return set(value);
	}

	@Override
	public RW_Model set(double key, Double value) {
		return set(value);
	}

	@Override
	public RW_Model set(String key, String value) {
		return set(value);
	}

	@Override
	public <C> RW_Model set(Class<C> key, Class<? extends C> value) {
		return set(value);
	}

	@Override
	public <E extends Enum<E>> RW_Model set(E key, E value) {
		return set(value);
	}

	@Override
	public <A extends Annotation> RW_Model set(A key, A value) {
		return set(value);
	}
	
	@Override
	public RW_Model set(boolean[] key, boolean... value) {
		return set(value);
	}

	@Override
	public RW_Model set(byte[] key, byte... value) {
		return set(value);
	}

	@Override
	public RW_Model set(char[] key, char... value) {
		return set(value);
	}

	@Override
	public RW_Model set(short[] key, short... value) {
		return set(value);
	}

	@Override
	public RW_Model set(int[] key, int... value) {
		return set(value);
	}

	@Override
	public RW_Model set(long[] key, long... value) {
		return set(value);
	}

	@Override
	public RW_Model set(float[] key, float... value) {
		return set(value);
	}

	@Override
	public RW_Model set(double[] key, double... value) {
		return set(value);
	}

	@Override
	public RW_Model set(String[] key, String... value) {
		return set(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <C> RW_Model set(Class<? extends C>[] key, Class<? extends C>... value) {
		return set(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <E extends Enum<E>> RW_Model set(E[] key, E... value) {
		return set(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <A extends Annotation> RW_Model set(A[] key, A... value) {
		return set(value);
	}
	
	@SuppressWarnings("unchecked")
	private <T> List<T> list(int index, T value) {
		List<T> values = (List<T>) this.values.get(toSet);
		if (values == null) {
			this.values.put(toSet, values = new ArrayList<>(index + 1));
		}
		for (; values.size() <= index; values.add(null));
		return values;
	}

	@Override
	public RW_Model set(int index, boolean[] key, boolean value) {
		list(index, value).set(index, value);
		return proxy;
	}

	@Override
	public RW_Model set(int index, byte[] key, byte value) {
		list(index, value).set(index, value);
		return proxy;
	}

	@Override
	public RW_Model set(int index, char[] key, char value) {
		list(index, value).set(index, value);
		return proxy;
	}

	@Override
	public RW_Model set(int index, short[] key, short value) {
		list(index, value).set(index, value);
		return proxy;
	}

	@Override
	public RW_Model set(int index, int[] key, int value) {
		list(index, value).set(index, value);
		return proxy;
	}

	@Override
	public RW_Model set(int index, long[] key, long value) {
		list(index, value).set(index, value);
		return proxy;
	}

	@Override
	public RW_Model set(int index, float[] key, float value) {
		list(index, value).set(index, value);
		return proxy;
	}

	@Override
	public RW_Model set(int index, double[] key, double value) {
		list(index, value).set(index, value);
		return proxy;
	}

	@Override
	public RW_Model set(int index, String[] key, String value) {
		list(index, value).set(index, value);
		return proxy;
	}

	@Override
	public <C> RW_Model set(int index, Class<? super C>[] key, Class<C> value) {
		list(index, value).set(index, value);
		return proxy;
	}

	@Override
	public <E extends Enum<E>> RW_Model set(int index, E[] key, E value) {
		list(index, value).set(index, value);
		return proxy;
	}

	@Override
	public <A extends Annotation> RW_Model set(int index, A[] key, A value) {
		list(index, value).set(index, value);
		return proxy;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		for (Class<?> iface : getClass().getInterfaces()) {
			if (iface.equals(method.getDeclaringClass()) || Object.class.equals(method.getDeclaringClass())) {
				return method.invoke(this, args);
			}
		}
		return defaultValue(toSet = method);
	}
}