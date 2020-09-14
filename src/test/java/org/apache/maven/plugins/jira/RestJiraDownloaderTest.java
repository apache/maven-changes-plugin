package org.apache.maven.plugins.jira;

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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.plugins.issues.Issue;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.*;

public class RestJiraDownloaderTest
{
    private static final int PORT = 3033;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(3033);

    @Test
    public void getIssues() throws Exception
    {
        stubFor(
            get(urlEqualTo("/rest/api/2/serverInfo"))
                .willReturn(
                    aResponse().withHeader("Content-Type", APPLICATION_JSON)
                )
        );
        stubFor(
            post(urlEqualTo("/rest/api/2/search"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody("{\"issues\": [{\"id\": \"some-id\", \"key\": null, \"fields\": { \"resolution\": null } }]}")
                )
        );

        RestJiraDownloader downloader = getDownloader();
        downloader.doExecute();

        List<Issue> issueList = downloader.getIssueList();
        assertEquals( 1, issueList.size() );
        Issue issue = issueList.get( 0 );
        assertEquals( "some-id", issue.getId() );
    }

    private static RestJiraDownloader getDownloader()
    {
        IssueManagement issueManagement = new IssueManagement();
        issueManagement.setUrl( "http://localhost:" + PORT + "/browse/" );

        MavenProject mavenProject = new MavenProject();
        mavenProject.setIssueManagement( issueManagement );

        RestJiraDownloader downloader = new RestJiraDownloader();
        downloader.setMavenProject( mavenProject );
        downloader.setLog( new SilentLog() );

        return downloader;
    }
}