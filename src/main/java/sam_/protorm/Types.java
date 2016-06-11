package sam_.protorm;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public enum Types {
	INTEGER(Integer.class, Integer.SIZE, void.class) {
		@Override
		public Object cast(Object value) {
			return super.cast(value instanceof Number ? ((Number) value).intValue() : value);
		}
	}, DOUBLE(Double.class, Double.SIZE, byte.class) {
		@Override
		public Object cast(Object value) {
			return super.cast(value instanceof Number ? ((Number) value).doubleValue() : value);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public <T, E extends Throwable> T read(InputStream inputStream, ByteOrder byteOrder, Object byteCount) throws IOException, E {
			return (T) Object.class.cast(Double.longBitsToDouble((long) LONG.read(inputStream, byteOrder, byteCount)));
		}
		
		@Override
		protected long toLong(Object value) {
			return Double.doubleToLongBits(((Number) value).doubleValue());
		}
	}, BYTE(Byte.class, Byte.SIZE, short.class) {
		@Override
		public Object cast(Object value) {
			return super.cast(value instanceof Number ? ((Number) value).byteValue() : value);
		}
	}, BOOLEAN(Boolean.class, Byte.SIZE, int.class) {
		@Override
		public Object cast(Object value) {
			return super.cast(value instanceof Number ? ((Number) value).doubleValue() != 0.0 : value);
		}
		
		@Override
		protected long toLong(Object value) {
			return Boolean.FALSE.equals(value) ? 0 : 1;
		}
	}, CHARACTER(Character.class, Character.SIZE, int.class) {
		@Override
		public Object cast(Object value) {
			return super.cast(value instanceof Number ? (char) ((Number) value).intValue() : value);
		}
		
		@Override
		protected long toLong(Object value) {
			return ((Character) value).charValue();
		}
	}, VOID(Void.class, 0, long.class) {
		@Override
		public Object cast(Object value) {
			return null;
		}
		
		@Override
		public <T, E extends Throwable> T read(InputStream inputStream, ByteOrder byteOrder, Object byteCount) throws IOException, E {
			throw new EOFException();
		}
		
		@Override
		protected long toLong(Object value) {
			throw new UnsupportedOperationException();
		}
	}, SHORT(Short.class, Short.SIZE, long.class) {
		@Override
		public Object cast(Object value) {
			return super.cast(value instanceof Number ? ((Number) value).shortValue() : value);
		}
	}, FLOAT(Float.class, Float.SIZE, long.class) {
		@Override
		public Object cast(Object value) {
			return super.cast(value instanceof Number ? ((Number) value).floatValue() : value);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public <T, E extends Throwable> T read(InputStream inputStream, ByteOrder byteOrder, Object byteCount) throws IOException, E {
			return (T) Object.class.cast(Float.intBitsToFloat((int) INTEGER.read(inputStream, byteOrder, byteCount)));
		}
		
		@Override
		protected long toLong(Object value) {
			return Float.floatToIntBits(((Number) value).floatValue());
		}
	}, LONG(Long.class, Long.SIZE, long.class) {
		@Override
		public Object cast(Object value) {
			return super.cast(value instanceof Number ? ((Number) value).longValue() : value);
		}
	}, OBJECT(Object.class, -1, byte[].class) {
		@Override
		public Object cast(Object value) {
			return value;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T, E extends Throwable> T read(InputStream inputStream, ByteOrder byteOrder, Object byteCount) throws IOException, E {
			if (byteCount == null || (byteCount instanceof Number && ((Number) byteCount).intValue() <= 0)) {
				if (inputStream instanceof ObjectInputStream) {
					try {
						return (T) ((ObjectInputStream) inputStream).readObject();
					} catch (ClassNotFoundException e) {
						throw (E) e;
					}
				}
				throw new UnsupportedOperationException();
			} else if (byteCount instanceof Number) {
				byteCount = new byte[((Number) byteCount).intValue()];
			}
			return (T) readFully(inputStream, (byte[]) byteCount);
		}
		
		private byte[] readFully(InputStream inputStream, byte[] b) throws IOException {
	        for (int n = 0; n < b.length; ) {
	            int count = inputStream.read(b, n, b.length - n);
	            for (n += count; count < 0; ) {
	                throw new EOFException();
	            }
	        }
	        return b;
		}
		
		@Override
		protected long toLong(Object value) {
			throw new UnsupportedOperationException();
		}
	};
	private static final Map<Class<?>, SortedMap<AccessibleObject, Class<?>>> FIELD_TYPES = new ConcurrentHashMap<>();
	
	private final Class<?> wrapperClass;
	private final int byteCount;
	private final Class<?> primitiveLargeEnoughForOrdinalByteCount;
	
	private Types(Class<?> wrapperClass, int bitCount, Class<?> primitiveLargeEnoughForOrdinalByteCount) {
		this.wrapperClass = wrapperClass;
		this.byteCount = bitCount / Byte.SIZE;
		this.primitiveLargeEnoughForOrdinalByteCount = primitiveLargeEnoughForOrdinalByteCount;
	}
	
	public Class<?> wrapperClass() {
		return wrapperClass;
	}
	
	public int byteCount() {
		return byteCount;
	}
	
	public Class<?> primitiveLargeEnoughForOrdinalByteCount() {
		return primitiveLargeEnoughForOrdinalByteCount;
	}
	
	/** attempt best effort cast of given argument to current {@link #wrapperClass()}
	 * @param value
	 * @return value given if null or not a primitive wrapper */
	public Object cast(Object value) {
		return value == null || wrapperClass().isInstance(value) ? value : cast(valueOf(value.getClass()).toLong(value));
	}
	
	protected long toLong(Object value) {
		return ((Number) value).longValue();
	}
	
	/** @param inputStream cannot be null
	 * @param byteOrder null treated as {@link ByteOrder#BIG_ENDIAN}
	 * @param byteCount only instances of either byte[] or {@link Number} allowed; null or 0 or below uses current instance {@link #byteCount()}
	 * @return never null, either {@link Number} or {@link Boolean} or {@link Character} or byte array
	 * @throws IOException {@link EOFException} for {@link #VOID}, {@link UnsupportedOperationException} for {@link #OBJECT} */
	@SuppressWarnings("unchecked")
	public <T, E extends Throwable> T read(InputStream inputStream, ByteOrder byteOrder, Object byteCount) throws IOException, E {
		int count = byteCount instanceof Number ? ((Number) byteCount).intValue() : -1;
		return (T) cast(read(inputStream, byteOrder, count <= 0 ? byteCount() : Math.min(count, byteCount())));
	}
	
	private static long read(InputStream inputStream, ByteOrder byteOrder, int count) throws IOException {
		for (int ch = inputStream.read(); ch >= 0; ) {
			return count > 1 ? read(inputStream, byteOrder, count - 1, ch) : ch;
		}
		throw new EOFException();
	}
	
	private static long read(InputStream inputStream, ByteOrder byteOrder, int count, int previous) throws IOException {
		long result = read(inputStream, byteOrder, count - 1);
		return ByteOrder.LITTLE_ENDIAN.equals(byteOrder) ? (result << 8) + previous : (result + (previous << 8));
	}
	
	/** @param outputStream cannot be null
	 * @param byteCount 0 or below auto-detects from given value
	 * @param byteOrder null treated as {@link ByteOrder#BIG_ENDIAN}, applied to primitive wrappers only
	 * @param value can be null or either {@link Number} or {@link Boolean} or {@link Character} or {@link byte[]}
	 * @throws IOException {@link EOFException} for {@link #OBJECT} */
	public static void write(OutputStream outputStream, int byteCount, ByteOrder byteOrder, Object value) throws IOException {
		if (byteCount <= 0) {
			for (byteCount = Types.valueOf(value.getClass()).byteCount(); byte[].class.isInstance(value); ) {
				outputStream.write(byte[].class.cast(value));
				return;
			}
		}
		if (value == null) {
			if (byteCount <= 0) {
				return;
			}
			throw new IllegalArgumentException("Cannot write " + byteCount + " bytes out of null value");
		} else if (byte[].class.isInstance(value)) {
			if (byte[].class.cast(value).length < byteCount) {
				throw new IllegalArgumentException("Cannot write " + byteCount + " bytes out of only " + byte[].class.cast(value).length + " bytes");
			}
			outputStream.write(byte[].class.cast(value), 0, byteCount);
			return;
		} else if (byteCount >= values().length - 1) {
			throw new IllegalArgumentException("Cannot write " + byteCount + " bytes out of primitive value " + value);
		}	// else definitely primitive/boolean/character
		writeByte(outputStream, byteCount, byteOrder, valueOf(value.getClass()).toLong(value));
	}
	
	private static void writeByte(OutputStream outputStream, int byteCount, ByteOrder byteOrder, long value) throws IOException {
		if (ByteOrder.LITTLE_ENDIAN.equals(byteOrder)) {
			outputStream.write((int) (value % 256));
		}
		if (byteCount > 1) {
			writeByte(outputStream, byteCount - 1, byteOrder, value / 256);
		}
		if (!ByteOrder.LITTLE_ENDIAN.equals(byteOrder)) {
			outputStream.write((int) (value % 256));
		}
	}
	
	/** get type of members annotated with {@link Field} regardless of whether it's {@link java.lang.reflect.Field} or {@link Method}, ordered by {@link Field#value()}<br>
	 * Annotated methods need to have exactly 1 non-primitive argument returning primitive (non-void), byte array or {@link FieldAccess}
	 * @param clazz cannot be null
	 * @return immutable, annotated members (both {@link java.lang.reflect.Field} or {@link Method}) used as keys, never null
	 * @throws Error with each constraint-violating method */
	private static SortedMap<AccessibleObject, Class<?>> fieldTypes(Class<?> clazz) {
		SortedMap<AccessibleObject, Class<?>> result = new TreeMap<>(FieldComparator.INSTANCE);
		for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
			if (field.isAnnotationPresent(Field.class)) {
				field.setAccessible(true);
				result.put(field, field.getType());
			}
		}
		Error error = null;
		for (Method method : clazz.getDeclaredMethods()) {
			Field field = method.getAnnotation(Field.class);
			if (field != null && !method.isBridge()) {
				if (method.getParameterTypes().length == 1 && !method.getParameterTypes()[0].isPrimitive() && 
						((method.getReturnType().isPrimitive() && !void.class.equals(method.getReturnType())) || byte[].class.equals(method.getReturnType()) || FieldAccess.class.isAssignableFrom(method.getReturnType()))) {
					if (byte[].class.isAssignableFrom(method.getParameterTypes()[0]) && field.byteCount() <= 0) {
						throw new IllegalStateException("byte array fields with unknown length not supported, specify size for " + method);
					}
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
	
	public static SortedMap<AccessibleObject, Class<?>> ofFields(Class<?> clazz) {
		SortedMap<AccessibleObject, Class<?>> fields = FIELD_TYPES.get(clazz);
		if (fields == null) {
			for (fields = FIELD_TYPES.putIfAbsent(clazz, fieldTypes(clazz)); fields == null; fields = FIELD_TYPES.get(clazz));
		}
		return fields;
	}

	/** primitive/wrapper class-based lookup<br>
	 * see <a href="https://github.com/melezov/runtime-bytegen/blob/master/src/main/java/org/revenj/Primitives.java">https://github.com/melezov/runtime-bytegen/blob/master/src/main/java/org/revenj/Primitives.java</a>
	 * @param clazz cannot be null
	 * @return never null */
	public static Types valueOf(Class<?> clazz) {
		String name = clazz.getName();
		int base = clazz.isPrimitive() ? 0 : "java.lang.".length(), c0 = name.charAt(base), c2 = name.charAt(base + 2), index = (c0 + c0 + c0 + 5) & (118 - c2);
		if (base == 0 || (index < values().length - 1 && clazz.equals(values()[index].wrapperClass()))) {
			return values()[index];
		}
		for (Types primitives : values()) {
			if (primitives.wrapperClass().isAssignableFrom(clazz)) {
				return primitives;
			}
		}
		throw new IllegalArgumentException("BUG: " + clazz);
	}
}
