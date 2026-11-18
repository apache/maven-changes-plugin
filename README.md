<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# Apache Maven Changes Plugin

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.maven.plugins/maven-changes-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.apache.maven.plugins/maven-changes-plugin)
[![Verify](https://github.com/apache/maven-changes-plugin/actions/workflows/maven-verify.yml/badge.svg)](https://github.com/apache/maven-changes-plugin/actions/workflows/maven-verify.yml)

Tells your users what changed between releases. The plugin reads the changes from a `changes.xml` file in the
project or from an issue tracker (JIRA, GitHub or Trac), renders them as site reports, and generates a release
announcement that it can also send by email.

```shell
mvn changes:changes                 # report from src/changes/changes.xml
mvn changes:github-changes          # report from the GitHub issues in <issueManagement>
mvn changes:changes-validate        # validate changes.xml against its schema
mvn changes:announcement-generate   # write target/announcement/announcement.vm
```

The plugin requires Maven 3.9.0 or later and Java 8 or later.

- Documentation: <https://maven.apache.org/plugins/maven-changes-plugin/>
- Issues: <https://github.com/apache/maven-changes-plugin/issues>
- Mailing lists: <https://maven.apache.org/mailing-lists.html>

## Build

```shell
mvn verify
```

Integration tests run with the `run-its` profile. The JIRA, GitHub and mail tests use local mock servers:

```shell
mvn -Prun-its verify
```

Build the site with the sample reports:

```shell
mvn install -DskipTests
mvn site -Preporting
```

## Contributing

Open an issue or a pull request on GitHub. Before you push, run `mvn spotless:apply` and `mvn -Prun-its verify`. Larger
changes are best discussed on the [developer mailing list](https://maven.apache.org/mailing-lists.html) first. See
the [guide to helping with Maven](https://maven.apache.org/guides/development/guide-helping.html) for more.
