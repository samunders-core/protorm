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
			if (line == null) {
				line = getClass().getAnnotation(Text.class);
			} else if (line.maxLength() <= 0) {
				throw new IllegalStateException("Max line length required above zero: " + methodOfT);
			}
			int minSize = Math.min(4096, line.maxLength()), off = 0;
			byte[] buffer = bufferProvider.buffer(minSize);
			buffer[0] = line.terminatedBy().length > 0 ? (byte) (1 + line.terminatedBy()[0]) : 0;
			try {
				for (; minSize <= line.maxLength() && !endsWith(buffer, off, line.terminatedBy()); minSize *= 2) {
					buffer = bufferProvider.buffer(minSize);
					for (; off < minSize && !endsWith(buffer, off, line.terminatedBy()); ++off) {
						input.readFully(buffer, off, 1);
					}
				}
			} catch (EOFException e) {
				if (off == 0) {
					throw e;
				}
			}
			if (endsWith(buffer, off, line.terminatedBy())) {
				off -= line.terminatedBy().length;
			}
			if (methodOfT.getReturnType().isArray() && byte.class.equals(methodOfT.getReturnType().getComponentType())) {
				return Arrays.copyOf(buffer, off);
			}
			return new String(buffer, 0, off, Charset.forName(line.charset()));
		}
		
		private boolean endsWith(byte[] buffer, int count, byte[] terminator) {
			if (terminator.length == 0 || count < terminator.length) {
				return false;
			}
			for (int b = count - 1, t = terminator.length - 1; b + t >= 0; --b, --t) {
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