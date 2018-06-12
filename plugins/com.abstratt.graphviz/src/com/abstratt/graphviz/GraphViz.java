/*******************************************************************************
 * Copyright (c) 2007 EclipseGraphviz contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     abstratt technologies
 *     Scott Bronson
 *******************************************************************************/
package com.abstratt.graphviz;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import com.abstratt.graphviz.ProcessController.TimeOutException;
import com.abstratt.pluginutils.LogUtils;

/**
 * The entry point to the Graphviz support API.
 */
// TODO generate and load have a lot in common, refactor
// TODO we should just pass the input stream directly (or buffered) to Graphviz,
// instead of creating a temporary file
public class GraphViz {
    private static final String DOT_EXTENSION = ".dot"; //$NON-NLS-1$
    private static final String TMP_FILE_PREFIX = "graphviz"; //$NON-NLS-1$
    private static final int MAX_DOT_LENGTH_TO_LOG = 4 * 64 * 1024;

    public static void generate(final InputStream input, String format, int dimensionX, int dimensionY,
            IPath outputLocation) throws CoreException {
        MultiStatus status = new MultiStatus(GraphVizActivator.ID, 0, "Errors occurred while running Graphviz", null);
        File dotInputFile = null, dotOutputFile = outputLocation.toFile();
        // we keep the input in memory so we can include it in error messages
        ByteArrayOutputStream dotContents = new ByteArrayOutputStream();
        try {
            // determine the temp input location
            dotInputFile = File.createTempFile(TMP_FILE_PREFIX, DOT_EXTENSION);
            // dump the contents from the input stream into the temporary file
            // to be submitted to dot
            FileOutputStream tmpDotOutputStream = null;
            try {
                IOUtils.copy(input, dotContents);
                tmpDotOutputStream = new FileOutputStream(dotInputFile);
                IOUtils.copy(new ByteArrayInputStream(dotContents.toByteArray()), tmpDotOutputStream);
            } finally {
                IOUtils.closeQuietly(tmpDotOutputStream);
            }
            IStatus result = runDot(format, dimensionX, dimensionY, dotInputFile, dotOutputFile);
            status.add(result);
            if (dotOutputFile.isFile() && dotOutputFile.length() > 0) {
                if (!result.isOK() && Platform.inDebugMode())
                    LogUtils.log(status);
                // success!
                return;
            }
        } catch (IOException e) {
            status.add(new Status(IStatus.ERROR, GraphVizActivator.ID, "", e));
        } finally {
            dotInputFile.delete();
            IOUtils.closeQuietly(input);
        }
        throw new CoreException(status);
    }

    /**
     * Higher-level API for launching a GraphViz transformation.
     * 
     * @return the resulting image, never <code>null</code>
     * @throws CoreException
     *             if any error occurs
     */
    public static byte[] load(final InputStream input, String format, int dimensionX, int dimensionY)
            throws CoreException {
        MultiStatus status = new MultiStatus(GraphVizActivator.ID, 0, "Errors occurred while running Graphviz", null);
        File dotInputFile = null, dotOutputFile = null;
        // we keep the input in memory so we can include it in error messages
        ByteArrayOutputStream dotContents = new ByteArrayOutputStream();
        try {
            // determine the temp input and output locations
            dotInputFile = File.createTempFile(TMP_FILE_PREFIX, DOT_EXTENSION);
            dotOutputFile = File.createTempFile(TMP_FILE_PREFIX, "." + format);
            // we created the output file just so we would know an output
            // location to pass to dot
            dotOutputFile.delete();

            // dump the contents from the input stream into the temporary file
            // to be submitted to dot
            byte[] contentsAsArray = null;
            try (FileOutputStream tmpDotOutputStream = new FileOutputStream(dotInputFile)) {
                IOUtils.copy(input, dotContents);
                contentsAsArray = dotContents.toByteArray();
                IOUtils.copy(new ByteArrayInputStream(contentsAsArray), tmpDotOutputStream);
            }

            IStatus result = runDot(format, dimensionX, dimensionY, dotInputFile, dotOutputFile);

            status.add(result);
            status.add(logInput(contentsAsArray));
            if (dotOutputFile.isFile()) {
                if (!result.isOK() && Platform.inDebugMode())
                    LogUtils.log(status);
                return FileUtils.readFileToByteArray(dotOutputFile);
            }
        } catch (IOException e) {
            status.add(new Status(IStatus.ERROR, GraphVizActivator.ID, "", e));
        } finally {
            dotInputFile.delete();
            dotOutputFile.delete();
            IOUtils.closeQuietly(input);
        }
        throw new CoreException(status);
    }

