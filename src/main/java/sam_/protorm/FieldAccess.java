package sam_.protorm;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface FieldAccess {
	final Logger log = LoggerFactory.getLogger(FieldAccess.class);
	
	default <T> T apply(AccessibleObject member, T value) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return member instanceof Method ? call((Method) member, value) : set((java.lang.reflect.Field) member, value);
	}
	
	@SuppressWarnings("unchecked")
	default <T> T get(AccessibleObject member, Class<T> type) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (member instanceof java.lang.reflect.Field) {
			return (T) ((java.lang.reflect.Field) member).get(this);
		}
		return type != null ? call((Method) member, type.cast(null)) : null;
	}
	
	@SuppressWarnings("unchecked")
	default <T> T call(Method member, Object argument) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		log.debug("{}.{}({})", this, member, argument);
		return (T) ((Method) member).invoke(this, argument);
	}
	
	default <T> T set(java.lang.reflect.Field member, T value) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		log.debug("{}.{} = {}", this, member, value);
		member.set(this, value);
		return value;
	}
}
