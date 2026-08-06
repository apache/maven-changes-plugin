<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Apache Maven Changes Plugin
This plugin is used to inform your users of the changes that have occurred between different releases of your project. The plugin can extract these changes, either from a `changes.xml` file or from an issue management system (Jira, Trac and GitHub supported), and presents them as a report.

You also have the option of creating a release announcement and even sending it via email to your users.

## Goals Overview

- [changes:announcement-mail](./announcement-mail-mojo.html) send a release announcement via email.
- [changes:announcement-generate](./announcement-generate-mojo.html) generate a release announcement.
- [changes:changes-check](./changes-check-mojo.html) check that the `changes.xml` file contains a valid release date.
- [changes:changes-validate](./changes-validate-mojo.html) validate the `changes.xml` file.
- [changes:changes](./changes-mojo.html) create a report from `changes.xml` file.
- [changes:jira-changes](./jira-changes-mojo.html) create a report from issues downloaded from [JIRA](https://www.atlassian.com/software/jira/).
- [changes:trac-changes](./trac-changes-mojo.html) create a report from issues downloaded from [Trac](https://trac.edgewall.org/).
- [changes:github-changes](./github-changes-mojo.html) create a report from issues downloaded from [GitHub](https://github.com/).
## Usage

General instructions on how to use the Changes Plugin can be found on the [usage page](./usage.html). Some more specific use cases are described in the examples given below.

In case you still have questions regarding the plugin's usage, please have a look at the [FAQ](./faq.html) and feel free to contact the [user mailing list](./mailing-lists.html). The posts to the mailing list are archived and could already contain the answer to your question as part of an older thread. Hence, it is also worth browsing/searching the [mail archive](./mailing-lists.html).

If you feel like the plugin is missing a feature or has a defect, you can fill a feature request or bug report in our [issue tracker](./issue-management.html). When creating a new issue, please provide a comprehensive description of your concern. Especially for fixing bugs it is crucial that the developers can reproduce your problem. For this reason, entire debug logs, POMs or most preferably little demo projects attached to the issue are very much appreciated. Of course, patches are welcome, too. Contributors can check out the project from our [source repository](./scm.html) and will find supplementary information in the [guide to helping with Maven](https://maven.apache.org/guides/development/guide-helping.html).

## Migration to 3.x

### changes.xml - schema changes

- you should update schema in your changes.xml to `2.0.0` - [Using the XML Schema](./using-changes-xsd.html).
- tag `action/dueto` - was removed, you can put your data into existing `due-to` and `due-to-email` attributes of `action` tag, which can be comma separated.
- tag `action/fixedIssues` - was changed to an attribute `fixed-issues` of **action** tag, it also can be comma separated.
### Report goals and output names

The reports output filename and goals name were changed for alignment with other reporting plugins from `org.apache.maven.plugins`.

See the following table for changes:

|goal name|output name|
|:---|:---|
|changes-report -&gt; changes|changes-report.html -&gt; changes.html|
|github-report -&gt; github-changes|github-report.html -&gt; github-changes.html|
|jira-report -&gt; jira-changes|jira-report.html -&gt; jira-changes.html|
|trac-report -&gt; trac-changes|trac-report.html -&gt; trac-changes.html|

### Deprecate Trac integration

**Trac** integration is prepared for removal in next major version due to lack of maintainers.

## Examples

To provide you with better understanding of some usages of the Changes Plugin, you can take a look at the following examples:

- [Alternate Location for the `changes.xml` File](./examples/alternate-changes-xml-location.html)
- [Check Your `changes.xml` File](./examples/check-changes-file.html)
- [Configuring the Trac Report](./examples/configuring-trac-report.html)
- [Customizing the JIRA Report](./examples/customizing-jira-report.html)
- [Configuring the GitHub Report](./examples/configuring-github-report.html)
- [Include an Announcement File in Your Packaging](./examples/include-announcement-file.html)
- [SMTP authentication](./examples/smtp-authentication.html)
- [Specifying the mail sender](./examples/specifying-mail-sender.html)
- [Using a Custom Announcement Template](./examples/using-a-custom-announcement-template.html)
- [Validate Your `changes.xml` File](./examples/changes-file-validation.html)
