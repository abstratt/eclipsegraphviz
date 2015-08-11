package com.abstratt.imageviewer;

import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.internal.debug.ui.JDIModelPresentation;

public class JDIObjectToByteArrayContentReader {

	public static String JDI_OBJECT_TO_DOT = "toDOT";

	public byte[] read(IJavaObject input) {

		String details = "";
		@SuppressWarnings("restriction")
		IJavaThread thread = JDIModelPresentation.getEvaluationThread((IJavaDebugTarget) input.getDebugTarget());
		IJavaValue toStringValue = null;
		try {
			toStringValue = input.sendMessage(JDI_OBJECT_TO_DOT, "()Ljava/lang/String;", null, thread, false);
		} catch (DebugException e) {
			//Activator.logUnexpected(null, e);
			//Silently return
			return null;
		}
		try {
			details = toStringValue.getValueString();
		} catch (DebugException e) {
			Activator.logUnexpected(null, e);
			return null;
		}
		return details.getBytes();
	}

}
