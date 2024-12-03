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

content = new File(basedir, 'target/site/github-changes.html').text;

assert content.contains('/owner-name/repo-name/issues/1234">1234</a>');
assert content.contains('<td>Authentication does not work after Upgrade</td>');
assert content.contains('<td>OPEN</td>');
assert content.contains('<td>assigned-user1</td>')
assert content.contains('<td>reporter-user1</td>');
assert content.contains('<td>2.12.1</td>');

assert content.contains('/owner-name/repo-name/issues/1235">1235</a>');
assert content.contains('<td>Next issue for testing</td>');
assert content.contains('<td>CLOSED</td>');
assert content.contains('<td>assigned-user2</td>')
assert content.contains('<td>reporter-user2</td>');
assert content.contains('<td>2.12.2</td>');
