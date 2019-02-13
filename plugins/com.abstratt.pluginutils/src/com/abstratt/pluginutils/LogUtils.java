package com.abstratt.pluginutils;

import java.util.function.Supplier;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

public class LogUtils {
    public static void log(int severity, String pluginId, Supplier<String> message, Throwable exception) {
        log(() -> new Status(severity, pluginId, message.get(), exception));
    }

    public static void log(int severity, String pluginId, String message, Throwable exception) {
        log(() -> new Status(severity, pluginId, message, exception));
    }
    
    public static void log(IStatus status) {
        log(() -> status);
    }
    public static void log(Supplier<IStatus> statusSupplier) {
        IStatus status = statusSupplier.get();
        if (!Platform.isRunning()) {
            System.err.println(status.getMessage());
            if (status.getException() != null)
                status.getException().printStackTrace();
            if (status.isMultiStatus())
                for (IStatus child : status.getChildren())
                    log(child);
            return;
        }
        Bundle bundle = Platform.getBundle(status.getPlugin());
        if (bundle == null) {
            String thisPluginId = LogUtils.class.getPackage().getName();
            bundle = Platform.getBundle(thisPluginId);
            Platform.getLog(bundle).log(
                    new Status(IStatus.WARNING, thisPluginId, "Could not find a plugin " + status.getPlugin()
                            + " for logging as"));
        }
        Platform.getLog(bundle).log(status);
    }

    public static void logError(String pluginId, String message, Throwable e) {
        log(IStatus.ERROR, pluginId, message, e);
    }

    public static void logWarning(String pluginId, String message, Throwable e) {
        log(IStatus.WARNING, pluginId, message, e);
    }
    
    public static void logInfo(String pluginId, String message, Throwable e) {
        log(IStatus.INFO, pluginId, message, e);
    }

    public static void debug(String pluginId, String message) {
        debug(pluginId, () -> message);
    }
    
    public static void debug(String pluginId, Supplier<String> message) {
        if (Boolean.getBoolean(pluginId + ".debug"))
            log(IStatus.INFO, pluginId, message.get(), null);
    }

    public static void logError(Class pluginClass, String message, Throwable e) {
    	logError(pluginClass.getPackage().getName(), message, e);
    }

    public static void logWarning(Class pluginClass, String message, Throwable e) {
    	logWarning(pluginClass.getPackage().getName(), message, e);
    }
    
    public static void logInfo(Class pluginClass, String message, Throwable e) {
        logInfo(pluginClass.getPackage().getName(), message, e);
    }

    public static void debug(Class pluginClass, String message) {
        debug(pluginClass.getName(), () -> message);
    }
    
    public static void debug(Class pluginClass, Supplier<String> message) {
    	debug(pluginClass.getName(), message);
    }    

    public static void log(int severity, Class pluginClass, Supplier<String> message, Throwable exception) {
        log(severity, pluginClass.getPackage().getName(), message, exception);
    }

    public static void log(int severity, Class pluginClass, String message, Throwable exception) {
        log(severity, pluginClass.getPackage().getName(), message, exception);
    }

    
}
