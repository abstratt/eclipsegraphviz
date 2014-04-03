package com.abstratt.pluginutils;

/**
 * A runnable that does not need to make changes.
 */
public interface ISharedContextRunnable<C, R> {
	public R runInContext(C context);
}
