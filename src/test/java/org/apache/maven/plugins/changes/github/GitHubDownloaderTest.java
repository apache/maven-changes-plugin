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
package org.apache.maven.plugins.changes.github;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.IssueManagement;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.changes.issues.Issue;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsProblem;
import org.apache.maven.settings.building.SettingsProblem.Severity;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHUser;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GitHubDownloaderTest {

    @Test
    public void testCreateIssue() throws IOException {
        IssueManagement issueManagement = newGitHubIssueManagement();
        GitHubDownloader gitHubDownloader = newGitHubDownloader(issueManagement);

        GHIssue githubIssue = mock(GHIssue.class);
        when(githubIssue.getNumber()).thenReturn(1);
        when(githubIssue.getTitle()).thenReturn("Title");
        when(githubIssue.getBody()).thenReturn("Body");
        when(githubIssue.getUser()).thenReturn(new GHUser());
        when(githubIssue.getState()).thenReturn(GHIssueState.OPEN);

        Issue issue = gitHubDownloader.createIssue(githubIssue);

        assertEquals(Integer.toString(githubIssue.getNumber()), issue.getId());
        assertEquals(Integer.toString(githubIssue.getNumber()), issue.getKey());
        assertEquals(githubIssue.getTitle(), issue.getSummary());
        assertEquals(githubIssue.getState().name(), issue.getStatus());
        assertEquals(issueManagement.getUrl() + githubIssue.getNumber(), issue.getLink());
    }

    @Test
    public void testConfigureAuthenticationWithProblems() throws Exception {
        IssueManagement issueManagement = newGitHubIssueManagement();
        GitHubDownloader gitHubDownloader = newGitHubDownloader(issueManagement);
        Settings settings = new Settings();
        Server server = newServer("github-server");
        settings.addServer(server);
        SettingsDecrypter decrypter = mock(SettingsDecrypter.class);
        SettingsDecryptionResult result = mock(SettingsDecryptionResult.class);
        Log log = mock(Log.class);
        when(result.getProblems())
                .thenReturn(Collections.singletonList(
                        new DefaultSettingsProblem("Ups " + server.getId(), Severity.ERROR, null, -1, -1, null)));
        when(result.getServer()).thenReturn(server);
        when(decrypter.decrypt(any(SettingsDecryptionRequest.class))).thenReturn(result);

        gitHubDownloader.configureAuthentication(decrypter, "github-server", settings, log);

        verify(log).error("Ups github-server", null);
        ArgumentCaptor<SettingsDecryptionRequest> argument = ArgumentCaptor.forClass(SettingsDecryptionRequest.class);
        verify(decrypter).decrypt(argument.capture());
        List<Server> servers = argument.getValue().getServers();
        assertEquals(1, servers.size());
        assertSame(server, servers.get(0));
    }

    @Test
    public void testConfigureAuthenticationWithNoServer() throws Exception {
        IssueManagement issueManagement = newGitHubIssueManagement();
        GitHubDownloader gitHubDownloader = newGitHubDownloader(issueManagement);
        Settings settings = new Settings();
        Server server = newServer("not-the-right-one");
        settings.addServer(server);
        SettingsDecrypter decrypter = mock(SettingsDecrypter.class);
        SettingsDecryptionResult result = mock(SettingsDecryptionResult.class);
        Log log = mock(Log.class);
        when(result.getProblems()).thenReturn(Collections.emptyList());
        when(result.getServer()).thenReturn(server);
        when(decrypter.decrypt(new DefaultSettingsDecryptionRequest(server))).thenReturn(result);

        gitHubDownloader.configureAuthentication(decrypter, "github-server", settings, log);

        verify(log).warn("Can't find server id [github-server] configured in settings.xml");
    }

    private Server newServer(String id) {
        Server server = new Server();
        server.setId(id);
        server.setUsername("some-user");
        server.setPassword("Sup3rSecret");
        return server;
    }

    private GitHubDownloader newGitHubDownloader(IssueManagement issueManagement) throws IOException {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setIssueManagement(issueManagement);
        return new GitHubDownloader(mavenProject, true, false);
    }

    private IssueManagement newGitHubIssueManagement() {
        IssueManagement issueManagement = new IssueManagement();
        issueManagement.setSystem("GitHub");
        issueManagement.setUrl("https://github.com/dadoonet/spring-elasticsearch/issues/");
        return issueManagement;
    }
}
