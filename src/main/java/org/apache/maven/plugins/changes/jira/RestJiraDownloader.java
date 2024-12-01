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
package org.apache.maven.plugins.changes.jira;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.changes.issues.Issue;
import org.apache.maven.plugins.changes.issues.IssueUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;

/**
 * Use the JIRA REST API to download issues. This class assumes that the URL points to a copy of JIRA that
 * implements the REST API.
 */
public class RestJiraDownloader {

    /** Log for debug output. */
    private Log log;

    /** The maven project. */
    private MavenProject project;

    /** The maven settings. */
    private Settings settings;

    private SettingsDecrypter settingsDecrypter;

    /** Filter the JIRA query based on the current version */
    private boolean onlyCurrentVersion;

    /** The versionPrefix to apply to the POM version */
    protected String versionPrefix;

    /** The maximum number of entries to show. */
    protected int nbEntriesMax;

    /** The filter to apply to query to JIRA. */
    protected String filter;

    /** Ids of fix versions to show, as comma separated string. */
    protected String fixVersionIds;

    /** Ids of status to show, as comma separated string. */
    protected String statusIds;

    /** Ids of resolution to show, as comma separated string. */
    protected String resolutionIds;

    /** Ids of priority to show, as comma separated string. */
    protected String priorityIds;

    /** The component to show. */
    protected String component;

    /** Ids of types to show, as comma separated string. */
    protected String typeIds;

    /** Column names to sort by, as comma separated string. */
    protected String sortColumnNames;

    /** The username to log into JIRA. */
    protected String jiraUser;

    /** The password to log into JIRA. */
    protected String jiraPassword;

    private String jiraServerId;

    /** The pattern used to parse dates from the JIRA xml file. */
    protected String jiraDatePattern;

    protected int connectionTimeout;

    protected int receiveTimout;

    private List<Issue> issueList;

    private JsonFactory jsonFactory;

    private SimpleDateFormat dateFormat;

    private List<String> resolvedFixVersionIds;

    private List<String> resolvedStatusIds;

    private List<String> resolvedComponentIds;

    private List<String> resolvedTypeIds;

    private List<String> resolvedResolutionIds;

    private List<String> resolvedPriorityIds;

    /**
     * Override this method if you need to get issues for a specific Fix For.
     *
     * @return a Fix For id or <code>null</code> if you don't have that need
     */
    protected String getFixFor() {
        if (onlyCurrentVersion) {
            // Let JIRA do the filtering of the current version instead of the JIRA mojo.
            // This way JIRA returns less issues and we do not run into the "nbEntriesMax" limit that easily.

            String version = (versionPrefix == null ? "" : versionPrefix) + project.getVersion();

            // Remove "-SNAPSHOT" from the end of the version, if it's there
            if (version.endsWith(IssueUtils.SNAPSHOT_SUFFIX)) {
                return version.substring(0, version.length() - IssueUtils.SNAPSHOT_SUFFIX.length());
            } else {
                return version;
            }
        } else {
            return null;
        }
    }

