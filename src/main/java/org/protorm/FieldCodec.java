package org.protorm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface FieldCodec<T extends Annotation> {
	/** read value corresponding to {@link Method} applying byte order + specific value/sequence + count constraints
	 * @param bufferProvider
	 * @param input
	 * @param readSoFar
	 * @param methodOfT null if skipping requested
	 * @return never null
	 * @throws IOException
	 * @throws IllegalStateException when result is not assignable to {@link Method#getReturnType()} or {@link BufferProvider#buffer(int)} is returned */
	Object readAndTransform(BufferProvider bufferProvider, DataInputStream input, T readSoFar, Method methodOfT) throws IOException, IllegalStateException;
	
	void write(DataOutputStream output, T value, Method methodOfT) throws IOException;
}
