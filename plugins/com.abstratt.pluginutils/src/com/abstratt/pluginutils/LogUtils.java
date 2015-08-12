package com.abstratt.pluginutils;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

public class LogUtils {
    public static void log(int severity, String pluginId, String message, Throwable exception) {
        log(new Status(severity, pluginId, message, exception));
    }

    public static void log(IStatus status) {
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
        if (Boolean.getBoolean(pluginId + ".debug"))
            log(IStatus.INFO, pluginId, message, null);
    }

}
