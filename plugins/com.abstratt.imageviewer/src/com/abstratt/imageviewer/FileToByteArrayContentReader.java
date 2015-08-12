package com.abstratt.imageviewer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public class FileToByteArrayContentReader {

    public byte[] read(IFile input) {
        InputStream contents = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            contents = input.getContents();
            IOUtils.copy(contents, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            Activator.logUnexpected(null, e);
        } catch (CoreException e) {
            Activator.logUnexpected(null, e);
        } finally {
            IOUtils.closeQuietly(contents);
        }
        return null;
    }

}
