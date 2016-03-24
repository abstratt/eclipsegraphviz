package com.abstratt.imageviewer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import com.abstratt.pluginutils.LogUtils;

public abstract class AbstractGraphicalContentProvider implements IGraphicalContentProvider {

    private Image image;

    private Point suggestedSize;

    private ContentLoader loaderJob = new ContentLoader();

    public final static ISchedulingRule CONTENT_LOADING_RULE = new ISchedulingRule() {

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.eclipse.core.runtime.jobs.ISchedulingRule#contains(org.eclipse
         * .core.runtime.jobs.ISchedulingRule)
         */
        public boolean contains(ISchedulingRule rule) {
            return rule == this;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.eclipse.core.runtime.jobs.ISchedulingRule#isConflicting(org.eclipse
         * .core.runtime.jobs.ISchedulingRule)
         */
        public boolean isConflicting(ISchedulingRule rule) {
            return rule == this;
        }

    };

    class ContentLoader extends Job {

        private static final long IMAGE_LOAD_DELAY = 100;
        static final String JOB_FAMILY = "ContentLoader";
        private Object input;
        private Viewer viewer;

        public ContentLoader() {
            super("Image loading job");
            setSystem(true);
            setPriority(Job.INTERACTIVE);
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            monitor.beginTask("loading image", 100);
            getJobManager().beginRule(CONTENT_LOADING_RULE, monitor);
            try {
                if (monitor.isCanceled())
                    return Status.CANCEL_STATUS;
                disposeImage();
                monitor.worked(50);
                try {
                    setImage(AbstractGraphicalContentProvider.this.loadImage(Display.getDefault(), getSuggestedSize(),
                            input));
                } catch (CoreException e) {
                    if (!e.getStatus().isOK())
                        LogUtils.log(e.getStatus());
                    setImage(createErrorImage(Display.getDefault(), getSuggestedSize(), e.getStatus()));
                }
                if (monitor.isCanceled())
                    return Status.CANCEL_STATUS;
                monitor.worked(20);
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        if (monitor.isCanceled())
                            return;
                        monitor.worked(20);
                        if (viewer != null)
                            viewer.refresh();
                    }
                });
            } finally {
                getJobManager().endRule(CONTENT_LOADING_RULE);
                monitor.done();
            }
            return Status.OK_STATUS;
        }

        public boolean belongsTo(Object family) {
            return JOB_FAMILY.equals(family);
        }

        private void asyncLoadImage(Object input, Viewer viewer) {
            // first cancel any competing image loading jobs
            getJobManager().cancel(JOB_FAMILY);
            this.input = input;
            if (viewer.getControl().isDisposed() || !viewer.getControl().isVisible())
                return;
            this.viewer = viewer;
            schedule(IMAGE_LOAD_DELAY);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {
        this.loaderJob.cancel();
        disposeImage();
    }

    public void setImage(Image newImage) {
        disposeImage();
        this.image = newImage;
    }

    public Image createErrorImage(Display display, Point size, IStatus status) {
        Image errorImg = new Image(display, size.x, size.y);
        GC gc = new GC(errorImg);
        StringBuffer output = new StringBuffer("Errors generating an image.");
        if (!status.isOK())
            output.append("More details in the log file.");
        output.append("\n\n");
        gc.drawText(renderMessage(status, output), 10, 10);
        gc.dispose();
        return errorImg;
    }

    private String renderMessage(IStatus status, StringBuffer output) {
        output.append(status.getMessage());
        if (status.getException() != null)
            output.append("\n" + status.getException());
        for (IStatus child : status.getChildren()) {
            output.append("\n");
            renderMessage(child, output);
        }
        return output.toString();
    }

    private void disposeImage() {
        if (image == null)
            return;
        image.dispose();
        image = null;
    }

    public final Image getImage() {
        return image;
    }

    protected Point getSuggestedSize() {
        return suggestedSize;
    }

    public final void inputChanged(final Viewer viewer, Object oldInput, final Object newInput) {
        disposeImage();
        if (newInput != null)
            loaderJob.asyncLoadImage(newInput, viewer);
    }

    public void setSuggestedSize(Point suggestedSize) {
        this.suggestedSize = suggestedSize;
    }

    protected void reload() {
        this.loaderJob.schedule(200);
    }

    public abstract Image loadImage(Display display, Point suggestedSize, Object newInput) throws CoreException;

    /**
     * {@inheritDoc}
     * 
     * This default implementation relies on the
     * {@link #loadImage(Display, Point, Object)} method, subclasses are
     * encouraged to provide a more efficient implementation.
     */
    public void saveImage(Display display, Point suggestedSize, Object input, IPath location, GraphicFileFormat fileFormat)
            throws CoreException {
    	int swtFileFormat = getSWTFileFormat(fileFormat);
        Image toSave = loadImage(Display.getDefault(), new Point(0, 0), input);
        try {
            ImageLoader imageLoader = new ImageLoader();
            imageLoader.data = new ImageData[] { toSave.getImageData() };
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(200 * 1024);
            imageLoader.save(buffer, swtFileFormat);
            try {
                FileUtils.writeByteArrayToFile(location.toFile(), buffer.toByteArray());
            } catch (IOException e) {
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error saving image", e));
            }
        } finally {
            toSave.dispose();
        }
    }

	private int getSWTFileFormat(GraphicFileFormat fileFormat) {
		switch (fileFormat) {
		case BITMAP:
			return SWT.IMAGE_BMP;
		case GIF:
			return SWT.IMAGE_GIF;
		case JPEG:
			return SWT.IMAGE_JPEG;
		case TIFF:
			return SWT.IMAGE_TIFF;
		case PNG:
		default:
			return SWT.IMAGE_PNG;
		}
	}
}
