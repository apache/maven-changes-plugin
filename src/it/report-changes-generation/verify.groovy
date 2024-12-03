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


def report = new File(basedir, 'target/site/changes.html')
assert report.exists()

def content = report.text

assert content.contains('Changes'): 'changes.html doesn\'t contain Changes title'

assert content.contains('href="http://myjira/browse/MCHANGES-88"'): 'changes.html doesn\'t contain jira issue link'

// Test for output problem caused by only using <dueTo> elements
assert !content.contains('Thanks to , '): 'changes.html has too many dueTos in the Map'

// Tests output problems caused by only using fixedIssues attribute
assert content.contains('bug-12345'): 'changes.html doesn\'t contain issue text for issue specified with <fixes> element'
assert !content.contains('Fixes .'): 'changes.html doesn\'t handle empty fixes attribute properly'

// due-to verification
assert content.contains('<a class="externalLink" href="mailto:john@doe.com">John Doe</a>')
assert content.contains('Thanks to External Submitter,')
assert content.contains('<a class="externalLink" href="mailto:others@users.com">others</a>')
