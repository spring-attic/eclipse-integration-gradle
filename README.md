Eclipse Integration Gradle
==========================

The Eclipse-Integration-Gradle project brings you developer tooling for Gradle into Eclipse.

It comes with Spring UAA (User Agent Analysis), an optional component that help us to collect some usage data. This is completely anonymous and helps us to understand better how the tooling is used and how to improve it in the future.

It also comes with the SpringSource Dashboard as an optional component, which brings you up-to-date information about SpringSource-related projects as well as an easy-to-use extension install to get additional tooling add-ons, like the tc Server Integration for Eclipse, the Cloud Foundry Integration for Eclipse, Grails IDE and Groovy Eclipse.

Requirements (for version 3.0.0 of the Gradle tooling)
------------------------------------------------------

Either one of the following:

 * An instance of STS 3.0.0 or later based on Eclipse 3.7 or 4.2.
 * An instance of Eclipse 3.7.2 or later. (Earlier versions of Eclipse such as Eclipse 3.6 probably also work but have not been   
   tested).

If you want support for editing .gradle files a compatible Groovy Eclipse installation is 
required. STS 3.0.0 requires Groovy Eclipse 2.7.0. The Gradle tooling should be usable without Groovy Eclipse, 
but some functionality (related to editing gradle files) will not work.

Documentation 
-------------

