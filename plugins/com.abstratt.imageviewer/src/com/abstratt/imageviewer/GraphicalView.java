package com.abstratt.imageviewer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

import com.abstratt.content.ContentSupport;
import com.abstratt.content.IContentProviderRegistry.IProviderDescription;

/**
 * A view that wraps a {@link GraphicalViewer}.
 */
public class GraphicalView extends ViewPart implements IResourceChangeListener, IPartListener2, ISelectionListener {
	public final static String VIEW_ID = "com.abstratt.imageviewer.GraphicalView";
	private Canvas canvas;
	private GraphicalViewer viewer;
	private String basePartName;
	private IFile selectedFile;
	private IProviderDescription providerDefinition;
	/**
	 * Whether this view should try to automatically react to changes in selection/selected object.
	 */
	private boolean autoSync = true;

	/**
	 * The constructor.
	 */
	public GraphicalView() {
		//
	}
	
	public GraphicalViewer getViewer() {
        return viewer;
    };

	/**
	 * This is a callback that will allow us to create the viewer and
	 * initialize it.
	 */
	@Override
	public void createPartControl(Composite parent) {
		basePartName = getPartName();
		canvas = new Canvas(parent, SWT.NONE);
		viewer = new GraphicalViewer(canvas);
		installResourceListener();
		installSelectionListener();
		installPartListener();
	}
	
	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		getSite().getPage().removePartListener(this);
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
		super.dispose();
	}

	private void installPartListener() {
		getSite().getPage().addPartListener(this);
		// tries to load an image for the current active part, if any
		final IWorkbenchPartReference activePartReference = getSite().getPage().getActivePartReference();
		if (activePartReference != null)
			reactToPartChange(activePartReference);
	}

	private void installResourceListener() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	private void installSelectionListener() {
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (!autoSync)
			return;
		if (!(selection instanceof IStructuredSelection))
			return;
		IStructuredSelection structured = (IStructuredSelection) selection;
		if (structured.size() != 1)
			return;
		Object selected = structured.getFirstElement();
		IFile file = (IFile) Platform.getAdapterManager().getAdapter(selected, IFile.class);
		reload(file);
	}

	public void partActivated(IWorkbenchPartReference partRef) {
		reactToPartChange(partRef);
	}

	public void partBroughtToTop(IWorkbenchPartReference partRef) {
		reactToPartChange(partRef);
	}

	public void partClosed(IWorkbenchPartReference partRef) {
		//
	}

	public void partDeactivated(IWorkbenchPartReference partRef) {
		//
	}

	public void partHidden(IWorkbenchPartReference partRef) {
		//
	}

	public void partInputChanged(IWorkbenchPartReference partRef) {
		reactToPartChange(partRef);
	}

	public void partOpened(IWorkbenchPartReference partRef) {
		reactToPartChange(partRef);
	}

	public void partVisible(IWorkbenchPartReference partRef) {
		reactToPartChange(partRef);
	}

	private void reactToPartChange(IWorkbenchPartReference part) {
		if (!autoSync)
			return;
		if (!(part.getPart(false) instanceof IEditorPart))
			return;
		IEditorPart editorPart = (IEditorPart) part.getPart(false);
		if (!getViewSite().getPage().isPartVisible(editorPart))
			return;
		IGraphicalFileProvider graphicalSource =
						(IGraphicalFileProvider) editorPart.getAdapter(IGraphicalFileProvider.class);
		if (graphicalSource != null)
			selectedFile = graphicalSource.getGraphicalFile();
		else 
			selectedFile = null;
		if (selectedFile == null) {
			IFile asFile = (IFile) editorPart.getAdapter(IFile.class);
			if (asFile == null)
				asFile = (IFile) editorPart.getEditorInput().getAdapter(IFile.class);
			if (asFile == null)
				return;
			selectedFile = asFile;
		}
		requestUpdate();
	}

	private void reload(IFile file) {
		setPartName(basePartName);
		this.providerDefinition = null;
		this.selectedFile = null;
		if (file == null || !file.exists())
			return;
		if (viewer.getContentProvider() != null)
			// to avoid one provider trying to interpret an
			// incompatible input
			viewer.setInput(null);
		IContentDescription contentDescription = null;
		try {
			contentDescription = file.getContentDescription();
		} catch (CoreException e) {
			if (Platform.inDebugMode())
				Activator.logUnexpected(null, e);
		}
		if (contentDescription == null || contentDescription.getContentType() == null)
			return;
		IProviderDescription providerDefinition =
						ContentSupport.getContentProviderRegistry().findContentProvider(
										contentDescription.getContentType(), IGraphicalContentProvider.class);
		if (providerDefinition == null) {
			return;
		}
		setPartName(basePartName + " - " + file.getName());
		IGraphicalContentProvider provider = (IGraphicalContentProvider) providerDefinition.getProvider();
		setContents(providerDefinition.read(file), provider);
		// enables support for file rendering
		this.selectedFile = file;
		this.providerDefinition = providerDefinition;
	}

	/**
	 * Force feeds the contents to be shown.
	 * 
	 * @param contents the contents to be presented (as expected by the content provider
	 * @param provider the graphical content provider that can render the given contents 
	 */
	public void setContents(Object contents,
			IGraphicalContentProvider provider) {
		// assumes the general case (instead of file rendering)
		selectedFile = null;
		providerDefinition = null;
		viewer.setContentProvider(provider);
		viewer.setInput(contents);
	}

	private void requestUpdate() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				if (getSite() == null || !GraphicalView.this.getSite().getPage().isPartVisible(GraphicalView.this))
					// don't do anything if we are not showing
					return;
				reload(selectedFile);
			}
		});
	}

	public void resourceChanged(IResourceChangeEvent event) {
		if (selectedFile == null || !selectedFile.exists())
			return;
		if (event.getDelta() == null)
			return;
		IResourceDelta interestingChange = event.getDelta().findMember(selectedFile.getFullPath());
		if (interestingChange != null)
			requestUpdate();
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	public IProviderDescription getContentProviderDescription() {
		return providerDefinition;
	}
	
	public IFile getSelectedFile() {
		return selectedFile;
	}
	
	public boolean isAutoSync() {
		return autoSync;
	}
	
	/**
	 * Enables/disables the view auto-sync feature.
	 * 
	 * @param autoSync whether the view should auto synchronize to the current selection.
	 */
	public void setAutoSync(boolean autoSync) {
		this.autoSync = autoSync;
		updateAutoSyncToggleButtonState();
	}

	private void updateAutoSyncToggleButtonState() {
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
		ActionContributionItem autoSyncToggleContribution = (ActionContributionItem) toolBarManager.find("com.abstratt.imageviewer.autoUpdate");
		if (autoSyncToggleContribution != null) {
			IAction action = autoSyncToggleContribution.getAction();
			action.setChecked(isAutoSync());
		}
	}
	
	
	/**
	 * Toggles the view auto-sync state.
	 */
	public void toggleSync() {
		setAutoSync(!isAutoSync()) ;
	}
}