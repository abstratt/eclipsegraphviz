package com.abstratt.imageviewer;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class ToggleSyncAction implements IViewActionDelegate {

    private GraphicalView view;

    public void init(IViewPart view) {
        this.view = (GraphicalView) view;
    }

    public void run(IAction action) {
        this.view.toggleSync();
    }

    public void selectionChanged(IAction action, ISelection selection) {
        // don't care
    }
}
