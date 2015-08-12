package com.abstratt.imageviewer;

import java.net.URI;

import org.eclipse.core.resources.IFile;

public class FileToURIReader {
    public URI read(IFile input) {
        return input.getLocationURI();
    }
}