    public static IStatus runDot(String format, int dimensionX, int dimensionY, File dotInput, File dotOutput) {
        // build the command line
        double dpi = 96;
        double widthInInches = dimensionX / dpi;
        double heightInInches = dimensionY / dpi;
        List<String> cmd = new ArrayList<String>();
        cmd.add("-o" + dotOutput.getAbsolutePath());
        cmd.add("-T" + format);
        if (widthInInches > 0 && heightInInches > 0)
            cmd.add("-Gsize=" + widthInInches + ',' + heightInInches);
        cmd.add(dotInput.getAbsolutePath());
        return runDot(cmd.toArray(new String[cmd.size()]));
    }

    private static IStatus logInput(byte[] dotContents) {
        String dotInput = new String(dotContents, 0, Math.min(dotContents.length, MAX_DOT_LENGTH_TO_LOG), StandardCharsets.UTF_8);
        return new Status(IStatus.INFO, GraphVizActivator.ID, "dot input was:\n" + dotInput, null);
    }

    /**
     * Bare bones API for launching dot. Command line options are passed to
     * Graphviz as specified in the options parameter. The location for dot is
     * obtained from the user preferences.
     * 
     * @param options
     *            command line options for dot
     * @return a non-zero integer if errors happened
     * @throws IOException
     */
    public static IStatus runDot(String... options) {
        IPath dotFullPath = GraphVizActivator.getInstance().getDotLocation();
        if (dotFullPath == null || dotFullPath.isEmpty())
            return new Status(
                    IStatus.ERROR,
                    GraphVizActivator.ID,
                    "dot.exe/dot not found in PATH. Please install it from graphviz.org, update the PATH or specify the absolute path in the preferences.");
        if (!dotFullPath.toFile().isFile())
            return new Status(IStatus.ERROR, GraphVizActivator.ID, "Could not find Graphviz dot at \"" + dotFullPath
                    + "\"");
        List<String> cmd = new ArrayList<String>();
        cmd.add(dotFullPath.toOSString());
        // insert user custom options
        String commandLineExtension = GraphVizActivator.getInstance().getCommandLineExtension();
        if (commandLineExtension != null) {
            String[] tokens = commandLineExtension.split(" ");
            cmd.addAll(Arrays.asList(tokens));
        }
        cmd.addAll(Arrays.asList(options));

        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        try {
            final ProcessController controller = new ProcessController(90000, cmd.toArray(new String[cmd.size()]),
                    null, dotFullPath.removeLastSegments(1).toFile());
            controller.forwardErrorOutput(errorOutput);
            controller.forwardOutput(System.out);
            controller.forwardInput(System.in);
            int exitCode = controller.execute();
            if (exitCode != 0)
                return new Status(IStatus.WARNING, GraphVizActivator.ID, "Graphviz exit code: " + exitCode + "."
                        + createContentMessage(errorOutput));
            if (errorOutput.size() > 0)
                return new Status(IStatus.WARNING, GraphVizActivator.ID, createContentMessage(errorOutput));
            return Status.OK_STATUS;
        } catch (TimeOutException e) {
            return new Status(IStatus.ERROR, GraphVizActivator.ID, "Graphviz process did not finish in a timely way."
                    + createContentMessage(errorOutput));
        } catch (InterruptedException e) {
            return new Status(IStatus.ERROR, GraphVizActivator.ID, "Unexpected exception executing Graphviz."
                    + createContentMessage(errorOutput), e);
        } catch (IOException e) {
            return new Status(IStatus.ERROR, GraphVizActivator.ID, "Unexpected exception executing Graphviz."
                    + createContentMessage(errorOutput), e);
        }
    }

    private static String createContentMessage(ByteArrayOutputStream errorOutput) {
        if (errorOutput.size() == 0)
            return "";
        return " dot produced the following error output: \n" + errorOutput;
    }
}
