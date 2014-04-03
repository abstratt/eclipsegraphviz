package com.abstratt.imageviewer;

import java.io.ByteArrayInputStream;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

public class DefaultGraphicalContentProvider extends AbstractGraphicalContentProvider {

	/*
	 * (non-Javadoc)
	 * @see com.abstratt.imageviewer.IGraphicalContentProvider#loadImage(org.eclipse.swt.widgets.Display, org.eclipse.swt.graphics.Point, java.lang.Object)
	 */
	public Image loadImage(Display display, Point suggestedSize, Object newInput) {
		ImageData imageData = new ImageData(new ByteArrayInputStream((byte[]) newInput));
		return new Image(display, imageData);
	}

}
