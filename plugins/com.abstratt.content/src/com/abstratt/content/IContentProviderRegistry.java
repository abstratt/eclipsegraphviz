package com.abstratt.content;

import java.util.Set;

import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.viewers.IContentProvider;

public interface IContentProviderRegistry {
	public interface IProviderDescription {
		public boolean canRead(Class<?> sourceType);

		public Set<IContentType> getAssociations();

		public IContentProvider getProvider();

		/**
		 * Converts the given source object (using one of the contributed readers) 
		 * to the format expected by the content provider. 
		 * 
		 * @param source object to convert
		 * @return the converted contents that are consumable by the content provider
		 */
		public Object read(Object source);
	}

	/**
	 * Find the content provider that matches the given content type.
	 */
	public IProviderDescription findContentProvider(IContentType target,
					Class<? extends IContentProvider> minimumProtocol);

}