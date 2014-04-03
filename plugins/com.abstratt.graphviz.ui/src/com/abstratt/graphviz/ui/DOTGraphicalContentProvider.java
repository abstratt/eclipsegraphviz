package com.abstratt.graphviz.ui;

import java.io.ByteArrayInputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import com.abstratt.graphviz.GraphViz;
import com.abstratt.imageviewer.AbstractGraphicalContentProvider;

/**
 * A graphical content provider that produces graphical output from a DOT
 * description.
 */
public class DOTGraphicalContentProvider extends AbstractGraphicalContentProvider {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.abstratt.imageviewer.IGraphicalContentProvider#loadImage(org.eclipse
	 * .swt.widgets.Display, org.eclipse.swt.graphics.Point, java.lang.Object)
	 */
	public Image loadImage(Display display, Point desiredSize, Object newInput) throws CoreException {
		if (desiredSize == null)
			desiredSize = new Point(0, 0);
		return GraphViz.load(new ByteArrayInputStream((byte[]) newInput), "png", desiredSize);
	}

	@Override
	public void saveImage(Display display, Point suggestedSize, Object input, IPath outputLocation, int fileFormat)
					throws CoreException {
		if (suggestedSize == null)
			suggestedSize = new Point(0, 0);
		String outputFormat = "jpg";
		switch (fileFormat) {
		case SWT.IMAGE_GIF:
			outputFormat = "gif";
			break;
		case SWT.IMAGE_PNG:
			outputFormat = "png";
			break;
		case SWT.IMAGE_BMP:
			outputFormat = "bmp";
			break;
		case SWT.IMAGE_TIFF:
			outputFormat = "tif";
			break;
		}
		GraphViz.generate(new ByteArrayInputStream((byte[]) input), outputFormat, suggestedSize, outputLocation);
	}
}
