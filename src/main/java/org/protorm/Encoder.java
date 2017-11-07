package org.protorm;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;

// Annotation: Return types are restricted to boolean, byte, char, short, int, long, float, double, String, Class, enums, annotations, and arrays of the preceding types
// expected type-safe usage: encoder.initFrom(prototype).set(encoder.key(), value).writeTo(outputStream).set(encoder.key(), anotherValue).writeTo(outputStream);
public interface Encoder<RO_View extends Annotation, RW_Model extends Annotation/* RO_View */ & Encoder<RO_View, RW_Model>> /*extends Externalizable*/ {
	/** @param init null restores annotation's defaults
	 * @return this, never null */
	RW_Model initFrom(RO_View init);	// TODO: name needs to stress this doesn't copy payload!!!
	
	RW_Model set(boolean key, Boolean value);	// primitives would cause unnecessary unboxing
	RW_Model set(byte key, Byte value);
	RW_Model set(char key, Character value);
	RW_Model set(short key, Short value);
	RW_Model set(int key, Integer value);
	RW_Model set(long key, Long value);
	RW_Model set(float key, Float value);
	RW_Model set(double key, Double value);
	RW_Model set(String key, String value);
	<C> RW_Model set(Class<C> key, Class<? extends C> value);
	<E extends Enum<E>> RW_Model set(E key, E value);
	<A extends Annotation> RW_Model set(A key, A value);
//	<U> T set(U key, U value);

	RW_Model set(boolean[] key, boolean... value);
	RW_Model set(byte[] key, byte... value);
	RW_Model set(char[] key, char... value);
	RW_Model set(short[] key, short... value);
	RW_Model set(int[] key, int... value);
	RW_Model set(long[] key, long... value);
	RW_Model set(float[] key, float... value);
	RW_Model set(double[] key, double... value);
	RW_Model set(String[] key, String... value);
	@SuppressWarnings("unchecked")
	<C> RW_Model set(Class<? extends C>[] key, Class<? extends C>... value);
	@SuppressWarnings("unchecked")
	<E extends Enum<E>> RW_Model set(E[] key, E... value);
	@SuppressWarnings("unchecked")
	<A extends Annotation> RW_Model set(A[] key, A... value);
//	@SuppressWarnings("unchecked")
//	<U> T set(U[] key, U... value);
	
	RW_Model set(int index, boolean[] key, boolean value);
	RW_Model set(int index, byte[] key, byte value);
	RW_Model set(int index, char[] key, char value);
	RW_Model set(int index, short[] key, short value);
	RW_Model set(int index, int[] key, int value);	// argument order intentional to prevent alias of array-keyed set() methods
	RW_Model set(int index, long[] key, long value);
	RW_Model set(int index, float[] key, float value);
	RW_Model set(int index, double[] key, double value);
	RW_Model set(int index, String[] key, String value);
	<C> RW_Model set(int index, Class<? super C>[] key, Class<C> value);
	<E extends Enum<E>> RW_Model set(int index, E[] key, E value);
	<A extends Annotation> RW_Model set(int index, A[] key, A value);
//	<U> T set(int index, U[] key, U value);
	
	// TODO: name needs to stress this doesn't write payload!!!
	RW_Model writeTo(DataOutputStream outputStream) throws IOException;	// TODO: EncodeException if some value missing and no default in annotation?
}
