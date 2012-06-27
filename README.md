# Gradle IDE

  The Gradle IDE brings you developer tooling for Gradle into Eclipse.

It comes with Spring UAA (User Agent Analysis), an optional component that help us to collect some usage data. This is completely anonymous and helps us to understand better how the tooling is used and how to improve it in the future.

It also comes with the SpringSource Dashboard as an optional component, which brings you up-to-date information about SpringSource-related projects as well as an easy-to-use extension install to get additional tooling add-ons, like the tc Server Integration for Eclipse, the Cloud Foundry Integration for Eclipse, Grails IDE and Groovy Eclipse.

## Installation (Release)

There is no release published yet. Once we have a release out, you will be able to install it from a release update site.

## Installation (Milestone)

There is no milestone of Gradle IDE available yet. Once a milestone is available it will published on a milestone update site.

## Installation (CI builds)

If you want to live on the leading egde, you can already install up-to-date continuous integration buids from this update site:

    <http://dist.springsource.com/snapshot/TOOLS/nightly/gradle>
    
  But take care, those builds could be broken from time to time and might contain non-ship-ready
  features that might never appear in the milestone or release builds.
  
  The contents of the update site is deemed to be compatible with Eclipse 3.7 and Eclipse 4.2 and requires a Java 6 JDK.

## Questions and bug reports:

If you have a question that Google can't answer, the best way is to go to the forum:

    http://forum.springsource.org/forumdisplay.php?32-SpringSource-Tool-Suite

There you can also ask questions and search for other people with related or similar problems (and solutions). New versions of the Grails IDE (and other tooling that is brought to you by SpringSource) are announced there as well.

With regards to bug reports, please go to:

    https://issuetracker.springsource.com/browse/STS

and choose the GRADLE component when filing new issues.

## Developing Gradle IDE

The remainder of this documents expects a familiarity with Eclipse architecture and how plugin development works.  If you are not already familiar with Eclipse development, then you may want to read some tutorials about the Eclipse plugin architecture and how to develop with it.  A good start is here: <http://www.ibm.com/developerworks/library/os-eclipse-plugindev1/>.

TODO: detailed instructions on how to setup a development environment to work on the Gradle IDE.

### Getting the Grails-ide source code into Eclipse

TODO

### Getting the remaining Grails-IDE related source code into Eclipse

TODO: the insstructions below where copied from 'grails-ide'. They need to be updated to reflect gradle-ide. But the section on how to setup groovy-eclipse is likely to remain the same.

By cloning only the `gradle-ide` repository, and not `eclipse-integration-commons` or `groovy-eclipse`, the Grails-IDE projects will resolve against the binaries of your Eclipse installation (aka the target platform).  Unless you explicitly installed the source code, it will not be available to browse.  Cloning these repositories will not only make the source code available for these projects, but it will also make (most of) the Grails-IDE test projects compile cleanly inside of Eclipse:

* Groovy-Eclipse: git@github.com:groovy/groovy-eclipse.git
* Eclipse-Integration-Commons: git@github.com:SpringSource/eclipse-integration-commons.git

More information on setting up the Groovy-Eclipse dev environment is available here: http://groovy.codehaus.org/Getting+Started+With+Groovy-Eclipse+Source+Code.  Note that there are projects that will not compile unless you have m2e (Maven-eclipse support) installed in your Eclipse.  These projects can be closed.

*Important* also, close the org.codehaus.groovy20 plugin unless you are working on Grails 2.2.x or later, which requires Groovy 2.0.

### Getting the tests to compile inside of Eclipse

TODO

### Running the tests inside of Eclipse

TODO

## Building Gradle IDE

TODO: verify that these instructions work for Gradle (current instructions are copied from Grails-Ide and are likely not to be exactly the same).

The Graidle IDE project uses [Maven](http://maven.apache.org/) Tycho [Tycho](http://eclipse.org/tycho) to do continuous integration builds and to produce p2 repos and update sites. To build the project yourself, you can execute:

    mvn -Pe37 -Dmaven.test.skip=true clean install

This will use maven to compile all Gradle-IDE plugins, excluding test bundles and package them up to produce an update site for a snapshot build.  The update site will be located in `gradle-ide/org.springsource.ide.eclipse.gradle.site/target`.

If you want to run tests during your build, then remove `-Dmaven.test.skip=true`.
If you want to also build test bundles but not run tests then replace `-Dmaven.test.skip=true` with '-DskipTests'

## Contributing

Here are some ways for you to get involved in the community:

  * Get involved with the Spring community on the Spring Community Forums.  Please help out on the [forum](http://forum.springsource.org/forumdisplay.php?32-SpringSource-Tool-Suite) by responding to questions and joining the debate.
  * Create [JIRA](https://issuetracker.springsource.com/browse/STS) tickets for bugs and new features and comment and vote on the ones that you are interested in.  
  * Github is for social coding: if you want to write code, we encourage contributions through pull requests from [forks of this repository](http://help.github.com/forking/). If you want to contribute code this way, please reference a JIRA ticket as well covering the specific issue you are addressing.
  * Watch for upcoming articles on Spring by [subscribing](http://www.springsource.org/node/feed) to springframework.org

Before we accept a non-trivial patch or pull request we will need you to sign the [contributor's agreement](https://support.springsource.com/spring_committer_signup). Signing the contributor's agreement does not grant anyone commit rights to the main repository, but it does mean that we can accept your contributions, and you will get an author credit if we do. Active contributors might be asked to join the core team, and given the ability to merge pull requests.