    /**
     * Sets the project.
     *
     * @param thisProject The project to set
     */
    public void setMavenProject(MavenProject thisProject) {
        this.project = thisProject;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    protected Log getLog() {
        return log;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public void setSettingsDecrypter(SettingsDecrypter settingsDecrypter) {
        this.settingsDecrypter = settingsDecrypter;
    }

    public void setOnlyCurrentVersion(boolean onlyCurrentVersion) {
        this.onlyCurrentVersion = onlyCurrentVersion;
    }

    public void setVersionPrefix(String versionPrefix) {
        this.versionPrefix = versionPrefix;
    }

    public void setJiraDatePattern(String jiraDatePattern) {
        this.jiraDatePattern = jiraDatePattern;
    }

    /**
     * Sets the maximum number of Issues to show.
     *
     * @param nbEntries The maximum number of Issues
     */
    public void setNbEntries(final int nbEntries) {
        nbEntriesMax = nbEntries;
    }

    /**
     * Sets the statusIds.
     *
     * @param thisStatusIds The id(s) of the status to show, as comma separated string
     */
    public void setStatusIds(String thisStatusIds) {
        statusIds = thisStatusIds;
    }

    /**
     * Sets the priorityIds.
     *
     * @param thisPriorityIds The id(s) of the priority to show, as comma separated string
     */
    public void setPriorityIds(String thisPriorityIds) {
        priorityIds = thisPriorityIds;
    }

    /**
     * Sets the resolutionIds.
     *
     * @param thisResolutionIds The id(s) of the resolution to show, as comma separated string
     */
    public void setResolutionIds(String thisResolutionIds) {
        resolutionIds = thisResolutionIds;
    }

    /**
     * Sets the sort column names.
     *
     * @param thisSortColumnNames The column names to sort by
     */
    public void setSortColumnNames(String thisSortColumnNames) {
        sortColumnNames = thisSortColumnNames;
    }

    /**
     * Sets the password to log into a secured JIRA.
     *
     * @param thisJiraPassword The password for JIRA
     */
    public void setJiraPassword(final String thisJiraPassword) {
        this.jiraPassword = thisJiraPassword;
    }

    /**
     * Sets the username to log into a secured JIRA.
     *
     * @param thisJiraUser The username for JIRA
     */
    public void setJiraUser(String thisJiraUser) {
        this.jiraUser = thisJiraUser;
    }

    public void setJiraServerId(String jiraServerId) {
        this.jiraServerId = jiraServerId;
    }

    /**
     * Sets the filter to apply to query to JIRA.
     *
     * @param thisFilter The filter to query JIRA
     */
    public void setFilter(String thisFilter) {
        this.filter = thisFilter;
    }

    /**
     * Sets the component(s) to apply to query JIRA.
     *
     * @param theseComponents The id(s) of components to show, as comma separated string
     */
    public void setComponent(String theseComponents) {
        this.component = theseComponents;
    }

    /**
     * Sets the fix version id(s) to apply to query JIRA.
     *
     * @param theseFixVersionIds The id(s) of fix versions to show, as comma separated string
     */
    public void setFixVersionIds(String theseFixVersionIds) {
        this.fixVersionIds = theseFixVersionIds;
    }

    /**
     * Sets the typeIds.
     *
     * @param theseTypeIds The id(s) of the types to show, as comma separated string
     */
    public void setTypeIds(String theseTypeIds) {
        typeIds = theseTypeIds;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setReceiveTimout(int receiveTimout) {
        this.receiveTimout = receiveTimout;
    }

    public static class NoRest extends Exception {
        private static final long serialVersionUID = 6970088805270319624L;

        public NoRest() {
            // blank on purpose.
        }

        public NoRest(String message) {
            super(message);
        }
    }

    public RestJiraDownloader() {
        jsonFactory = new MappingJsonFactory();
        // 2012-07-17T06:26:47.723-0500
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        resolvedFixVersionIds = new ArrayList<>();
        resolvedStatusIds = new ArrayList<>();
        resolvedComponentIds = new ArrayList<>();
        resolvedTypeIds = new ArrayList<>();
        resolvedResolutionIds = new ArrayList<>();
        resolvedPriorityIds = new ArrayList<>();
    }

    public void doExecute() throws Exception {

        Map<String, String> urlMap =
                JiraHelper.getJiraUrlAndProjectName(project.getIssueManagement().getUrl());
        String jiraUrl = urlMap.get("url");
        String jiraProject = urlMap.get("project");

        try (CloseableHttpClient client = setupHttpClient(jiraUrl)) {
            checkRestApi(client, jiraUrl);
            doSessionAuth(client, jiraUrl);
            resolveIds(client, jiraUrl, jiraProject);
            search(client, jiraProject, jiraUrl);
        }
    }

    private void search(CloseableHttpClient client, String jiraProject, String jiraUrl)
            throws IOException, MojoExecutionException {
        String jqlQuery = new JqlQueryBuilder(log)
                .urlEncode(false)
                .project(jiraProject)
                .fixVersion(getFixFor())
                .fixVersionIds(resolvedFixVersionIds)
                .statusIds(resolvedStatusIds)
                .priorityIds(resolvedPriorityIds)
                .resolutionIds(resolvedResolutionIds)
                .components(resolvedComponentIds)
                .typeIds(resolvedTypeIds)
                .sortColumnNames(sortColumnNames)
                .filter(filter)
                .build();

        log.debug("JIRA jql=" + jqlQuery);

        StringWriter searchParamStringWriter = new StringWriter();
        try (JsonGenerator gen = jsonFactory.createGenerator(searchParamStringWriter)) {
            gen.writeStartObject();
            gen.writeStringField("jql", jqlQuery);
            gen.writeNumberField("maxResults", nbEntriesMax);
            gen.writeArrayFieldStart("fields");
            // Retrieve all fields. If that seems slow, we can reconsider.
            gen.writeString("*all");
            gen.writeEndArray();
            gen.writeEndObject();
        }

        HttpPost httpPost = new HttpPost(jiraUrl + "/rest/api/2/search");
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        httpPost.setEntity(new StringEntity(searchParamStringWriter.toString()));

        try (CloseableHttpResponse response = client.execute(httpPost)) {

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                reportErrors(response);
            }

            JsonNode issueTree = getResponseTree(response);

            assertIsObject(issueTree);
            JsonNode issuesNode = issueTree.get("issues");
            assertIsArray(issuesNode);
            buildIssues(issuesNode, jiraUrl);
        }
    }

    private void checkRestApi(CloseableHttpClient client, String jiraUrl) throws IOException, NoRest {
        // We use version 2 of the REST API, that first appeared in JIRA 5
        // Check if version 2 of the REST API is supported
        // http://docs.atlassian.com/jira/REST/5.0/
        // Note that serverInfo can always be accessed without authentication

        HttpGet httpGet = new HttpGet(jiraUrl + "/rest/api/2/serverInfo");
        try (CloseableHttpResponse response = client.execute(httpGet)) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new NoRest("This JIRA server does not support version 2 of the REST API, "
                        + "which maven-changes-plugin requires.");
            }
        }
    }

