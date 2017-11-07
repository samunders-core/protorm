package org.protorm.codecs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.protorm.BufferProvider;
import org.protorm.CodecUtils;
import org.protorm.FieldCodec;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Text {
	byte[] terminatedBy() default '\n';
	String charset() default "UTF-8";
	int maxLength() default 1024*1024;
	
	@Text
	enum Line implements FieldCodec<Annotation> {
		INSTANCE;

		@Override
		public Object readAndTransform(BufferProvider bufferProvider, DataInputStream input, Annotation readSoFar, Method methodOfT) throws IOException, IllegalStateException {
			Text line = methodOfT.getAnnotation(Text.class);
			for (Class<?> clazz = methodOfT.getDeclaringClass(); line == null; clazz = getClass()) {
				line = clazz.getAnnotation(Text.class);
			}
			if (line.maxLength() <= 0) {
				throw new IllegalStateException("Max line length required above zero: " + methodOfT);
			}
			int off = -1;
			byte[] buffer = bufferProvider.buffer(Math.min(4096, line.maxLength()));
			try {
				off = read(input, ++off, line.terminatedBy(), buffer);
				while (buffer.length < line.maxLength() && !endsWith(buffer, off, line.terminatedBy())) {
					off = read(input, ++off, line.terminatedBy(), buffer = bufferProvider.buffer(buffer.length * 2));
				}
			} catch (EOFException e) {
				if (off == 0) {
					throw e;
				}
			}
			if (endsWith(buffer, off, line.terminatedBy())) {
				off -= line.terminatedBy().length;
			}
			++off;
			if (methodOfT.getReturnType().isArray() && byte.class.equals(methodOfT.getReturnType().getComponentType())) {
				return Arrays.copyOf(buffer, off);
			}
			String result = new String(buffer, 0, off, Charset.forName(line.charset()));
			return methodOfT.getReturnType().isInstance(result) ? result : transform(methodOfT.getReturnType(), line, result);
		}
		
		@SuppressWarnings("unchecked")
		private static <T extends Enum<T>> Object transform(Class<?> type, Text line, String value) {
			if (type.isPrimitive()) {
				try {
					return CodecUtils.toPrimitive(type, Long.decode(value));
				} catch (NumberFormatException floatingPointThen) {
					try {
						return CodecUtils.toPrimitive(type, Double.valueOf(value));
					} catch (NumberFormatException boolOrChar) {
						return type.isInstance(value.charAt(0)) ? value.charAt(0) : !Boolean.FALSE.equals(Boolean.valueOf(value));
					}
				}
			} else if (type.isEnum()) {
				return Enum.valueOf((Class<T>) type, value);
			} else if (Class.class.isAssignableFrom(type)) {
				try {
					return Class.forName(value, false, Thread.currentThread().getContextClassLoader());
				} catch (ClassNotFoundException e) {}
			} else if (type.isArray()) {
				if (byte.class.equals(type.getComponentType())) {
					return value.getBytes(Charset.forName(line.charset()));
				} else if (char.class.equals(type.getComponentType())) {
					return value.toCharArray();
				}
			}
			throw new RuntimeException("Don't know how to make " + type + " from " + value);
		}
		
		private static int read(DataInputStream input, int off, byte[] terminator, byte[] buffer) throws IOException {
			--off;
			do {
				input.readFully(buffer, ++off, 1);
			} while (off < buffer.length - 1 && !endsWith(buffer, off, terminator));
			return off;
		}
		
		private static boolean endsWith(byte[] buffer, int off, byte[] terminator) {
			if (terminator.length == 0 || off < terminator.length - 1) {
				return false;
			}
			for (int b = off, t = terminator.length - 1; b >= 0 && t >= 0; --b, --t) {
				if (buffer[b] != terminator[t]) {
					return false;
				}
			}
			return true;
		}

		@Override
		public void write(DataOutputStream output, Annotation value, Method methodOfT) throws IOException {
			// TODO Auto-generated method stub
			
		}
	}
}