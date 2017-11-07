package org.protorm;

import java.util.concurrent.Callable;

public interface ResetableIterator<V> extends Callable<V> {
	void reset();
}
