# EclipseGraphviz #

This is the home for the EclipseGraphviz project, originally on SourceForge.

[![Build Status](https://textuml.ci.cloudbees.com/buildStatus/icon?job=eclipse-graphviz)](https://textuml.ci.cloudbees.com/job/eclipse-graphviz/)

EclipseGraphviz is Eclipse plug-in that provides a Java API for Graphviz and allows easily visualizing the graphical output.

#Installing the TextUML Toolkit

Requirements
------------

-   Java 8
-   Eclipse Luna or later

Install Eclipse
---------------

-   If you don't have Eclipse Luna or newer, install it from
    [http://eclipse.org/downloads/](http://eclipse.org/downloads/ "http://eclipse.org/downloads/")
-   Start Eclipse

Method 1: Installation via Eclipse Marketplace (preferred)
-------------------------------

If you have the [Eclipse
Marketplace](http://marketplace.eclipse.org/marketplace-client-intro "http://marketplace.eclipse.org/marketplace-client-intro")
client installed, that is the easiest way to install EclipseGraphviz. Just search for the "TextUML Toolkit" (EclipseGraphviz ships as part of it), and install it directly. You can install EclipseGraphviz and skip installing the TextUML Toolkit if you prefer ([what is the TextUML Toolkit?](http://abstratt.com/textuml)). 

Method 2: Installation via Update Manager 
-------------------------------

-   Open the [Software
    Updates](http://help.eclipse.org/stable/topic/org.eclipse.platform.doc.user/tasks/tasks-121.htm "http://help.eclipse.org/stable/topic/org.eclipse.platform.doc.user/tasks/tasks-121.htm")
    dialog (Help \> Install New Software...), and enter the following
    JAR URL in the "Work with:" field (include jar: to the !/ at the
    end):

<pre>jar:http://repository-textuml.forge.cloudbees.com/snapshot/com/abstratt/mdd/com.abstratt.mdd.oss.repository/2.1/com.abstratt.mdd.oss.repository-2.1.zip!/</pre>

-   Select the EclipseGraphviz feature from the Modeling category.

-   Accept to restart Eclipse to make the changes effective.

