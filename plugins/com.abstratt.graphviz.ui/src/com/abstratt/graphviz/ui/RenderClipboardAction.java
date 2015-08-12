package com.abstratt.graphviz.ui;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import com.abstratt.imageviewer.GraphicalView;

public class RenderClipboardAction implements IViewActionDelegate {

	private GraphicalView view;

	public void init(IViewPart view) {
		this.view = (GraphicalView) view;
	}

	public void run(IAction action) {
		Clipboard clipboard = new Clipboard(Display.getCurrent());
		String contents = (String) clipboard.getContents(TextTransfer
		        .getInstance());
		if (contents != null) {
			view.setAutoSync(false);
			view.setContents(contents.getBytes(),
			        new DOTGraphicalContentProvider());
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// don't care
	}

}
