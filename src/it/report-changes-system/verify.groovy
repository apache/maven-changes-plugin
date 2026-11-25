
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.nio.file.*

File report = new File(basedir, "target/site/en/changes.html")
assert report.exists() : "report file is missing."
assert !report.isDirectory() : "report file is a directory."
String reportContent = new String(Files.readAllBytes( report.toPath() ), "UTF-8")

assert reportContent.contains( "Changes" ) : "changes.html doesn't contain Changes title"

assert reportContent.contains( "href=\"http://myjira/browse/MCHANGES-88\"" ) : "changes.html doesn't contain jira issue link"
assert reportContent.contains( "href=\"http://localhost/1212\"" ) : "changes.html doesn't contain qc issue link"

assert reportContent.contains( "2008-01-01" ) : "changes.html doesn't contain date 2008-01-01"
