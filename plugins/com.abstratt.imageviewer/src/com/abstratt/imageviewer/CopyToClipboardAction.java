package com.abstratt.imageviewer;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class CopyToClipboardAction implements IViewActionDelegate {

	private GraphicalView view;

	public void init(IViewPart view) {
		this.view = (GraphicalView) view;
	}

	public void run(IAction action) {
		Image image = view.getViewer().getImage();
		ImageData imageData = image.getImageData();
		ImageTransfer imageTransfer = ImageTransfer.getInstance();
		Clipboard clipboard = new Clipboard(Display.getCurrent());
		clipboard.setContents(new Object[] { imageData }, new Transfer[] { imageTransfer });
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// don't care
	}

}
