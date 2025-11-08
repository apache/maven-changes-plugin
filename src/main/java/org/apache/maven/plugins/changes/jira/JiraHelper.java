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
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Map;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.maven.plugin.logging.Log;

/**
 * A helper class with common JIRA related functionality.
 *
 * @author Dennis Lundberg
 * @version $Id$
 */
public class JiraHelper {
    private static final String PID = "?pid="; // MCHANGES-281 addd ?

    /**
     * Parse out the base URL for JIRA and the JIRA project id from the issue management URL.
     *
     * @param issueManagementUrl The URL to the issue management system
     * @return A <code>Map</code> containing the URL and project id
     */
    static Map<String, String> getJiraUrlAndProjectId(String issueManagementUrl) {
        String url = issueManagementUrl;

        if (url.endsWith("/")) {
            // MCHANGES-218
            url = url.substring(0, url.lastIndexOf('/'));
        }

        // chop off the parameter part
        int pos = url.indexOf('?');

        // and get the id while we're at it
        String id = "";

        if (pos >= 0) {
            // project id
            id = url.substring(url.lastIndexOf('=') + 1);
        }

        String jiraUrl = url.substring(0, url.lastIndexOf('/'));

        if (jiraUrl.endsWith("secure")) {
            jiraUrl = jiraUrl.substring(0, jiraUrl.lastIndexOf('/'));
        } else {
            // If the issueManagement.url points to a component, then "browse"
            // will not be at the end - it might be in the middle somewhere.
            // Try to find it.
            final int index = jiraUrl.indexOf("/browse");
            if (index != -1) {
                jiraUrl = jiraUrl.substring(0, index);
            }
        }

        HashMap<String, String> urlMap = new HashMap<>(4);

        urlMap.put("url", jiraUrl);

        urlMap.put("id", id);

        return urlMap;
    }

    /**
     * Try to get a JIRA pid from the issue management URL.
     *
     * @param log used to tell the user what happened
     * @param issueManagementUrl the URL to the issue management system
     * @param client the client used to connect to JIRA
     * @return the JIRA id for the project, or null if it can't be found
     */
    public static String getPidFromJira(Log log, String issueManagementUrl, HttpClient client) {
        String jiraId = null;
        HttpGet request = new HttpGet(issueManagementUrl);

        String projectPage;
        try {
            HttpResponse response = client.execute(request);
            log.debug("Successfully reached JIRA.");
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                projectPage = EntityUtils.toString(entity);
            } else {
                if (log.isDebugEnabled()) {
                    log.error("Unable to read the JIRA project page");
                }
                return null;
            }
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.error("Unable to reach the JIRA project page:", e);
            } else {
                log.error("Unable to reach the JIRA project page. Cause is: " + e.getLocalizedMessage());
            }
            return null;
        }

        int pidIndex = projectPage.indexOf(PID);

        if (pidIndex == -1) {
            log.error("Unable to extract a JIRA pid from the page at the url " + issueManagementUrl);
        } else {
            NumberFormat nf = NumberFormat.getInstance();
            Number pidNumber = nf.parse(projectPage, new ParsePosition(pidIndex + PID.length()));
            jiraId = Integer.toString(pidNumber.intValue());
            log.debug("Found the pid " + jiraId + " at " + issueManagementUrl);
        }
        return jiraId;
    }

    private JiraHelper() {
        // utility class
    }

    /**
     * Parse out the base URL for JIRA and the JIRA project name from the issue management URL. The issue management URL
     * is assumed to be of the format http(s)://host:port/browse/{projectname}
     *
     * @param issueManagementUrl the URL to the issue management system
     * @return A <code>Map</code> containing the URL and project name
     * @since 2.8
     */
    public static Map<String, String> getJiraUrlAndProjectName(String issueManagementUrl) {
        final int indexBrowse = issueManagementUrl.indexOf("/browse/");

        HashMap<String, String> urlMap = new HashMap<>(4);

        if (indexBrowse != -1) {
            String jiraUrl = issueManagementUrl.substring(0, indexBrowse);
            urlMap.put("url", jiraUrl);

            final int indexBrowseEnd = indexBrowse + "/browse/".length();

            final int indexProject = issueManagementUrl.indexOf("/", indexBrowseEnd);

            if (indexProject != -1) {
                // Project name has trailing '/'
                String project = issueManagementUrl.substring(indexBrowseEnd, indexProject);
                urlMap.put("project", project);
            } else {
                // Project name without trailing '/'
                String project = issueManagementUrl.substring(indexBrowseEnd);
                urlMap.put("project", project);
            }
        } else {
            throw new IllegalArgumentException("Invalid browse URL");
        }

        return urlMap;
    }

    /**
     * @param url URL
     * @return the base URL
     * @since 2.8
     */
    public static String getBaseUrl(String url) {
        int index = url.indexOf("/", 8); // Ignore http:// or https://
        return url.substring(0, index);
    }
}
