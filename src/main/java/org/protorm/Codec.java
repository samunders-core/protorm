package org.protorm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

// T extends annotation guarantees empty args to invocation handlers, abstract makes getClass().getGenericSuperclass().getActualTypeArguments() inference work
public abstract class Codec<RO_View extends Annotation, RW_Model extends Annotation/* RO_View */ & Encoder<RO_View, RW_Model>> {
	private final Map<Object, FieldCodec<? super RO_View>> fieldCodecs = new LinkedHashMap<>();
	
	/** use {@link #rawBytes(int)}, {@link #withCodec(Enum)} or {@link #withCodec(Codec)} to record key order in anonymous block */
	protected Codec() {}

	@SuppressWarnings("unchecked")
	protected Class<RO_View> RO_View() {	// returns correct result thanks to Codec<View, Model> being abstract
		return (Class<RO_View>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}
	
	@SuppressWarnings("unchecked")
	protected Class<RW_Model> RW_Model() {	// returns correct result thanks to Codec<View, Model> being abstract
		return (Class<RW_Model>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
	}
	
	protected <C extends FieldCodec<? super RO_View>> void skip(C codec) {
		if (codec == null) {
			throw new NullPointerException("Codec required");
		}
		fieldCodecs.put(fieldCodecs.size(), codec);
	}
	
	protected void skipBytes(final int bytes) {
		skip(Validate.NO, new byte[bytes]);
	}
	
	/** @param validate null treated as {@link Validate#NO}
	 * @param bytes zero-length forbidden */
	protected void skip(final Validate validate, final byte... bytes) {
		if (bytes.length == 0) {
			throw new IllegalArgumentException("Non-zero bytes required");
		}
		fieldCodecs.put(fieldCodecs.size(), new FieldCodec<RO_View>() {
			@Override
			public Object readAndTransform(BufferProvider bufferProvider, DataInputStream input, RO_View readSoFar, Method methodOfT) throws IOException, IllegalStateException {
				if (validate == Validate.YES) {
					input.readFully(bufferProvider.buffer(bytes.length), 0, bytes.length);
					for (int i = 0; i < bytes.length; i++) {
						if (bufferProvider.buffer(bytes.length)[i] != bytes[i]) {
							throw new IllegalStateException("At index " + i + ": expected " + bytes[i] + ", got " + bufferProvider.buffer(bytes.length)[i]);
						}
					}
				} else {
					for (int skipped = input.skipBytes(bytes.length), remaining = bytes.length - skipped; remaining > 0 && skipped > 0; remaining -= skipped) {
						skipped = input.skipBytes(remaining);
					}
				}
				return null;
			}

			@Override
			public void write(DataOutputStream output, RO_View value, Method methodOfT) throws IOException {
				output.write(bytes);
			}
		});
	}

	/** @param bytes negative means &quot;take up to&quot;
	 * @return never null
	 * @throws IllegalArgumentException for 0 and -1 */
	protected RO_View rawBytes(final int bytes) {
		if (bytes == 0 || bytes == -1) {
			throw new IllegalArgumentException("Any value but 0 and -1 required");
		}
		final FieldCodec<RO_View> result = new FieldCodec<RO_View>() {
			@Override
			public Object readAndTransform(BufferProvider bufferProvider, DataInputStream input, RO_View readSoFar, Method methodOfT) throws IOException, IllegalStateException {
				if (bytes > 0) {
					input.readFully(bufferProvider.buffer(bytes), 0, bytes);
					return Arrays.copyOf(bufferProvider.buffer(bytes), bytes);
				}
				int count = 0;
				try {
					for (; count < Math.abs(bytes); input.readFully(bufferProvider.buffer(count), count++, 1));
				} catch (EOFException e) {
					if (count == 1) {
						throw e;
					}
				}
				return Arrays.copyOf(bufferProvider.buffer(count), count);
			}

			@Override
			public void write(DataOutputStream output, RO_View value, Method methodOfT) throws IOException {
				try {
					output.write((byte[]) methodOfT.invoke(value));
				} catch (IllegalAccessException | InvocationTargetException unreachableThanksToAnnotation) {
					throw new RuntimeException(unreachableThanksToAnnotation);
				}
			}
		};
		return CodecUtils.proxy(new ValidatingRecorder() {
			@Override
			protected FieldCodec<? super RO_View> codec(Method method) {
				if (!byte[].class.equals(method.getReturnType())) {
					throw new IllegalStateException("Use byte[] return type for " + method);
				}
				return result;
			}
		}, RO_View());
	}
	
	protected <C extends FieldCodec<? super RO_View>> RO_View withCodec(final C codec) {
		if (codec == null) {
			throw new NullPointerException("Codec required");
		}
		return CodecUtils.proxy(new ValidatingRecorder() {
			@Override
			protected FieldCodec<? super RO_View> codec(Method method) {
				return codec;
			}
		}, RO_View());
	}
	
	protected <T extends Annotation, U extends Annotation & Encoder<T, U>> RO_View withCodec(final Codec<T, U> codec) {
		if (codec == null) {
			throw new NullPointerException("Codec required");
		}
		final FieldCodec<RO_View> result = new FieldCodec<RO_View>() {
			@SuppressWarnings("unchecked")
			@Override
			public Object readAndTransform(BufferProvider bufferProvider, DataInputStream input, RO_View readSoFar, Method methodOfT) throws IOException, IllegalStateException {
				Collection<U> result = null;
				for (U inner : codec.decode(input)) {
					if (!methodOfT.getReturnType().isArray()) {
						return inner;
					} else if (result == null) {
						result = new ArrayList<>();
					}
					result.add(codec.newEncoder().initFrom((T) inner));
				}
				if (!methodOfT.getReturnType().isArray() || result == null) {
					throw new EOFException();
				}
				return result.toArray();
			}

			@SuppressWarnings("unchecked")
			@Override
			public void write(DataOutputStream output, RO_View value, Method methodOfT) throws IOException {
				try {
					if (methodOfT.getReturnType().isArray()) {
						for (U inner : (U[]) methodOfT.invoke(value)) {
							inner.writeTo(output);
						}
					} else {
						((U) methodOfT.invoke(value)).writeTo(output);
					}
				} catch (IllegalAccessException | InvocationTargetException unreachable) {
					throw new RuntimeException(unreachable);
				}
			}
		};
		return CodecUtils.proxy(new ValidatingRecorder() {
			@Override
			protected FieldCodec<? super RO_View> codec(final Method method) {
				for (Class<?> type = method.getReturnType().isArray() ? method.getReturnType().getComponentType() : method.getReturnType(); !type.isAnnotation(); ) {
					throw new IllegalStateException("Use Annotation or its array as return type for " + method);
				}
				return result;
			}
		}, RO_View());
	}
	
	/** @return new MT unsafe instance, never null; iteration order set up in constructor's anonymous block via protected methods */
	public Iterable<Method> keys() {
		return CodecUtils.iterable(new ResetableIterator<Method>() {
			private Iterator<Object> keys = fieldCodecs.keySet().iterator();
			
			@Override
			public Method call() throws Exception {
				while (keys.hasNext()) {
					for (Object result = keys.next(); result instanceof Method; ) {
						return (Method) result;
					}
				}
				throw new Exception("no more elements");
			}
			
			@Override
			public void reset() {
				keys = fieldCodecs.keySet().iterator();
			}
		});
	}
	
	/** @return MT-unsafe defaults-providing instance, never null */
	@SuppressWarnings("resource")
	public RW_Model newEncoder() {
		return new EncoderImpl().proxy;
	}
		
	/** @param outer cannot be null
	 * @return never null
	 * @throws IOException */
	@SuppressWarnings("unchecked")
	public <T extends Annotation, U extends Annotation & Encoder<T, U>> RW_Model payloadOf(T outer) throws IOException {	// method allows passing domain objects around without accompanying InputStream
		Codec<T, U>.DecodedValues decoded = (Codec<T, U>.DecodedValues) Proxy.getInvocationHandler(outer);
		for (RW_Model result = RW_Model().cast(decoded.values.get(null)); result != null; ) {
			return result;
		}
		for (RW_Model inner : decode(decoded.dataInput)) {
			decoded.values.put(null, inner);
			return inner;
		}
		throw new EOFException();
	}
	
	/** @param is cannot be null
	 * @return never null, empty when empty stream given; {@link Iterator#remove()} does nothing
	 * @throws IOException */
	public Iterable<RW_Model> decode(InputStream is) throws IOException {
		final DataInputStream dataInput = is instanceof DataInputStream ? (DataInputStream) is : new DataInputStream(is);
		assert RO_View().isAssignableFrom(RW_Model());
		return CodecUtils.proxy(new DecodedValues(dataInput), RW_Model(), Iterator.class, Iterable.class);
	}

	private class DecodedValues extends EncoderImpl implements Iterable<RW_Model>, Iterator<RW_Model> {
		private byte[] buffer = new byte[1024];
		private final DataInputStream dataInput;
		
		private DecodedValues(DataInputStream dataInput) {
			this.dataInput = dataInput;
		}
		
		@Override
		public byte[] buffer(int minSize) {
			if (buffer.length < minSize) {
				buffer = Arrays.copyOf(buffer, 2 * minSize);
			}
			return buffer;
		}
		
		@Override
		public Iterator<RW_Model> iterator() {
			values.put(null, null);	// just enough to pass first check in hasNext
			return this;
		}
		
		@Override
		public boolean hasNext() {
			if (!values.isEmpty()) {
				values.clear();
				try {
					for (Map.Entry<Object, FieldCodec<? super RO_View>> entry : fieldCodecs.entrySet()) {
						Method m = entry.getKey() instanceof Method ? (Method) entry.getKey() : null;
						Object value = entry.getValue().readAndTransform(this, dataInput, RO_View().cast(proxy), m);
						if (m != null) {
							values.put(m, value);
						}	// else skipped exactly as recorded
					}
				} catch (IOException e) {}
			}
			return !values.isEmpty();
		}
		
		@Override
		public RW_Model next() {
			return proxy;
		}
		
		@Override
		public void remove() {}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] empty) throws Throwable {
			if (RO_View().equals(method.getDeclaringClass())) { 
				for (Object result = values.get(method); result != null; ) {
					return result;
				}
			}
			return super.invoke(proxy, method, empty);	// defaults
		}
	}
	