    private JsonNode getResponseTree(HttpResponse response) throws IOException {
        try (InputStream inputStream = response.getEntity().getContent();
                JsonParser jsonParser = jsonFactory.createParser(inputStream)) {
            return jsonParser.readValueAsTree();
        }
    }

    private void reportErrors(HttpResponse resp) throws IOException, MojoExecutionException {
        ContentType contentType = ContentType.get(resp.getEntity());
        if (contentType != null && contentType.getMimeType().equals(ContentType.APPLICATION_JSON.getMimeType())) {
            JsonNode errorTree = getResponseTree(resp);
            assertIsObject(errorTree);
            JsonNode messages = errorTree.get("errorMessages");
            if (messages != null) {
                for (int mx = 0; mx < messages.size(); mx++) {
                    getLog().error(messages.get(mx).asText());
                }
            } else {
                JsonNode message = errorTree.get("message");
                if (message != null) {
                    getLog().error(message.asText());
                }
            }
        }
        throw new MojoExecutionException(String.format(
                "Failed to query issues; response %d", resp.getStatusLine().getStatusCode()));
    }

    private void resolveIds(CloseableHttpClient client, String jiraUrl, String jiraProject)
            throws IOException, MojoExecutionException, MojoFailureException {
        resolveList(
                resolvedComponentIds,
                client,
                "components",
                component,
                jiraUrl + "/rest/api/2/project/" + jiraProject + "/components");
        resolveList(
                resolvedFixVersionIds,
                client,
                "fixVersions",
                fixVersionIds,
                jiraUrl + "/rest/api/2/project/" + jiraProject + "/versions");
        resolveList(resolvedStatusIds, client, "status", statusIds, jiraUrl + "/rest/api/2/status");
        resolveList(resolvedResolutionIds, client, "resolution", resolutionIds, jiraUrl + "/rest/api/2/resolution");
        resolveList(resolvedTypeIds, client, "type", typeIds, jiraUrl + "/rest/api/2/issuetype");
        resolveList(resolvedPriorityIds, client, "priority", priorityIds, jiraUrl + "/rest/api/2/priority");
    }

    private void resolveList(
            List<String> targetList, CloseableHttpClient client, String what, String input, String listRestUrlPattern)
            throws IOException, MojoExecutionException, MojoFailureException {
        if (input == null || input.isEmpty()) {
            return;
        }

        HttpGet httpGet = new HttpGet(listRestUrlPattern);

        try (CloseableHttpResponse response = client.execute(httpGet)) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                getLog().error(String.format("Could not get %s list from %s", what, listRestUrlPattern));
                reportErrors(response);
            }

