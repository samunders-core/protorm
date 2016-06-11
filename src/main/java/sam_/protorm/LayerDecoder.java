package sam_.protorm;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

/** @param <T> input data type, default implementation operates with {@link InputStream} */
public interface LayerDecoder<T> extends FieldAccess {
	/** @param input cannot be null
	 * @param layers cannot be null/immutable
	 * @return number of layers valid/stored into given list
	 * @throws IOException */
	@SuppressWarnings("unchecked")
	default int decode(T input, List<LayerDecoder<T>> layers) throws IOException {
		int level = 0;
		try {
			for (LayerDecoder<T> payload = decodeFieldsFromSuperclass(input, level, (Class<? super LayerDecoder<?>>) getClass()); payload != null; payload = payload.decodeFieldsFromSuperclass(input, level, (Class<? super LayerDecoder<?>>) payload.getClass())) {
				if (++level < layers.size()) {
					layers.set(level, payload);
				} else {
					layers.add(payload);
				}
			}
			return 1 + level;
		} catch (EOFException e) {
			return level;
		}
	}
	
	/** @param input cannot be null
	 * @param level order of layer within current PDU, 0 or above
	 * @param clazz fields from which super/class of current instance to decode, cannot be null
	 * @return null to return from {@link #decode(Object, List)}
	 * @throws IOException */
	@SuppressWarnings("unchecked")
	default <E extends Throwable> LayerDecoder<T> decodeFieldsFromSuperclass(T input, int level, Class<? super LayerDecoder<?>> clazz) throws IOException, E {
		LayerDecoder<T> candidate = !Object.class.equals(clazz.getSuperclass()) ? decodeFieldsFromSuperclass(input, level, clazz.getSuperclass()) : null;
		Object result = null;
		Map<AccessibleObject, Class<?>> fields = Types.ofFields(clazz);
		try {
			for (Map.Entry<AccessibleObject, Class<?>> entry : fields.entrySet()) {
				result = entry.getKey() instanceof Method && entry.getValue().isInstance(input) ? call((Method) entry.getKey(), input) : decodeField(input, entry.getKey(), entry.getKey().getAnnotation(Field.class), entry.getValue());
			}
			return result instanceof LayerDecoder<?> ? LayerDecoder.class.cast(result) : candidate;
		} catch (InvocationTargetException | IllegalAccessException e) {
			throw (E) e;
		}
	}
	
	/** override if using input other than {@link InputStream}
	 * @param input cannot be null, default implementation assumes {@link InputStream}
	 * @param member cannot be null
	 * @param field cannot be null
	 * @param type cannot be null
	 * @return decoded value, can be null
	 * @throws IOException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException */
	default Object decodeField(T input, AccessibleObject member, Field field, Class<?> type) throws IOException, InvocationTargetException, IllegalAccessException {
		if (!InputStream.class.isInstance(input)) {
			throw new Error("Override " + new Object() {}.getClass().getEnclosingMethod());
		}
		Class<?> largerPrimitive = field.byteCount() <= 0 ? type : Types.values()[Math.min(field.byteCount(), Types.values().length - 1)].primitiveLargeEnoughForOrdinalByteCount();
		Object byteCount = createByteCount(member, type, field.byteCount(), get(member, null));
		Object value = Types.valueOf(largerPrimitive).read((InputStream) input, field.littleEndian() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, byteCount);
		return apply(member, Types.valueOf(type).cast(value));
	}
	
	static Object createByteCount(AccessibleObject member, Class<?> type, int byteCount, Object previous) throws InvocationTargetException, IllegalAccessException {
		if (byte[].class.isInstance(previous) && byteCount > 0 && ((byte[]) previous).length == byteCount) {
			return previous;
		} else if (byte[].class.equals(type) && byteCount <= 0) {	// definitely class field
			throw new IllegalStateException("byte array fields with unknown length not supported, either initialize " + member + " or specify its size");
		}
		return byteCount;
	}
}
