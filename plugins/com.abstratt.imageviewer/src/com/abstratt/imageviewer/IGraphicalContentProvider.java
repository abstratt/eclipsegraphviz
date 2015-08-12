package com.abstratt.imageviewer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

/**
 * A content provider that can render a graphical output.
 */
public interface IGraphicalContentProvider extends IContentProvider {
    /**
     * Returns this provider's current image.
     * 
     * @param suggestedDimension
     * 
     * @throws IllegalStateException
     *             if no input has been set
     * @return the rendered image
     */
    public Image getImage();

    public void setSuggestedSize(Point suggested);

    /**
     * Returns an image produced from the given input. This method might be
     * invoked from a non-UI thread.
     * 
     * @param display
     * @param suggestedSize
     * @param newInput
     * @return the image loaded
     * @throws CoreException
     *             if an error occurs while producing the image
     */
    public Image loadImage(Display display, Point suggestedSize, Object newInput) throws CoreException;

    /**
     * Generates an image at the given location.
     * 
     * @param display
     * @param suggestedSize
     * @param input
     * @param location
     * @throws CoreException
     */
    public void saveImage(Display display, Point suggestedSize, Object input, IPath location, int fileFormat)
            throws CoreException;
}
