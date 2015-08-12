package com.abstratt.content;

import com.abstratt.internal.content.ContentProviderRegistry;

public class ContentSupport {
	public static final String PLUGIN_ID = ContentSupport.class.getPackage().getName();

	private static IContentProviderRegistry registry = new ContentProviderRegistry();

	public static IContentProviderRegistry getContentProviderRegistry() {
		return registry;
	}
}
