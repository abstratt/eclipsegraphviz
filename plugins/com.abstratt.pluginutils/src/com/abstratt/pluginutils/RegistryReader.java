/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.abstratt.pluginutils;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;

/**
 * Template implementation of a registry reader that creates objects
 * representing registry contents. Typically, an extension contains one element,
 * but this reader handles multiple elements per extension.
 * 
 * To start reading the extensions from the registry for an extension point,
 * call the method <code>readRegistry</code>.
 * 
 * To read children of an IConfigurationElement, call the method
 * <code>readElementChildren</code> from your implementation of the method
 * <code>readElement</code>, as it will not be done by default.
 */
public abstract class RegistryReader {

	public static final String ATT_CLASS = "class"; //$NON-NLS-1$

	/**
	 * Utility for extracting the value of a class attribute or a nested class
	 * element that follows the pattern set forth by
	 * {@link org.eclipse.core.runtime.IExecutableExtension}.
	 * 
	 * @param configElement
	 *            the element
	 * @param classAttributeName
	 *            the name of the class attribute to check
	 * @return the value of the attribute or nested class element
	 * @since 3.1
	 */
	public static String getClassValue(IConfigurationElement configElement, String classAttributeName) {
		String className = configElement.getAttribute(classAttributeName);
		if (className != null) {
			return className;
		}
		IConfigurationElement[] candidateChildren = configElement.getChildren(classAttributeName);
		if (candidateChildren.length == 0) {
			return null;
		}

		return candidateChildren[0].getAttribute(ATT_CLASS);
	}

	/**
	 * Apply a reproducable order to the list of extensions provided, such that
	 * the order will not change as extensions are added or removed.
	 * 
	 * @param extensions
	 *            the extensions to order
	 * @return ordered extensions
	 */
	public static IExtension[] orderExtensions(IExtension[] extensions) {
		// By default, the order is based on plugin id sorted
		// in ascending order. The order for a plugin providing
		// more than one extension for an extension point is
		// dependent in the order listed in the XML file.
		IExtension[] sortedExtension = new IExtension[extensions.length];
		System.arraycopy(extensions, 0, sortedExtension, 0, extensions.length);
		Comparator<IExtension> comparer = new Comparator<IExtension>() {
			public int compare(IExtension e1, IExtension e2) {
				return e1.getNamespaceIdentifier().compareToIgnoreCase(e2.getNamespaceIdentifier());
			}
		};
		Arrays.sort(sortedExtension, comparer);
		return sortedExtension;
	}

	protected abstract String getNamespace();

	/**
	 * Logs the error in the workbench log using the provided text and the
	 * information in the configuration element.
	 */
	protected void logError(IConfigurationElement element, String text) {
		IExtension extension = element.getDeclaringExtension();
		StringBuffer buf = new StringBuffer();
		buf
						.append("Plugin " + extension.getContributor().getName() + ", extension " + extension.getExtensionPointUniqueIdentifier());//$NON-NLS-2$//$NON-NLS-1$
		buf.append("\n" + text);//$NON-NLS-1$
		LogUtils.logError(getNamespace(), buf.toString(), null);
	}

	/**
	 * Logs a very common registry error when a required attribute is missing.
	 */
	protected void logMissingAttribute(IConfigurationElement element, String attributeName) {
		logError(element, "Required attribute '" + attributeName + "' not defined");//$NON-NLS-2$//$NON-NLS-1$
	}

	/**
	 * Logs a very common registry error when a required child is missing.
	 */
	protected void logMissingElement(IConfigurationElement element, String elementName) {
		logError(element, "Required sub element '" + elementName + "' not defined");//$NON-NLS-2$//$NON-NLS-1$
	}

	/**
	 * Logs a registry error when the configuration element is unknown.
	 */
	protected void logUnknownElement(IConfigurationElement element) {
		logError(element, "Unknown extension tag found: " + element.getName());//$NON-NLS-1$
	}

	/**
	 * Implement this method to read element's attributes. If children should
	 * also be read, then implementor is responsible for calling
	 * <code>readElementChildren</code>. Implementor is also responsible for
	 * logging missing attributes.
	 * 
	 * @return true if element was recognized, false if not.
	 */
	protected abstract boolean readElement(IConfigurationElement element);

	/**
	 * Read the element's children. This is called by the subclass' readElement
	 * method when it wants to read the children of the element.
	 */
	protected void readElementChildren(IConfigurationElement element) {
		readElements(element.getChildren());
	}

	/**
	 * Read each element one at a time by calling the subclass implementation of
	 * <code>readElement</code>.
	 * 
	 * Logs an error if the element was not recognized.
	 */
	protected void readElements(IConfigurationElement[] elements) {
		for (int i = 0; i < elements.length; i++) {
			if (!readElement(elements[i])) {
				logUnknownElement(elements[i]);
			}
		}
	}

	/**
	 * Read one extension by looping through its configuration elements.
	 */
	protected void readExtension(IExtension extension) {
		readElements(extension.getConfigurationElements());
	}

	/**
	 * Start the registry reading process using the supplied plugin ID and
	 * extension point.
	 * 
	 * @param registry
	 *            the registry to read from
	 * @param extensionPoint
	 *            the fully qualified extension point id
	 */
	public void readRegistry(IExtensionRegistry registry, String extensionPoint) {
		IExtensionPoint point = registry.getExtensionPoint(extensionPoint);
		if (point == null) {
			return;
		}
		IExtension[] extensions = point.getExtensions();
		extensions = orderExtensions(extensions);
		for (int i = 0; i < extensions.length; i++) {
			readExtension(extensions[i]);
		}
	}
}
