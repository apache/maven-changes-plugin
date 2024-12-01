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
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor

WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.options()
        .dynamicPort()
        .notifier(new ConsoleNotifier(false)))
wireMockServer.start()

def userProperties = context.get('userProperties')
userProperties.put('mockServerUrl', wireMockServer.baseUrl())

configureFor(wireMockServer.port())

stubFor(get('/repos/owner-name/repo-name')
        .withHeader('accept', equalTo('application/vnd.github+json'))
        .withHeader('authorization', equalTo('Bearer github-token'))
        .willReturn(aResponse().withStatus(200).withBody('''
{
    "full_name": "owner-name/repo-name"
}
''')))

stubFor(get('/repos/owner-name/repo-name/issues?state=open')
        .withHeader('accept', equalTo('application/vnd.github+json'))
        .withHeader('authorization', equalTo('Bearer github-token'))
        .willReturn(aResponse().withStatus(200).withBody('''
[
    {
        "number": 1234,
        "title": "Authentication does not work after Upgrade",
        "created_at": "2024-09-22T07:34:16Z",
        "updated_at": "2024-11-04T06:12:17Z",
        "closed_at": null,
        "user": {
            "login": "reporter-user1"        
        },
        "assignee": {
            "login": "assigned-user1"
        },
        "milestone": {
            "title": "2.12.1"
        },
        "state": "open"
    }
]
''')))

stubFor(get('/repos/owner-name/repo-name/issues?state=closed')
        .withHeader('accept', equalTo('application/vnd.github+json'))
        .withHeader('authorization', equalTo('Bearer github-token'))
        .willReturn(aResponse().withStatus(200).withBody('''
[
    {
        "number": 1235,
        "title": "Next issue for testing",
        "created_at": "2024-09-22T07:34:16Z",
        "updated_at": "2023-11-04T06:12:17Z",
        "closed_at": "2024-12-01T06:12:17Z",
        "user": {
            "login": "reporter-user2"        
        },
        "assignee": {
            "login": "assigned-user2"
        },
        "milestone": {
            "title": "2.12.2"
        },
        "state": "closed"
    }
]
''')))

context.put("wireMockServer", wireMockServer)
true

