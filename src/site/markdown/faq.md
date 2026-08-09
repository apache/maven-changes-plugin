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
2. [I get a `org.xml.sax.SAXParseException`, when generating the JIRA Report. What can do about it?](#question2)

<a id="question1"></a>

### What's the difference between the changes-maven-plugin at Mojo and this one?

This plugin started out at [the Mojo project](http://mojo.codehaus.org/). In March of 2006 the plugin was moved
to Apache and the Maven sandbox. So it's the same plugin, but this one is newer.

<a id="question2"></a>

### I get a `org.xml.sax.SAXParseException`, when generating the JIRA Report. What can do about it?

If you have a lot of entities in the xml file returned from your JIRA installation, you might get an error like
this one, when you run the JIRA Report:

```
org.xml.sax.SAXParseException: Parser has reached the entity expansion limit "64,000" set by the Application.
```

If that happens you need to tell the xml parser to use a higher limit. This can be accomplished by adding a
command line parameter. In the following example we have set it to double the original value:

```
mvn -DentityExpansionLimit=128000 ...
```

Unfortunately we have not been able to set this in Java. If someone knows how to do this, then please reopen
[MCHANGES-75](https://issues.apache.org/jira/browse/MCHANGES-75), and tell us how. It would be nicer if this
could be set using a parameter in the POM.
