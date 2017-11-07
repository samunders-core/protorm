package org.protorm;

public interface BufferProvider {
	/** @param minSize
	 * @return never null, with contents preserved, size <strong> at least</strong> as given */
	byte[] buffer(int minSize);
}
