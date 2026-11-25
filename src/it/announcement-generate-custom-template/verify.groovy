
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

import java.nio.file.*;

File report = new File( basedir, "target/announcement/test-announce.vm" )
assert report.exists() : "report file is missing."
assert !report.isDirectory() : "report file is a directory."
String reportContent = new String(Files.readAllBytes( report.toPath() ), "UTF-8")

assert reportContent.contains( "maven-changes-plugin-test-1.1 release" ) : "test-announce.vm doesn't contain 'maven-changes-plugin-test-1.1 release'"

assert reportContent.contains( "Test Announcement Custom template" ) : "Test Announcement Custom template"

assert !reportContent.contains( '$date.systemDate' ) : '$date.systemDate not resolved'
