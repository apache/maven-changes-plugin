
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
String text = new File( basedir, "target/announcement/values.vm" ).text
assert text.contains( "groupId=org.apache.maven.plugins.changes.it" )
assert text.contains( "artifactId=announce-generate-project-values" )
assert text.contains( "version=1.1" )
assert text.contains( "packaging=pom" )
assert text.contains( "url=https://example.org/values" )
assert text.contains( "introduction=Project description." )
assert text.contains( "team=Values Project team" )
assert text.contains( "finalName=announce-generate-project-values-1.1" )
assert text.contains( "download=https://example.org/download" )
assert text.contains( "projectName=Values Project" )
assert text.contains( "projectArtifactId=announce-generate-project-values" )
assert text.contains( "param=hello" )
assert text.contains( "release=1.1" )
