package org.protorm.codecs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.protorm.BufferProvider;
import org.protorm.CodecUtils;
import org.protorm.FieldCodec;

public enum NetworkOrder implements FieldCodec<Annotation> {
	TWO_BYTES {
		@Override
		public Object readAndTransform(BufferProvider bufferProvider, DataInputStream input, Annotation readSoFar, Method methodOfT) throws IOException, IllegalStateException {
			return CodecUtils.toPrimitive(methodOfT.getReturnType(), input.readUnsignedShort());
		}
		
		@Override
		public void write(DataOutputStream output, Annotation value, Method methodOfT) throws IOException {
			output.writeShort(invoke(value, methodOfT).intValue());
		}
	}, FOUR_BYTES {
		@Override
		public Object readAndTransform(BufferProvider bufferProvider, DataInputStream input, Annotation readSoFar, Method methodOfT) throws IOException, IllegalStateException {
			return CodecUtils.toPrimitive(methodOfT.getReturnType(), input.readInt());
		}
		
		@Override
		public void write(DataOutputStream output, Annotation value, Method methodOfT) throws IOException {
			output.writeInt(invoke(value, methodOfT).intValue());
		}
	}, EIGHT_BYTES {
		@Override
		public Object readAndTransform(BufferProvider bufferProvider, DataInputStream input, Annotation readSoFar, Method methodOfT) throws IOException, IllegalStateException {
			return CodecUtils.toPrimitive(methodOfT.getReturnType(), input.readLong());
		}
		
		@Override
		public void write(DataOutputStream output, Annotation value, Method methodOfT) throws IOException {
			output.writeLong(invoke(value, methodOfT).longValue());
		}
	};
	
	@SuppressWarnings("unchecked")
	protected static <T extends Number> T invoke(Annotation value, Method methodOfT) {
		try {
			return (T) methodOfT.invoke(value);
		} catch (IllegalAccessException | InvocationTargetException unreachableThanksToAnnotation) {
			throw new RuntimeException(unreachableThanksToAnnotation);
		}
	}
}
