package com.abstratt.imageviewer;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import com.abstratt.content.IContentProviderRegistry.IProviderDescription;
import com.abstratt.content.PlaceholderProviderDescription;
import com.abstratt.imageviewer.IGraphicalContentProvider.GraphicFileFormat;

public class SaveToFileAction implements IViewActionDelegate {

    private GraphicalView view;

    public void init(IViewPart view) {
        this.view = (GraphicalView) view;
    }

    public void run(IAction action) {
        IProviderDescription providerDefinition = view.getContentProviderDescription();
        IGraphicalContentProvider contentProvider = view.getContentProvider();
        if (providerDefinition == null)
			providerDefinition = new PlaceholderProviderDescription(view.getInput(), contentProvider);
        
        IFile selectedFile = view.getSelectedFile();
        String suggestedName;
        if (selectedFile == null)
            suggestedName = "image";
        else
            suggestedName = selectedFile.getLocation().removeFileExtension().lastSegment();
        boolean pathIsValid = false;
        IPath path = null;
        GraphicFileFormat fileFormat = null;
        while (!pathIsValid) {
            FileDialog saveDialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
            saveDialog.setText("Choose a location to save to");
            saveDialog.setFileName(suggestedName);
            saveDialog.setFilterExtensions(contentProvider.getSupportedFormats().stream().map(it -> "*." + it.getExtension()).toArray(s -> new String[s]));
            String pathString = saveDialog.open();
            if (pathString == null)
                return;
            path = Path.fromOSString(pathString);
            if (path.toFile().isDirectory()) {
                MessageDialog.openError(null, "Invalid file path", "Location is already in use by a directory");
                continue;
            }
            fileFormat = GraphicFileFormat.byExtension(path.getFileExtension()); 
            if (fileFormat == null) {
                MessageDialog.openError(null, "Invalid file extension", "Supported file formats are: "
                        + contentProvider.getSupportedFormats().toString());
                continue;
            }
            File parentDir = path.toFile().getParentFile();
            parentDir.mkdirs();
            if (parentDir.isDirectory())
                pathIsValid = true;
            else
                MessageDialog.openError(null, "Invalid file path", "Could not create directory");
        }

        new SaveImageJob(fileFormat, path, providerDefinition).schedule();
    }

    private class SaveImageJob extends Job {

        private IProviderDescription providerDefinition;
        private IPath path;
        private GraphicFileFormat fileFormat;

        public SaveImageJob(GraphicFileFormat fileFormat, IPath path, IProviderDescription providerDefinition) {
            super("Image saving job");
            this.fileFormat = fileFormat;
            this.path = path;
            this.providerDefinition = providerDefinition;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            monitor.beginTask("saving image", 100);
            getJobManager().beginRule(AbstractGraphicalContentProvider.CONTENT_LOADING_RULE, monitor);
            try {
                IGraphicalContentProvider provider = (IGraphicalContentProvider) providerDefinition.getProvider();
                Object input = providerDefinition.read(view.getSelectedFile());
                provider.saveImage(Display.getDefault(), new Point(0, 0), input, path, fileFormat);
            } catch (CoreException e) {
                return e.getStatus();
            } finally {
                getJobManager().endRule(AbstractGraphicalContentProvider.CONTENT_LOADING_RULE);
                monitor.done();
            }
            return Status.OK_STATUS;
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
        // don't care
    }

}
