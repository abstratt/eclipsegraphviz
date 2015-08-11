package com.abstratt.internal.content;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.beanutils.MethodUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.viewers.IContentProvider;

import com.abstratt.content.ContentSupport;
import com.abstratt.content.IContentProviderRegistry;
import com.abstratt.pluginutils.LogUtils;
import com.abstratt.pluginutils.RegistryReader;

public class ContentProviderRegistry implements IContentProviderRegistry {
	public class ContentTypeDescriptor implements IProviderDescription {
		private IConfigurationElement configElement;
		private Set<IContentType> associations = new HashSet<IContentType>();
		private List<Object> readers = new ArrayList<Object>();

		public ContentTypeDescriptor(IConfigurationElement configElement) {
			this.configElement = configElement;
			IConfigurationElement[] associationElements = configElement.getChildren("association");
			IContentTypeManager pcm = Platform.getContentTypeManager();
			for (IConfigurationElement associationEl : associationElements)
				associations.add(pcm.getContentType(associationEl.getAttribute("contentType")));
			IConfigurationElement[] readerElements = configElement.getChildren("reader");
			for (IConfigurationElement readerEl : readerElements)
				try {
					Object reader = readerEl.createExecutableExtension("class");
					readers.add(reader);
				} catch (CoreException e) {
					LogUtils.logError(ContentSupport.PLUGIN_ID, "Error processing content provider extension "
									+ configElement.getNamespaceIdentifier(), e);
				}
		}

		public boolean canRead(Class<?> sourceType) {
			return findReader(sourceType) != null;
		}

		public Object findReader(Class<?> sourceType) {
			for (Object reader : readers)
				if (getReaderMethod(reader, sourceType) != null)
					return reader;
			return null;
		}

		public Set<IContentType> getAssociations() {
			return associations;
		}

		public IContentProvider getProvider() {
			try {
				return (IContentProvider) configElement.createExecutableExtension("class");
			} catch (CoreException e) {
				LogUtils.logError(ContentSupport.PLUGIN_ID, "Could not instantiate content provider", e);
			}
			return null;
		}

		private Method getReaderMethod(Object reader, Class<?> sourceType) {
			Method method =
							MethodUtils.getMatchingAccessibleMethod(reader.getClass(), "read",
											new Class[] { sourceType });
			return method == null || method.getReturnType() == Void.class ? null : method;
		}

		public Object read(Object source) {
			Object reader = findReader(source.getClass());
			if (reader == null)
				throw new IllegalArgumentException("Cannot read " + source);
			Method readerMethod = getReaderMethod(reader, source.getClass());
			try {
				return readerMethod.invoke(reader, source);
			} catch (IllegalAccessException e) {
				Activator.logUnexpected(null, e);
			} catch (InvocationTargetException e) {
				Activator.logUnexpected(null, e);
			}
			return null;
		}
	}


	public class DebuggingDescriptor implements IProviderDescription {

		private IConfigurationElement configElement;
		private Object reader;;

		public DebuggingDescriptor(IConfigurationElement configElement) {
			this.configElement = configElement;
			IConfigurationElement[] readerElements = configElement.getChildren("reader");
			// There should only be one!
			for (IConfigurationElement readerEl : readerElements)
				try {
					reader = readerEl.createExecutableExtension("class");
				} catch (CoreException e) {
					LogUtils.logError(ContentSupport.PLUGIN_ID, "Error processing content provider extension "
									+ configElement.getNamespaceIdentifier(), e);
				}

		}

		@Override
		public boolean canRead(Class<?> sourceType) {
			return false;
		}

		@Override
		public Set<IContentType> getAssociations() {
			return null;
		}

		@Override
		public IContentProvider getProvider() {
			try {
				return (IContentProvider) configElement.createExecutableExtension("class");
			} catch (CoreException e) {
				LogUtils.logError(ContentSupport.PLUGIN_ID, "Could not instantiate content provider", e);
			}
			return null;
		}

		@Override
		public Object read(Object source) {
			Method readerMethod = getReaderMethod(reader, source.getClass());
			try {
				return readerMethod.invoke(reader, source);
			} catch (IllegalAccessException e) {
				Activator.logUnexpected(null, e);
			} catch (InvocationTargetException e) {
				Activator.logUnexpected(null, e);
			}
			return null;
		}

		private Method getReaderMethod(Object reader, Class<?> sourceType) {
			Method method =
					MethodUtils.getMatchingAccessibleMethod(reader.getClass(), "read",
											new Class[] { sourceType });
			return method == null || method.getReturnType() == Void.class ? null : method;
		}

	}

	private static final String CONTENT_PROVIDER_XP = ContentSupport.PLUGIN_ID + ".contentProvider"; //$NON-NLS-1$

	private static final String DEBUG_PROVIDER_XP = ContentSupport.PLUGIN_ID + ".debug"; //$NON-NLS-1$

	public List<ContentTypeDescriptor> contentTypeDescriptors = new ArrayList<ContentTypeDescriptor>();
	public DebuggingDescriptor debugDescriptor;

	public ContentProviderRegistry() {
		build();
	}

	private void addContentTypeProvider(IConfigurationElement element) {
		contentTypeDescriptors.add(new ContentTypeDescriptor(element));
	}

	private void addDebuggerProvider(IConfigurationElement element) {
		debugDescriptor = new DebuggingDescriptor(element);
	}

	private void build() {
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		new RegistryReader() {
			@Override
			protected String getNamespace() {
				return ContentSupport.PLUGIN_ID;
			}

			@Override
			protected boolean readElement(IConfigurationElement element) {
				addContentTypeProvider(element);
				return true;
			}
		}.readRegistry(registry, CONTENT_PROVIDER_XP);
		new RegistryReader() {
			@Override
			protected String getNamespace() {
				return ContentSupport.PLUGIN_ID;
			}

			@Override
			protected boolean readElement(IConfigurationElement element) {
				addDebuggerProvider(element);
				return true;
			}
		}.readRegistry(registry, DEBUG_PROVIDER_XP);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.abstratt.content.IContentProviderRegistry#findContentProvider(org.eclipse.core.runtime.content.IContentType,
	 *      java.lang.Class)
	 */
	public IProviderDescription findContentProvider(IContentType target,
					Class<? extends IContentProvider> minimumProtocol) {
		for (IProviderDescription descriptor : contentTypeDescriptors)
			for (IContentType contentType : descriptor.getAssociations())
				if (target.isKindOf(contentType)) {
					if (minimumProtocol != null && minimumProtocol.isInstance(descriptor.getProvider()))
						return descriptor;
				}
		return null;
	}

	@Override
	public IProviderDescription getDebugContentProvider() {
		return debugDescriptor;

	}

}
