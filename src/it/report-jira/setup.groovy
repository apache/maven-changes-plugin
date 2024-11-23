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
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.post
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor

WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.options()
        .dynamicPort()
        .notifier(new ConsoleNotifier(false)))
wireMockServer.start()

def userProperties = context.get('userProperties')
userProperties.put('mockServerUrl', wireMockServer.baseUrl())

configureFor(wireMockServer.port())

stubFor(get('/rest/api/2/serverInfo')
        .withHeader('accept', equalTo('application/json'))
        .willReturn(aResponse().withStatus(200)))

stubFor(post('/rest/auth/1/session')
        .withHeader('accept', equalTo('application/json'))
        .withHeader('content-type', equalTo('application/json'))
        .withRequestBody(equalToJson('{"username" : "jira-test",  "password" : "jira-password"}'))
        .willReturn(aResponse()
                .withStatus(200)
        .withHeader('Set-Cookie', 'JSESSIONID=D6961281D4A029FFA4E6BC8A526CF5CC; Path=/; HttpOnly')))

stubFor(get('/rest/api/2/status')
        .withHeader('accept', equalTo('application/json'))
        .withCookie('JSESSIONID', equalTo('D6961281D4A029FFA4E6BC8A526CF5CC'))
        .willReturn(aResponse().withStatus(200)
                .withBody('[{"name": "Closed","id": "6"}]')))

stubFor(get('/rest/api/2/resolution')
        .withHeader('accept', equalTo('application/json'))
        .withCookie('JSESSIONID', equalTo('D6961281D4A029FFA4E6BC8A526CF5CC'))
        .willReturn(aResponse().withStatus(200)
                .withBody('[{"name": "Fixed","id": "1"}]')))

stubFor(post('/rest/api/2/search')
        .withHeader('accept', equalTo('application/json'))
        .withHeader('content-type', equalTo('application/json'))
        .withCookie('JSESSIONID', equalTo('D6961281D4A029FFA4E6BC8A526CF5CC'))
        .withRequestBody(equalToJson('''
          {"jql":"project = TEST_PROJECT AND status in (6) AND resolution in (1) ORDER BY priority DESC, created DESC",
            "maxResults":100, "fields":["*all"]}
'''))
        .willReturn(aResponse().withStatus(200)
                .withBody('''
{
    "issues": [
    {
        "id": "13036790",
        "key": "TEST_PROJECT-1",
        "fields": {
            "assignee": {
                "name": "assigned-user",
                "displayName": "Assigned User"
            },
            "created": "2024-11-21T12:10:35.000+0000",
            "updated": "2024-11-22T20:05:52.000+0000",
            "components": [
                { "name": "jira" }
            ],
            "fixVersions": [
                { "name": "2.1" },
                { "name": "3.1" }
            ],
            "issuetype": {
                "name": "Bug"
            },
            "priority": {
                "name": "Bug"
            },
            "reporter": {
                "name": "reporter-user",
                "displayName": "~Reporter User"
            },
            "resolution": {
                "name": "Fixed"
            },
            "status": {
                "name": "Closed"
            },
            "summary": "Authentication does not work after Upgrade",
            "versions": [
                { "name": "2.12.1" }
            ]
        }
    }]
}
''')))

context.put("wireMockServer", wireMockServer)
true

