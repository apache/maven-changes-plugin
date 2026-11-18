---
title: Introduction
author: 
  - Dennis Lundberg
date: 2013-07-22
---

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

The Changes Plugin tells your users what changed between releases of your project. It reads the changes either
from a `changes.xml` file that you keep in the project, or from an issue tracker (JIRA, GitHub or Trac), and turns
them into a site report or a plain-text release announcement.

Most users come for one of these tasks:

- Publish a changes report built from `src/changes/changes.xml`. Add the `changes` report to the `<reporting>`
  section and run `mvn site`, or run the report on its own:

  ```
  mvn changes:changes
  ```

- Publish a report of the issues in your tracker, here GitHub (configured in `<issueManagement>`):

  ```
  mvn changes:github-changes
  ```

- Check `changes.xml` against the schema and make sure the release you build has a release date:

  ```
  mvn changes:changes-validate changes:changes-check
  ```

- Generate a release announcement text in `target/announcement/`, and optionally send it by email:

  ```
  mvn changes:announcement-generate
  ```

The plugin requires Maven 3.9.0 or later and Java 8 or later.

## Goals Overview

- [changes:changes](./changes-mojo.html) creates a report from the `changes.xml` file.
- [changes:github-changes](./github-changes-mojo.html) creates a report from issues downloaded from
  [GitHub](https://github.com/).
- [changes:jira-changes](./jira-changes-mojo.html) creates a report from issues downloaded from
  [JIRA](https://www.atlassian.com/software/jira).
- [changes:trac-changes](./trac-changes-mojo.html) creates a report from issues downloaded from
  [Trac](https://trac.edgewall.org/). Deprecated, see [below](#Deprecate_Trac_integration).
- [changes:changes-validate](./changes-validate-mojo.html) validates the `changes.xml` file against its XML schema.
- [changes:changes-check](./changes-check-mojo.html) checks that the `changes.xml` file contains a valid release date
  for the current version.
- [changes:announcement-generate](./announcement-generate-mojo.html) generates a release announcement.
- [changes:announcement-mail](./announcement-mail-mojo.html) generates the release announcement and sends it by email.
- [changes:help](./help-mojo.html) displays help information on the plugin.

The goals `changes-report`, `github-report`, `jira-report` and `trac-report` are deprecated aliases of the report
goals above; see [Report goals and output names](#Report_goals_and_output_names).

## Usage

General instructions on how to use the Changes Plugin can be found on the [usage page](./usage.html). Some more
specific use cases are described in the examples given below.

If you still have questions about the plugin's usage, have a look at the [FAQ](./faq.html) and contact the
[user mailing list](./mailing-lists.html). The posts to the mailing list are archived and could already contain the
answer to your question as part of an older thread.

If the plugin is missing a feature or has a defect, file a feature request or bug report in our
[issue tracker](./issue-management.html). Especially for bugs, the developers need to be able to reproduce your
problem, so a small demo project or a debug log (`mvn -X`) helps a lot. Pull requests are welcome, too: contributors
can check out the project from our [source repository](./scm.html) and find more information in the
[guide to helping with Maven](https://maven.apache.org/guides/development/guide-helping.html).

## Migration to 3.x

### changes.xml - schema changes

- Update the schema in your `changes.xml` to `2.0.0`; see [Using the XML Schema](./using-changes-xsd.html).
- The element `action/dueto` was removed. Put its data into the `due-to` and `due-to-email` attributes of the
  `action` element, which accept comma-separated lists.
- The element `action/fixedIssues` was changed to the attribute `fixed-issues` of the `action` element, which also
  accepts a comma-separated list.

### Report goals and output names

The report goals and their output file names were renamed in line with the other reporting plugins from
`org.apache.maven.plugins`. The old goal names still work but are deprecated.

|Old goal name|New goal name|Old output name|New output name|
|:---|:---|:---|:---|
|changes-report|changes|changes-report.html|changes.html|
|github-report|github-changes|github-report.html|github-changes.html|
|jira-report|jira-changes|jira-report.html|jira-changes.html|
|trac-report|trac-changes|trac-report.html|trac-changes.html|

### Deprecate Trac integration

The **Trac** integration is prepared for removal in the next major version due to lack of maintainers. The
`trac-changes` goal is deprecated already.

## Examples

To provide you with better understanding of some usages of the Changes Plugin, you can take a look at the following
examples:

- [Alternate Location for the `changes.xml` File](./examples/alternate-changes-xml-location.html)
- [Check Your `changes.xml` File](./examples/check-changes-file.html)
- [Validate Your `changes.xml` File](./examples/changes-file-validation.html)
- [Configuring the GitHub Report](./examples/configuring-github-report.html)
- [Customizing the JIRA Report](./examples/customizing-jira-report.html)
- [Configuring the Trac Report](./examples/configuring-trac-report.html)
- [Include an Announcement File in Your Packaging](./examples/include-announcement-file.html)
- [Using a Custom Announcement Template](./examples/using-a-custom-announcement-template.html)
- [Specifying the Mail Sender](./examples/specifying-mail-sender.html)
- [SMTP Authentication](./examples/smtp-authentication.html)
