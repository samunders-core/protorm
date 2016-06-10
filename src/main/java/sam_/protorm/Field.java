package sam_.protorm;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** for marking either:<br><ul>
 * <li>primitive/byte array properties</li>
 * <li>void/non-void methods accepting single argument of either: primitive or primitive wrapper or byte array or {@link InputStream} or {@link OutputStream}</li></ul>
 * non-void result is ignored unless it's {@link Layer} or its type matches type of primitive wrapper argument (null argument then signals encoding) */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Field {
	/** @return relative order of field in protocol frame, unique per class scope required, all parent-class members take precedence */
	int value();
	/** @return 0 or below to auto-detect from annotated element (auto-detection not supported for null fields and method arguments) */
	int byteCount() default 0;
	/** @return false for standard JAVA=network order, true for Intel ordering; meaningful for primitive fields and their wrappers only */
	boolean littleEndian() default false;
}