	private class EncoderImpl extends AbstractEncoder<RO_View, RW_Model> {
		protected EncoderImpl() {
			super(RW_Model());
		}

		@Override
		public byte[] buffer(int minSize) {
			return new byte[minSize];
		}
		
		@Override
		public RW_Model writeTo(DataOutputStream outputStream) throws IOException {
			for (Map.Entry<?, FieldCodec<? super RO_View>> entry : fieldCodecs.entrySet()) {
				entry.getValue().write(outputStream, RO_View().cast(proxy), entry.getKey() instanceof Method ? (Method) entry.getKey() : null);
			}
			return proxy;
		}
		
		@Override
		protected Object defaultValue(Method m) throws Throwable {
			for (Object result = m.getDefaultValue(); result != null; ) {
				return result;
			}
			for (Class<?> type = m.getReturnType(); type.isArray(); ) {
				return fieldCodecs.get(m).readAndTransform(this, this, RO_View().cast(proxy), m);
			}
			return super.defaultValue(m);
		}
	}
	
	private abstract class ValidatingRecorder implements InvocationHandler {
		/** @return never null */
		protected abstract FieldCodec<? super RO_View> codec(Method method);
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] empty) throws Throwable {
			if (Annotation.class.equals(method.getDeclaringClass())) {	// annotationType
				throw new UnsupportedOperationException(method.toString());
			}
			fieldCodecs.put(method, codec(method));
			return CodecUtils.toPrimitive(method.getReturnType(), 0);
		}
	}
}
