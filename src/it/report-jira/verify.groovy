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

import com.github.tomakehurst.wiremock.WireMockServer

WireMockServer wireMockServer = context.get("wireMockServer")
wireMockServer.stop()

content = new File(basedir, 'target/site/jira-changes.html').text;

assert content.contains('/browse/TEST_PROJECT-1">TEST_PROJECT-1</a>');
assert content.contains('<td>Authentication does not work after Upgrade</td>');
assert content.contains('<td>Closed</td>');
assert content.contains('<td>Fixed</td>');
assert content.contains('<td>Assigned User</td>');
