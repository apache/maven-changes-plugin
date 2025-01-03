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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.changes.issues.Issue;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GitHubBuilder;

/**
 * @since 2.8
 */
public class GitHubDownloader {

    /**
     * The github client.
     */
    private GitHubBuilder client;

    /**
     * A boolean to indicate if we should include open issues as well
     */
    private boolean includeOpenIssues;

    /**
     * A boolean to indicate if we should only include issues with milestones
     */
    private boolean onlyMilestoneIssues;

    /**
     * The owner/organization of the github repo.
     */
    private String githubOwner;

    /**
     * The name of the github repo.
     */
    private String githubRepo;

    /**
     * The url to the github repo's issue management
     */
    private String githubIssueURL;

    public GitHubDownloader(MavenProject project, boolean includeOpenIssues, boolean onlyMilestoneIssues)
            throws IOException {
        this.includeOpenIssues = includeOpenIssues;
        this.onlyMilestoneIssues = onlyMilestoneIssues;

        URL githubURL = new URL(project.getIssueManagement().getUrl());

        // The githubclient prefers to connect to 'github.com' using the api domain, unlike github enterprise
        // which can connect fine using its domain, so for github.com use empty constructor
        if (githubURL.getHost().equalsIgnoreCase("github.com")) {
            this.client = new GitHubBuilder();
        } else {
            this.client = new GitHubBuilder()
                    .withEndpoint(githubURL.getProtocol() + "://" + githubURL.getHost()
                            + (githubURL.getPort() == -1 ? "" : ":" + githubURL.getPort()));
        }

        this.githubIssueURL = project.getIssueManagement().getUrl();
        if (!this.githubIssueURL.endsWith("/")) {
            this.githubIssueURL = this.githubIssueURL + "/";
        }

        String urlPath = githubURL.getPath();
        if (urlPath.startsWith("/")) {
            urlPath = urlPath.substring(1);
        }

        if (urlPath.endsWith("/")) {
            urlPath = urlPath.substring(0, urlPath.length() - 2);
        }

        String[] urlPathParts = urlPath.split("/");

        if (urlPathParts.length != 3) {
            throw new MalformedURLException(
                    "GitHub issue management URL must look like, " + "[GITHUB_DOMAIN]/[OWNER]/[REPO]/issues");
        }

        this.githubOwner = urlPathParts[0];
        this.githubRepo = urlPathParts[1];
    }

    protected Issue createIssue(GHIssue githubIssue) throws IOException {
        Issue issue = new Issue();

        issue.setKey(String.valueOf(githubIssue.getNumber()));
        issue.setId(String.valueOf(githubIssue.getNumber()));

        issue.setLink(this.githubIssueURL + githubIssue.getNumber());

        issue.setCreated(githubIssue.getCreatedAt());

        issue.setUpdated(githubIssue.getUpdatedAt());

        if (githubIssue.getAssignee() != null) {
            if (githubIssue.getAssignee().getName() != null) {
                issue.setAssignee(githubIssue.getAssignee().getName());
            } else {
                issue.setAssignee(githubIssue.getAssignee().getLogin());
            }
        }

        issue.setSummary(githubIssue.getTitle());

        if (githubIssue.getMilestone() != null) {
            issue.addFixVersion(githubIssue.getMilestone().getTitle());
        }

        issue.setReporter(githubIssue.getUser().getLogin());

        issue.setStatus(githubIssue.getState().name());

        final Collection<GHLabel> labels = githubIssue.getLabels();
        if (labels != null && !labels.isEmpty()) {
            issue.setType(labels.stream().findAny().get().getName());
        }

        return issue;
    }

    public List<Issue> getIssueList() throws IOException {
        List<Issue> issueList = new ArrayList<>();

        if (includeOpenIssues) {
            final List<GHIssue> openIssues =
                    client.build().getRepository(githubOwner + "/" + githubRepo).getIssues(GHIssueState.OPEN);
            for (final GHIssue issue : openIssues) {
                if (!onlyMilestoneIssues || issue.getMilestone() != null) {
                    issueList.add(createIssue(issue));
                }
            }
        }

        final List<GHIssue> closedIssues =
                client.build().getRepository(githubOwner + "/" + githubRepo).getIssues(GHIssueState.CLOSED);

        for (final GHIssue issue : closedIssues) {
            if (!onlyMilestoneIssues || issue.getMilestone() != null) {
                issueList.add(createIssue(issue));
            }
        }

        return issueList;
    }

    public void configureAuthentication(
            SettingsDecrypter decrypter, String githubAPIServerId, Settings settings, Log log) {
        boolean configured = false;

        List<Server> servers = settings.getServers();

        for (Server server : servers) {
            if (server.getId().equals(githubAPIServerId)) {
                SettingsDecryptionResult result = decrypter.decrypt(new DefaultSettingsDecryptionRequest(server));
                for (SettingsProblem problem : result.getProblems()) {
                    log.error(problem.getMessage(), problem.getException());
                }
                server = result.getServer();
                String password = server.getPassword();
                client.withJwtToken(password);

                configured = true;
                break;
            }
        }

        if (!configured) {
            log.warn("Can't find server id [" + githubAPIServerId + "] configured in settings.xml");
        }
    }
}
