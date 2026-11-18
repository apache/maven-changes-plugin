---
title: Frequently Asked Questions
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

<a id="top"></a>

# Frequently Asked Questions

1. [What's the difference between the changes-maven-plugin at Mojo and this one?](#question1)
2. [I get a `org.xml.sax.SAXParseException` when generating the JIRA Report. What can I do about it?](#question2)
3. [Why does the Changes Report link to the wrong issue URL?](#question3)

<a id="question1"></a>

### What's the difference between the changes-maven-plugin at Mojo and this one?

This plugin started out at the Mojo project (now [MojoHaus](https://www.mojohaus.org/)). In March of 2006 the plugin
was moved to Apache. So it's the same plugin, and this one is the maintained version.

<a id="question2"></a>

### I get a `org.xml.sax.SAXParseException` when generating the JIRA Report. What can I do about it?

Old versions of the plugin downloaded issues as XML from JIRA and could hit the parser's entity expansion limit.
The plugin now reads issues through the JIRA REST API, which returns JSON, so this error no longer occurs. Upgrade to
the latest version of the plugin.

<a id="question3"></a>

### Why does the Changes Report link to the wrong issue URL?

The link is built from `<issueManagement>/<url>` with the last `/` and everything after it removed. For GitHub, end
the URL with a slash (`https://github.com/example/project/issues/`). See
[Linking to Your Issue Management System](./usage.html#Linking_to_Your_Issue_Management_System).
