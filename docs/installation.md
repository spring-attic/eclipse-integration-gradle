Gradle STS Support -- 3.0.0 (M2) -- Installation
================================================

<small>Note: The latest version of this file can always be found [here][latest].</small>

You can install the Gradle STS support in one of two ways. The easiest
way to install is from the STS Dashboard extensions page. Simply search
for "Gradle" to find the extension, select it and install.

Optionally (if you want editor support) you should also install the "Groovy Eclipse" extension in the same way.

Requirements:
-------------

Either one of the following:

 * An instance of STS 3.0.0.M2 or later based on Eclipse 3.7 or 4.2.
 * An instance of Eclipse 3.7.2 or later. (Earlier versions of Eclipse such as Eclipse 3.6 probably also work but have not been   
   tested).

Optionally (if you want support for editing .gradle files) a compatible Groovy Eclipse installation is 
required (STS 3.0.0.M2) requires Groovy Eclipse 2.7.0. The Gradle tooling should be usable without Groovy Eclipse, but some functionality (related to editing gradle files) will not work.

Installation instructions:
--------------------------

### Installing from the STS dashboard:

The easiest way to install is from the STS Dashboard "Extensions" page.

  1. First download and install a recent release of STS or Groovy and Grails Toolsuite (GGTS) version 3.0.0.M3 or later.
  3. Search for "Gradle" or "Groovy" depending on what you are installing, select it and click "Install".
  4. Review the list of software that will be installed and click "Next".
  5. Review and accept licence agreements and click "Finish".

### Installing Gradle Tooling from update site:

Alternatively you can install from update sites. The following update
sites are available:

  * https://dist.springsource.com/snapshot/TOOLS/nightly/gradle (latest development snapshot)
  * https://dist.springsource.com/milestone/TOOLS/gradle (latest milestone build.)
  * https://dist.springsource.com/release/TOOLS/gradle (latest release)

**Note:** pasting the above URLs into a web browser will not work. You need
to follow the instructions given below to use an Eclipse update site.

**Note:** presently (while STS 3.0.0 is still under development), only snapshot and milestone update sites contain versions 
that are compatible with STS 3.0.0.M3 or later. Once 3.0.0 is released, the release site will also contain a 3.0.0 compatible 
release.

**If you are installing into STS** one of these update sites will be
sufficient.

**If you are installing in a plain Eclipse**, you will also need to add an STS update site to satisfy some dependencies.

  * For eclipse 3.7:
     * STS Release: https://dist.springsource.com/release/TOOLS/update/e3.7/ (this site will only contain a compatible version after 3.0.0 release!)
     * Milestone: https://dist.springsource.com/milestone/TOOLS/update/e3.7/
     * Nightly: https://dist.springsource.com/snapshot/TOOLS/nightly/e3.7/
  * For eclipse 4.2:
     * STS Release: https://dist.springsource.com/release/TOOLS/update/e4.2/ (this site will only contain a compatible version after 3.0.0 release!)
     * Milestone: https://dist.springsource.com/milestone/TOOLS/update/e4.2/
     * Nightly: https://dist.springsource.com/snapshot/TOOLS/nightly/e4.2/
     
#### Detailed instructions: 

Steps 2..4 can be skipped if you are installing into STS.

 1. Open **Help >> Install New Software**
 2. Click "Available Software sites".
 3. Click the "Add" button.
 4. Add an STS update site according to your Eclipse version.
 5. Paste a Gradle update site link into the "Work with" text box.
 6. Select everything on the site.
 7. Click "Next".
 8. Review the list of software that will be installed. Click "Next" again.
 9. Review and accept licence agreement and Click "Finish".
  
### Installing Groovy Eclipse from Update Site

To install Groovy Eclipse from update site see [here][greclipse].

   [latest]: https://static.springsource.org/sts/docs/latest/reference/html/gradle/  "Latest Gradle Docs"
   [greclipse]: https://groovy.codehaus.org/Eclipse+Plugin "Groovy Eclipse"
