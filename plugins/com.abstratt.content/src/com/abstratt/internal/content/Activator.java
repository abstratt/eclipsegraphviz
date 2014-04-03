package com.abstratt.internal.content;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.abstratt.content.ContentSupport;
import com.abstratt.pluginutils.LogUtils;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator implements BundleActivator {

	public static void logUnexpected(String message, Exception e) {
		LogUtils.logError(ContentSupport.PLUGIN_ID, message, e);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		// just to initialize the content provider registry in a thread-safe way
		ContentSupport.getContentProviderRegistry();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
	}
}