            JsonNode items = getResponseTree(response);
            String[] pieces = input.split(",");
            for (String item : pieces) {
                targetList.add(resolveOneItem(items, what, item.trim()));
            }
        }
    }

    private String resolveOneItem(JsonNode items, String what, String nameOrId) throws MojoFailureException {
        for (int cx = 0; cx < items.size(); cx++) {
            JsonNode item = items.get(cx);
            if (nameOrId.equals(item.get("id").asText())) {
                return nameOrId;
            } else if (nameOrId.equals(item.get("name").asText())) {
                return item.get("id").asText();
            }
        }
        throw new MojoFailureException(String.format("Could not find %s %s.", what, nameOrId));
    }

    private void buildIssues(JsonNode issuesNode, String jiraUrl) {
        issueList = new ArrayList<>();
        for (int ix = 0; ix < issuesNode.size(); ix++) {
            JsonNode issueNode = issuesNode.get(ix);
            assertIsObject(issueNode);
            Issue issue = new Issue();
            JsonNode val;

            val = issueNode.get("id");
            if (val != null) {
                issue.setId(val.asText());
            }

            val = issueNode.get("key");
            if (val != null) {
                issue.setKey(val.asText());
                issue.setLink(String.format("%s/browse/%s", jiraUrl, val.asText()));
            }

            // much of what we want is in here.
            JsonNode fieldsNode = issueNode.get("fields");

            val = fieldsNode.get("assignee");
            processAssignee(issue, val);

            val = fieldsNode.get("created");
            processCreated(issue, val);

            val = fieldsNode.get("components");
            processComponents(issue, val);

            val = fieldsNode.get("fixVersions");
            processFixVersions(issue, val);

            val = fieldsNode.get("issuetype");
            processIssueType(issue, val);

            val = fieldsNode.get("priority");
            processPriority(issue, val);

            val = fieldsNode.get("reporter");
            processReporter(issue, val);

            val = fieldsNode.get("resolution");
            processResolution(issue, val);

            val = fieldsNode.get("status");
            processStatus(issue, val);

            val = fieldsNode.get("summary");
            if (val != null) {
                issue.setSummary(val.asText());
            }

            val = fieldsNode.get("updated");
            processUpdated(issue, val);

            val = fieldsNode.get("versions");
            processVersions(issue, val);

            issueList.add(issue);
        }
    }

    private void processVersions(Issue issue, JsonNode val) {
        StringBuilder sb = new StringBuilder();
        if (val != null) {
            for (int vx = 0; vx < val.size(); vx++) {
                sb.append(val.get(vx).get("name").asText());
                sb.append(", ");
            }
        }
        if (sb.length() > 0) {
            // remove last ", "
            issue.setVersion(sb.substring(0, sb.length() - 2));
        }
    }

    private void processStatus(Issue issue, JsonNode val) {
        if (val != null) {
            issue.setStatus(val.get("name").asText());
        }
    }

    private void processPriority(Issue issue, JsonNode val) {
        if (val != null) {
            issue.setPriority(val.get("name").asText());
        }
    }

    private void processResolution(Issue issue, JsonNode val) {
        if (val != null) {
            issue.setResolution(val.get("name").asText());
        }
    }

    private String getPerson(JsonNode val) {
        JsonNode nameNode = val.get("displayName");
        if (nameNode == null) {
            nameNode = val.get("name");
        }
        if (nameNode != null) {
            return nameNode.asText();
        } else {
            return null;
        }
    }

    private void processAssignee(Issue issue, JsonNode val) {
        if (val != null) {
            String text = getPerson(val);
            if (text != null) {
                issue.setAssignee(text);
            }
        }
    }

    private void processReporter(Issue issue, JsonNode val) {
        if (val != null) {
            String text = getPerson(val);
            if (text != null) {
                issue.setReporter(text);
            }
        }
    }

    private void processCreated(Issue issue, JsonNode val) {
        if (val != null) {
            try {
                issue.setCreated(parseDate(val));
            } catch (ParseException e) {
                getLog().warn("Invalid created date " + val.asText());
            }
        }
    }

    private void processUpdated(Issue issue, JsonNode val) {
        if (val != null) {
            try {
                issue.setUpdated(parseDate(val));
            } catch (ParseException e) {
                getLog().warn("Invalid updated date " + val.asText());
            }
        }
    }

    private Date parseDate(JsonNode val) throws ParseException {
        return dateFormat.parse(val.asText());
    }

    private void processFixVersions(Issue issue, JsonNode val) {
        if (val != null) {
            assertIsArray(val);
            for (int vx = 0; vx < val.size(); vx++) {
                JsonNode fvNode = val.get(vx);
                issue.addFixVersion(fvNode.get("name").asText());
            }
        }
    }

    private void processComponents(Issue issue, JsonNode val) {
        if (val != null) {
            assertIsArray(val);
            for (int cx = 0; cx < val.size(); cx++) {
                JsonNode cnode = val.get(cx);
                issue.addComponent(cnode.get("name").asText());
            }
        }
    }

    private void processIssueType(Issue issue, JsonNode val) {
        if (val != null) {
            issue.setType(val.get("name").asText());
        }
    }

    private void assertIsObject(JsonNode node) {
        if (!node.isObject()) {
            throw new IllegalArgumentException("json node: " + node + " is not an object");
        }
    }

    private void assertIsArray(JsonNode node) {
        if (!node.isArray()) {
            throw new IllegalArgumentException("json node: " + node + " is not an array");
        }
    }

    private void doSessionAuth(CloseableHttpClient client, String jiraUrl)
            throws IOException, MojoExecutionException, NoRest {

        Server server = settings.getServer(jiraServerId);
        if (server != null) {
            SettingsDecryptionResult result = settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(server));
            if (!result.getProblems().isEmpty()) {
                for (SettingsProblem problem : result.getProblems()) {
                    log.error(problem.getMessage());
                }
            } else {
                jiraUser = result.getServer().getUsername();
                jiraPassword = result.getServer().getPassword();
            }
        }

        if (jiraUser != null) {
            StringWriter jsWriter = new StringWriter();
            try (JsonGenerator gen = jsonFactory.createGenerator(jsWriter)) {
                gen.writeStartObject();
                gen.writeStringField("username", jiraUser);
                gen.writeStringField("password", jiraPassword);
                gen.writeEndObject();
            }

            HttpPost post = new HttpPost(jiraUrl + "/rest/auth/1/session");
            post.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            post.setEntity(new StringEntity(jsWriter.toString()));

            try (CloseableHttpResponse response = client.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK) {
                    if (statusCode != HttpStatus.SC_UNAUTHORIZED && statusCode != HttpStatus.SC_FORBIDDEN) {
                        // if not one of the documented failures, assume that there's no rest in there in the first
                        // place.
                        throw new NoRest();
                    }
                    throw new MojoExecutionException(String.format("Authentication failure status %d.", statusCode));
                }
            }
        }
    }

    private CloseableHttpClient setupHttpClient(String jiraUrl) {

        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setDefaultCookieStore(new BasicCookieStore())
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(receiveTimout)
                        .setConnectTimeout(connectionTimeout)
                        .build())
                .setDefaultHeaders(Collections.singletonList(new BasicHeader("Accept", "application/json")));

        Proxy proxy = getProxy(jiraUrl);
        if (proxy != null) {
            if (proxy.getUsername() != null && proxy.getPassword() != null) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(proxy.getHost(), proxy.getPort()),
                        new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword()));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
            httpClientBuilder.setProxy(new HttpHost(proxy.getHost(), proxy.getPort()));
        }
        return httpClientBuilder.build();
    }

    private Proxy getProxy(String jiraUrl) {
        Proxy proxy = settings.getActiveProxy();
        if (proxy != null) {
            SettingsDecryptionResult result = settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(proxy));
            if (!result.getProblems().isEmpty()) {
                for (SettingsProblem problem : result.getProblems()) {
                    log.error(problem.getMessage());
                }
            } else {
                proxy = result.getProxy();
            }
        }

        if (proxy != null && proxy.getNonProxyHosts() != null) {
            URI jiraUri = URI.create(jiraUrl);
            boolean nonProxy = Arrays.stream(proxy.getNonProxyHosts().split("\\|"))
                    .anyMatch(host -> !host.equalsIgnoreCase(jiraUri.getHost()));
            if (nonProxy) {
                return null;
            }
        }

        return proxy;
    }

    public List<Issue> getIssueList() {
        return issueList;
    }
}
