package com.abstratt.content;

import org.eclipse.jface.viewers.IContentProvider;

import com.abstratt.content.IContentProviderRegistry.IProviderDescription;

public class PlaceholderProviderDescription implements IProviderDescription {

	private Object input;
	private IContentProvider contentProvider;

	public PlaceholderProviderDescription(Object input,
			IContentProvider contentProvider) {
		this.input = input;
		this.contentProvider = contentProvider;
	}

	@Override
	public IContentProvider getProvider() {
		return contentProvider;
	}

	@Override
	public Object read(Object source) {
		return input;
	}

}
