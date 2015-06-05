/*******************************************************************************
 * Copyright (c) 2007 EclipseGraphviz contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     abstratt technologies
 *     Scott Bronson
 *******************************************************************************/
package com.abstratt.graphviz;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Enumeration;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.abstratt.pluginutils.LogUtils;

public class GraphVizActivator implements BundleActivator {


	/**
	 * DotLocation keeps track of how the user wants to select the dot
	 * executable. We include strings for each term so that the settings files
	 * remain human readable.
	 */
	public enum DotMethod {
		AUTO, BUNDLE, DETECT, MANUAL;

		/**
		 * Given a string, looks up the corresponding DotMethod. If no match,
		 * returns the default AUTO.
		 */
		static public DotMethod find(String term) {
			for (DotMethod p : DotMethod.values()) {
				if (p.name().equals(term)) {
					return p;
				}
			}
			return AUTO;
		}
	}


	public static final String DOT_SEARCH_METHOD = "dotSearchMethod";
	// The manual path is entered by the user. It should never be changed or
	// deleted except by the user.
	public static final String DOT_MANUAL_PATH = "dotManualPath";
	
	public static final String DOT_FILE_NAME = "dot";
	
	public static final String COMMAND_LINE = "commandLineExtension";

	public static String ID = GraphVizActivator.class.getPackage().getName();

	private static GraphVizActivator instance;

	public static GraphVizActivator getInstance() {
		return instance;
	}

	/**
	 * Returns whether the given file is executable. Depending on the platform
	 * we might not get this right.
	 * 
	 * TODO find a better home for this function
	 */
	public static boolean isExecutable(File file) {
		if (!file.isFile())
			return false;
		if (Platform.getOS().equals(Platform.OS_WIN32))
			// executable attribute is a *ix thing, on Windows all files are
			// executable
			return true;
		return Files.isExecutable(file.toPath());
	}

	public static void logUnexpected(String message, Exception e) {
		LogUtils.logError(ID, message, e);
	}

	// store paths as strings so they won't get screwed up by platform issues.
	/**
	 * Path to bundled dot or null if it can't be found. See
	 * extractGraphVizBinaries().
	 */
	private String bundledDotLocation;

	/**
	 * Path to autodetected dot or null if it can't be found. See
	 * autodetectDots().
	 */
	private String autodetectedDotLocation;
	
	private String commandLine;

	/**
	 * The path the bundled Graphviz install was extracted to (null if not
	 * found/looked up).
	 */

	public GraphVizActivator() {
		instance = this;
	}

	/**
	 * This routine browses through the user's PATH looking for dot executables.
	 * 
	 * @return the absolute path if a suitable dot is found, null if not. This
	 *         is normally called once at plugin startup but it can also be
	 *         called while the plugin is running (in case user has installed
	 *         dot without restarting Eclipse).
	 */
	public String autodetectDots() {
		autodetectedDotLocation = null;
		String paths = System.getenv("PATH");
		for (String path : paths.split(File.pathSeparator)) {
			File directory = new File(path);
			File[] matchingFiles = directory.listFiles(new ExecutableFinder(DOT_FILE_NAME));
			if (matchingFiles != null && matchingFiles.length > 0) {
				File found = matchingFiles[0];
				autodetectedDotLocation = found.getAbsolutePath();
				break;
			}
		}
		return autodetectedDotLocation;
	}
	
	private static class ExecutableFinder implements FileFilter {
		private String nameToMatch;
		
		public ExecutableFinder(String nameToMatch) {
			this.nameToMatch = nameToMatch;
		}

		public boolean accept(File candidate) {
		    if (!candidate.getName().equalsIgnoreCase(nameToMatch)
                            && !candidate.getName().startsWith(nameToMatch + '.'))
		        return false;
			boolean executable = isExecutable(candidate);
			return executable;
		}
	}

