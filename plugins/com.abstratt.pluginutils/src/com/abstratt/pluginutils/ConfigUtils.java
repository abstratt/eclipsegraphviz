package com.abstratt.pluginutils;

public class ConfigUtils {
	public static String get(String name) {
		return get(name, null);
	}

	public static String get(String name, String defaultValue) {
		String asProperty = System.getProperty(name);
		if (asProperty != null)
			return asProperty;
		String asEnvVar = System.getenv(name);
		if (asEnvVar != null)
			return asEnvVar;
		return defaultValue;
	}
}
