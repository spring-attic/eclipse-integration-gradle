All the jar files in this directory will be added automatically into the Gradle DSLD Classpath container.
This directory therefore should only contain 'active' Gradle API jars and their dependencies. 

Make sure to delete older versions of the jars when upgrading to a newer version.

See ../build.gradle and the 'updateLibs' task to download the required libraries.