	/**
	 * Tries to find and (if required) extract the binaries for a bundled
	 * Graphviz install. If cannot find, the Graphviz integration will only work
	 * if the ... ?
	 * 
	 * @param context
	 * @throws IOException
	 */
	private String extractGraphVizBinaries(BundleContext context) throws IOException {
		Enumeration<?> found = context.getBundle().findEntries("/graphviz-min/", "eg_dot*", false);
		if (found == null || !found.hasMoreElements()) {
			return null;
		}

		URL dotURL = (URL) found.nextElement();
		URL basicURL = new URL(dotURL, ".");
		URL fileURL = FileLocator.toFileURL(basicURL);
		if (!fileURL.getProtocol().startsWith("file")) {
			IStatus bundledBinariesStatus =
							new Status(IStatus.ERROR, ID, "Unexpected protocol for location of GraphViz binaries: '"
											+ fileURL + "'");
			Platform.getLog(context.getBundle()).log(bundledBinariesStatus);
			return null;
		}
		bundledDotLocation = FileLocator.toFileURL(dotURL).getPath();
		return bundledDotLocation;
	}

	public String getBundledDotLocation() {
		return bundledDotLocation;
	}

	// preference getters and setters

	/**
	 * Gets the path to the dot executable. It takes user's preferences into
	 * account so it should always do the right thing.
	 */
	public IPath getDotLocation() {
		final String manualLocation = getManualDotPath();
		switch (getDotSearchMethod()) {
		case AUTO:
			if (bundledDotLocation != null)
				return new Path(bundledDotLocation);
			if (autodetectedDotLocation != null)
				return new Path(autodetectedDotLocation);
			return manualLocation != null ? new Path(manualLocation) : null;

		case BUNDLE:
			return bundledDotLocation != null ? new Path(bundledDotLocation) : null;

		case DETECT:
			return autodetectedDotLocation != null ? new Path(autodetectedDotLocation) : null;

		case MANUAL:
			return manualLocation != null ? new Path(manualLocation) : null;
		}

		// Someone must have edited the prefs file manually... just reset the
		// value.
		setDotSearchMethod(DotMethod.AUTO);
		return null;
	}

	public DotMethod getDotSearchMethod() {
		String value = getPreference(DOT_SEARCH_METHOD);
		return value != null ? DotMethod.find(value) : DotMethod.AUTO;
	}

	public File getGraphVizDirectory() {
		IPath dotLocation = getDotLocation();
		return dotLocation == null ? null : dotLocation.removeLastSegments(1).toFile();
	}

	public String getManualDotPath() {
		return getPreference(DOT_MANUAL_PATH);
	}
	
	public String getCommandLineExtension() {
		return getPreference(COMMAND_LINE);
	}
	
	public void setCommandLineExtension(String commandLineExtension) {
		setPreference(COMMAND_LINE, commandLineExtension);
	}

	/** Returns the preference with the given name */
	public String getPreference(String preference_name) {
		Preferences node =
						Platform.getPreferencesService().getRootNode().node(InstanceScope.SCOPE).node(
										GraphVizActivator.ID);
		return node.get(preference_name, null);
	}

	public boolean hasBundledInstall() {
		return bundledDotLocation != null;
	}

	public void setDotSearchMethod(DotMethod dotMethod) {
		setPreference(DOT_SEARCH_METHOD, dotMethod.name());
	}

	public void setManualDotPath(String newLocation) {
		setPreference(DOT_MANUAL_PATH, newLocation);
	}

	/** Sets the given preference to the given value */
	public void setPreference(String preferenceName, String value) {
		IEclipsePreferences root = Platform.getPreferencesService().getRootNode();
		Preferences node = root.node(InstanceScope.SCOPE).node(GraphVizActivator.ID);
		node.put(preferenceName, value);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			LogUtils.logError(ID, "Error updating preferences.", e);
		}
	}

	public void start(BundleContext context) throws Exception {
		// try to find a bundled dot.
		try {
			extractGraphVizBinaries(context);
		} catch (IOException e) {
			LogUtils.logError(ID, "Error looking for dot executable.", e);
		}

		// then try to find any installed copies of dot
		autodetectDots();
		if (autodetectedDotLocation == null && getDotSearchMethod() == DotMethod.DETECT) {
			setDotSearchMethod(DotMethod.AUTO);
			LogUtils.logWarning(ID, "Could not find a suitable dot executable.  Please specify one using Window -> Preferences -> Graphviz.", null);
		}
	}

	public void stop(BundleContext context) throws Exception {
		// nothing to do 
	}
}