Documentation for the functionality offered by Eclipse-Integration-Gradle tooling is available from [our
project's GitHub Wiki](https://github.com/SpringSource/eclipse-integration-gradle/wiki).

Installation instructions:
--------------------------

### Installing from the STS dashboard:

The easiest way to install is from the STS Dashboard "Extensions" page.

  1. First download and install a [recent release of STS](http://www.springsource.org/springsource-tool-suite-download) 
     or Groovy and Grails Toolsuite (GGTS) version 3.0.0 or later.
  2. Open the Dashboard and select the 'Extensions' Tab.
  3. Search for "Gradle" or "Groovy" depending on what you are installing, select it and click "Install".
  4. Review the list of software that will be installed and click "Next".
  5. Review and accept licence agreements and click "Finish".

###Installing Gradle Tooling from update site

Alternatively you can install from update sites. The following update
sites are available:

  * http://dist.springsource.com/snapshot/TOOLS/gradle/nightly (latest development snapshot)
  * http://dist.springsource.com/milestone/TOOLS/gradle (latest milestone build)
  * http://dist.springsource.com/release/TOOLS/gradle (latest release)
  
Pasting the above URLs into a web browser will not work. You need
to follow the instructions given below to use an Eclipse update site.

 1. In Eclipse Open **Help >> Install New Software**
 2. Paste a Gradle update site link into the "Work with" text box.
 3. Click the Add button at the top of the screen.
 4. Ensure that the option "Group Items by Category" is enabled.
 5. Select the top-level node 'Extensions / Gradle Integration'. 
 6. Click "Next".  This may take a while.
 7. Review the list of software that will be installed. Click "Next" again.
 8. Review and accept licence agreements and Click "Finish".

If you follow this installation procedure in a plain Eclipse, this will install the STS Dashboard.
This gives you an easy way to subsequently install Groovy Eclipse as well. See instructions on 
[Installing from the STS Dashboard](#installing-from-the-sts-dashboard) above. 

## Questions and bug reports:

If you have a question that Google can't answer, the best way is to go to the [STS forum](http://forum.springsource.org/forumdisplay.php?32-SpringSource-Tool-Suite).

There you can also ask questions and search for other people with related or similar problems (and solutions). New versions of the Gradle Eclipse Integration (and other tooling that is brought to you by SpringSource) are announced there as well.

With regards to bug reports, please go to the [STS issue tracker](https://issuetracker.springsource.com/browse/STS) and choose the [GRADLE component](https://issuetracker.springsource.com/browse/STS-3405?jql=project%20%3D%20STS%20AND%20resolution%20%3D%20Unresolved%20AND%20component%20%3D%20GRADLE%20ORDER%20BY%20priority%20DESC) when filing new issues.

## Developing Eclipse Integration Gradle

The remainder of this documents expects a familiarity with Eclipse architecture and how plugin development works.  If you are not already familiar with Eclipse development, then you may want to read some tutorials about the Eclipse plugin architecture and how to develop with it.  A good start is here: <http://www.ibm.com/developerworks/library/os-eclipse-plugindev1/>.

### Setting up the Development Environment and Target Platform  

The instructions here are tested starting with a 'clean' copy of Eclipse 4.4 JEE. It is also possible to setup a similar environment based on older versions of Eclipse.

First we will start by setting up a suitable instance of Eclipse. This instance will serve a double purpose:
  
 - it will be our development environment.
 - it will serve as the 'target platform' against which Eclipse can compile and run our code.

Steps:

 1. download and install Eclipse for Java EE Developers from [eclipse.org](http://www.eclipse.org/downloads/).
 2. install egit support (e.g. from the Eclipse Market Place). This step is optional, you can also use git commandline tools.
 4. install Groovy Eclipse from this update site: `http://dist.codehaus.org/groovy/distributions/greclipse/snapshot/e4.2/`
    - install everything on the update site except 'm2e Configurator ...'
    - make sure you *do* install 'Groovy Eclipse Test Feature' if you want to be able to compile and run the Gradle IDE regressions tests.
 5. install Eclipse Integration Gradle tooling from this update site: `http://dist.springsource.com/snapshot/TOOLS/gradle/nightly`
 
### Getting the source code

The source code for this project is [hosted on github](https://github.com/SpringSource/eclipse-integration-gradle). You can use egit (Eclipse Integration for Git) or commandline git tools to get the source code on to your machine.

To get the source code onto your machine:

    git clone git://github.com/SpringSource/eclipse-integration-gradle.git
    
or if you are a committer:

    git clone git@github.com:SpringSource/eclipse-integration-gradle.git

### Importing the source code into Eclipse

The source code consists of a single root project and several sub-projects nested within it. You can use either 'EGit' or the generic Eclipse 'Import Existing Projects' wizard. However both wizards are tripped up by the nested project structure. As soon as the Wizard sees the root `.project` file it will stop looking for nested projects. To side-step this problem simply delete the root `.project` file before importing the subprojects.

    cd eclipse-integration-gradle
    rm .project

After importing the sub-projects, reinstate the root `.project` file:
   
    git checkout .project

If you want to, you can now import the root project as well.

### Getting and Updating the Tooling API Jars

At this point you likely have errors in your workspace like 'The project cannot be build until build path errors are resolved'.

Our tools are built on top of the Gradle Tooling API. However the Tooling API jars are not included in the git repository. They have to be downloaded and placed into `org.springsource.ide.eclipse.gradle.core/lib`. This is accomplished by using gradle itself to download the libraries.

Open the 'Gradle Tasks' View (via Window >> Show View menu).

  - select project `org.springsource.ide.eclipse.gradle.toolingapi`.
  - run the task called `updateLibs`.

You can also do this on the commandline:
    
    cd org.springsource.ide.eclipse.gradle.toolingapi/
    ./gradlew updateLibs

If you do this on the commandline you will need to manually refresh the `org.springsource.ide.eclipse.gradle.core` project in the Eclipse workspace afterwards. If you use the 'Gradle Tasks View' then the refresh is automatic.

Note that the script only downloads the jars and updates the plugin `MANIFEST.MF` but it does not automatically update the `.classpath` file in the project. Normally neither of these files changes unless the Tooling API version has changed. In this case no further action is required.
However if `MANIFEST.MF` did change then you will have to force Eclipse to update the .classpath file as well. To do this, right click the project and select 'Plugin Tools >> Update Classpath' (note: in Eclipse 3.7 the menu is called 'PDE Tools').

After this all compile errors should disappear. (Note: In Eclipse 4.2 I have on occasion noticed that you need to force a workspace rebuild by cleaning the project before errors actually disappear).

### Running the tests inside of Eclipse

To run the regression tests inside of Eclipse. Find the class 'AllGradleCoreTests' and run it as a 'Junit Plugin Test'. You can also run the smaller testsuites individually in the same way.

Notes: 

  - `testImportSpringSecurity` will fail if you have followed these instructions exactly.
     
     This is expected because the spring-security-code that is imported in this test requires a very specific version of the Groovy Compiler 
     (1.7 at the time of this writing) 
     You can ignore this error or edit the launch configuration to disable all but the 1.7 version of the org.codehouse.groovy bundles.

## Building Gradle IDE

The Gradle IDE project uses [Maven](http://maven.apache.org/) [Tycho](http://eclipse.org/tycho) to do continuous integration builds and to produce p2 repos and update sites. To build the project yourself, you can execute:

    mvn -Pe42 -Dmaven.test.skip=true clean install 
    
This will use maven to compile all Gradle-IDE plugins, excluding test bundles and package them up to produce an update site for a snapshot build.  The update site will be located in `gradle-ide/org.springsource.ide.eclipse.gradle.site/target`.

If you want to run tests during your build, then remove `-Dmaven.test.skip=true`.
If you want to also build test bundles but not run tests then replace `-Dmaven.test.skip=true` with `-DskipTests`.
If you want to build against Eclipse 3.7 instead of Eclipse 4.2 than use `-Pe37` instead of `-Pe42`.

## Contributing

Here are some ways for you to get involved in the community:

  * Get involved with the Spring community on the Spring Community Forums.  Please help out on the [forum](http://forum.springsource.org/forumdisplay.php?32-SpringSource-Tool-Suite) by responding to questions and joining the debate.
  * Create [JIRA](https://issuetracker.springsource.com/browse/STS) tickets for bugs and new features and comment and vote on the ones that you are interested in.  
  * Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](http://help.github.com/forking/). If you want to contribute code this way, please reference a JIRA ticket as well covering the specific issue you are addressing.
  * Watch for upcoming articles on Spring by [subscribing](http://www.springsource.org/node/feed) to springframework.org

Before we accept a patch or pull request we will need you to sign the [contributor's agreement](https://support.springsource.com/spring_eclipsecla_committer_signup). Signing the contributor's agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do. Active contributors might be asked to join the core team, and given the ability to merge pull requests.

