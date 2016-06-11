package sam_.protorm;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface FieldAccess {
	static final Logger log = LoggerFactory.getLogger(FieldAccess.class);
	
	/** either call given method with given value as single argument or set given field to given value
	 * @param member cannot be null, only {@link Method} (accepting single argument) and {@link Field} accepted
	 * @param value can be null
	 * @return call result if method given or given value if field given; can be null
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException */
	default <T> T apply(AccessibleObject member, T value) throws IllegalAccessException, InvocationTargetException {
		return member instanceof Field ? set((Field) member, value) : call((Method) member, value);
	}
	
	/** either get value of given field or result of calling given method with null as single argument (provided argumentType is given)
	 * @param member cannot be null
	 * @param callArgumentType can be null
	 * @return definitely null if method given without argument type
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException */
	@SuppressWarnings("unchecked")
	default <T> T get(AccessibleObject member, Class<T> callArgumentType) throws IllegalAccessException, InvocationTargetException {
		if (member instanceof Field) {
			return (T) ((Field) member).get(this);
		}
		return callArgumentType != null ? call((Method) member, callArgumentType.cast(null)) : null;
	}
	
	/** invoke given member with single argument
	 * @param member cannot be null
	 * @param argument can be null
	 * @return result of {@link Method#invoke(Object, Object...)}
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException */
	@SuppressWarnings("unchecked")
	default <T> T call(Method member, Object argument) throws IllegalAccessException, InvocationTargetException {
		log.debug("{}.{}({})", this, member, argument);
		return (T) ((Method) member).invoke(this, argument);
	}
	
	/** set given field to given value
	 * @param member cannot be null
	 * @param value can be null
	 * @return value given
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException */
	default <T> T set(Field member, T value) throws IllegalAccessException, InvocationTargetException {
		log.debug("{}.{} = {}", this, member, value);
		member.set(this, value);
		return value;
	}
}
