package sam_.protorm;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

/** @param <T> output data type, default implementation operates with {@link OutputStream} */
public interface LayerEncoder<T> extends FieldAccess {
	/** @param output cannot be null
	 * @param layers cannot be null/immutable
	 * @return number of layers valid/stored into given list
	 * @throws IOException */
	@SuppressWarnings("unchecked")
	default int encode(T output, List<LayerEncoder<T>> layers) throws IOException {
		int level = 0;
		try {
			for (LayerEncoder<T> payload = encodeFieldsFromSuperclass(output, level, (Class<? super LayerEncoder<?>>) getClass()); payload != null; payload = payload.encodeFieldsFromSuperclass(output, level, (Class<? super LayerEncoder<?>>) payload.getClass())) {
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
	
	/** @param output cannot be null
	 * @param level order of layer within current PDU, 0 or above
	 * @param clazz fields from which super/class of current instance to encode, cannot be null
	 * @return null to return from {@link #encode(Object, List)}
	 * @throws IOException */
	@SuppressWarnings("unchecked")
	default <E extends Throwable> LayerEncoder<T> encodeFieldsFromSuperclass(T output, int level, Class<? super LayerEncoder<?>> clazz) throws IOException, E {
		LayerEncoder<T> candidate = !Object.class.equals(clazz.getSuperclass()) ? encodeFieldsFromSuperclass(output, level, clazz.getSuperclass()) : null;
		Object result = null;
		Map<AccessibleObject, Class<?>> fields = Types.ofFields(clazz);
		try {
			for (Map.Entry<AccessibleObject, Class<?>> entry : fields.entrySet()) {
				result = entry.getKey() instanceof Method && entry.getValue().isInstance(output) ? call((Method) entry.getKey(), output) : encodeField(output, entry.getKey(), entry.getKey().getAnnotation(Field.class), entry.getValue());
			}
			return result instanceof LayerEncoder<?> ? LayerEncoder.class.cast(result) : candidate;
		} catch (InvocationTargetException | IllegalAccessException e) {
			throw (E) e;
		}
	}
	
	/** override if output input other than {@link OutputStream}
	 * @param output cannot be null, default implementation assumes {@link OutputStream}
	 * @param member cannot be null
	 * @param field cannot be null
	 * @param type cannot be null
	 * @return current value of field, can be null
	 * @throws IOException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException */
	default Object encodeField(T output, AccessibleObject member, Field field, Class<?> type) throws IOException, InvocationTargetException, IllegalAccessException {
		if (!OutputStream.class.isInstance(output)) {
			throw new Error("Override " + new Object() {}.getClass().getEnclosingMethod());
		}
		int byteCount = field.byteCount() > 0 ? field.byteCount() : Types.valueOf(type).byteCount();
		Object value = get(member, type);
		Types.write((OutputStream) output, byteCount, field.littleEndian() ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN, value);
		return value;	// type is either [Bb]oolean, [Cc]har(acter)?, Number, byte[]
	}
}
