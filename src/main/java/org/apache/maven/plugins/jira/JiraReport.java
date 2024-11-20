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
package org.apache.maven.plugins.jira;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.changes.AbstractChangesReport;
import org.apache.maven.plugins.changes.ProjectUtils;
import org.apache.maven.plugins.issues.Issue;
import org.apache.maven.plugins.issues.IssueUtils;
import org.apache.maven.plugins.issues.IssuesReportGenerator;
import org.apache.maven.plugins.issues.IssuesReportHelper;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

/**
 * Goal which downloads issues from the Issue Tracking System and generates a report.
 *
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 */
@Mojo(name = "jira-report", threadSafe = true)
public class JiraReport extends AbstractChangesReport {
    /**
     * Valid JIRA columns.
     */
    private static final Map<String, Integer> JIRA_COLUMNS = new HashMap<>(16);

    static {
        JIRA_COLUMNS.put("Assignee", IssuesReportHelper.COLUMN_ASSIGNEE);
        JIRA_COLUMNS.put("Component", IssuesReportHelper.COLUMN_COMPONENT);
        JIRA_COLUMNS.put("Created", IssuesReportHelper.COLUMN_CREATED);
        JIRA_COLUMNS.put("Fix Version", IssuesReportHelper.COLUMN_FIX_VERSION);
        JIRA_COLUMNS.put("Id", IssuesReportHelper.COLUMN_ID);
        JIRA_COLUMNS.put("Key", IssuesReportHelper.COLUMN_KEY);
        JIRA_COLUMNS.put("Priority", IssuesReportHelper.COLUMN_PRIORITY);
        JIRA_COLUMNS.put("Reporter", IssuesReportHelper.COLUMN_REPORTER);
        JIRA_COLUMNS.put("Resolution", IssuesReportHelper.COLUMN_RESOLUTION);
        JIRA_COLUMNS.put("Status", IssuesReportHelper.COLUMN_STATUS);
        JIRA_COLUMNS.put("Summary", IssuesReportHelper.COLUMN_SUMMARY);
        JIRA_COLUMNS.put("Type", IssuesReportHelper.COLUMN_TYPE);
        JIRA_COLUMNS.put("Updated", IssuesReportHelper.COLUMN_UPDATED);
        JIRA_COLUMNS.put("Version", IssuesReportHelper.COLUMN_VERSION);
    }

    /**
     * Sets the names of the columns that you want in the report. The columns will appear in the report in the same
     * order as you specify them here. Multiple values can be separated by commas.
     * <p>
     * Valid columns are: <code>Assignee</code>, <code>Component</code>, <code>Created</code>, <code>Fix Version</code>,
     * <code>Id</code>, <code>Key</code>, <code>Priority</code>, <code>Reporter</code>, <code>Resolution</code>,
     * <code>Status</code>, <code>Summary</code>, <code>Type</code>, <code>Updated</code> and <code>Version</code>.
     * </p>
     *
     * @since 2.0
     */
    @Parameter(defaultValue = "Key,Summary,Status,Resolution,Assignee")
    private String columnNames;

    /**
     * Use the JIRA query language instead of the JIRA query based on HTTP parameters. From JIRA 5.1 and up only JQL is
     * supported. JIRA 4.4 supports both JQL and URL parameter based queries. From 5.1.1 this is obsolete, since REST
     * queries only use JQL.
     *
     * @since 2.8
     */
    @Parameter(property = "changes.useJql", defaultValue = "false")
    private boolean useJql;

    /**
     * Since JIRA 5.1.1, it is no longer possible to construct a URL that downloads RSS. Meanwhile JIRA added a REST API
     * in 4.2. By default, this plugin uses the REST API if available. Setting this parameter to true forces it to
     * attempt to use RSS.
     *
     * @since 2.9
     */
    @Parameter(defaultValue = "false")
    private boolean forceRss;

    /**
     * Sets the component(s) that you want to limit your report to include. Multiple values can be separated by commas
     * (such as 10011,10012). If this is set to empty - that means all components will be included.
     */
    @Parameter
    private String component;

    /**
     * Defines the filter parameters to restrict which issues are retrieved from JIRA. The filter parameter uses the
     * same format of url parameters that is used in a JIRA search.
     */
    @Parameter()
    private String filter;

    /**
     * Sets the fix version id(s) that you want to limit your report to include. These are JIRA's internal version ids,
     * <b>NOT</b> the human readable display ones. Multiple fix versions can be separated by commas. If this is set to
     * empty - that means all fix versions will be included.
     *
     * @since 2.0
     */
    @Parameter
    private String fixVersionIds;

    /**
     * The pattern used by dates in the JIRA XML-file. This is used to parse the Created and Updated fields.
     *
     * @since 2.4
     */
    @Parameter(defaultValue = "EEE, d MMM yyyy HH:mm:ss Z")
    private String jiraDatePattern;

    /**
     * Defines the JIRA password for authentication into a private JIRA installation.
     */
    @Parameter
    private String jiraPassword;

    /**
     * Defines the JIRA username for authentication into a private JIRA installation.
     */
    @Parameter
    private String jiraUser;

    /**
     * The settings.xml server id to be used for authentication into a private JIRA installation.
     *
     * @since 3.0.0
     */
    @Parameter(property = "changes.jiraServerId")
    private String jiraServerId;

    /**
     * Path to the JIRA XML file, which will be parsed.
     */
    @Parameter(defaultValue = "${project.build.directory}/jira-results.xml", required = true, readonly = true)
    private File jiraXmlPath;

    /**
     * Maximum number of entries to be fetched from JIRA.
     */
    @Parameter(defaultValue = "100")
    private int maxEntries;

    /**
     * If you only want to show issues for the current version in the report. The current version being used is
     * <code>${project.version}</code> minus any "-SNAPSHOT" suffix.
     *
     * @since 2.0
     */
    @Parameter(defaultValue = "false")
    private boolean onlyCurrentVersion;

    /**
     * Sets the priority(s) that you want to limit your report to include. Valid statuses are <code>Blocker</code>,
     * <code>Critical</code>, <code>Major</code>, <code>Minor</code> and <code>Trivial</code>. Multiple values can be
     * separated by commas. If this is set to empty - that means all priorities will be included.
     */
    @Parameter
    private String priorityIds;

    /**
     * Sets the resolution(s) that you want to fetch from JIRA. Valid resolutions are: <code>Unresolved</code>,
     * <code>Fixed</code>, <code>Won't Fix</code>, <code>Duplicate</code>, <code>Incomplete</code> and
     * <code>Cannot Reproduce</code>. Multiple values can be separated by commas.
     * <p>
     * <b>Note:</b> In versions 2.0-beta-3 and earlier this parameter had no default value.
     * </p>
     */
    @Parameter(defaultValue = "Fixed")
    private String resolutionIds;

    /**
     * Settings XML configuration.
     */
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    private Settings settings;

    /**
     * If set to <code>true</code>, then the JIRA report will not be generated.
     *
     * @since 2.8
     */
    @Parameter(property = "changes.jira.skip", defaultValue = "false")
    private boolean skip;

    /**
     * Sets the column names that you want to sort the report by. Add <code>DESC</code> following the column name to
     * specify <i>descending</i> sequence. For example <code>Fix Version DESC, Type</code> sorts first by the Fix
     * Version in descending order and then by Type in ascending order. By default sorting is done in ascending order,
     * but is possible to specify <code>ASC</code> for consistency. The previous example would then become
     * <code>Fix Version DESC, Type ASC</code>.
     * <p>
     * Valid columns are: <code>Assignee</code>, <code>Component</code>, <code>Created</code>, <code>Fix Version</code>,
     * <code>Id</code>, <code>Key</code>, <code>Priority</code>, <code>Reporter</code>, <code>Resolution</code>,
     * <code>Status</code>, <code>Summary</code>, <code>Type</code>, <code>Updated</code> and <code>Version</code>.
     * </p>
     * <p>
     * <strong>Note:</strong> If you are using JIRA 4 you need to put your sort column names in the reverse order. The
     * handling of this changed between JIRA 3 and JIRA 4. The current default value is suitable for JIRA 3. This may
     * change in the future, so please configure your sort column names in an order that works for your own JIRA
     * version. If you use JQL, by setting the <code>useJql</code> parameter to <code>true</code>, then the order of the
     * fields are in normal order again. Starting with JIRA 5.1 you have to use JQL.
     * </p>
     *
     * @since 2.0
     */
    @Parameter(defaultValue = "Priority DESC, Created DESC")
    private String sortColumnNames;

    /**
     * Sets the status(es) that you want to fetch from JIRA. Valid statuses are: <code>Open</code>,
     * <code>In Progress</code>, <code>Reopened</code>, <code>Resolved</code> and <code>Closed</code>. Multiple values
     * can be separated by commas.
     * <p>
     * If your installation of JIRA uses custom status IDs, you can reference them here by their numeric values. You can
     * obtain them on the Statuses page (in 4.0.2 it's under Administration > Issue Settings > Statuses) - just hover
     * over the Edit link for the status you want and you'll see something like &lt;your JIRA
     * URL&gt;/secure/admin/EditStatus!default.jspa?id=12345; in this case the value is 12345.
     * </p>
     * <p>
     * <b>Note:</b> In versions 2.0-beta-3 and earlier this parameter had no default value.
     * </p>
     */
    @Parameter(defaultValue = "Closed")
    private String statusIds;

    /**
     * Sets the types(s) that you want to limit your report to include. Valid types are: <code>Bug</code>,
     * <code>New Feature</code>, <code>Task</code>, <code>Improvement</code>, <code>Wish</code>, <code>Test</code> and
     * <code>Sub-task</code>. Multiple values can be separated by commas. If this is set to empty - that means all types
     * will be included.
     *
     * @since 2.0
     */
    @Parameter
    private String typeIds;

    /**
     * The prefix used when naming versions in JIRA.
     * <p>
     * If you have a project in JIRA with several components that have different release cycles, it is an often used
     * pattern to prefix the version with the name of the component, e.g. maven-filtering-1.0 etc. To fetch issues from
     * JIRA for a release of the "maven-filtering" component you would need to set this parameter to "maven-filtering-".
     * </p>
     *
     * @since 2.4
     */
    @Parameter
    private String versionPrefix;

    /**
     * Defines the http password for basic authentication into the JIRA webserver.
     */
    @Parameter
    private String webPassword;

    /**
     * Defines the http user for basic authentication into the JIRA webserver.
     */
    @Parameter
    private String webUser;

    /*
     * Used for tests.
     */
    private AbstractJiraDownloader mockDownloader;

    /* --------------------------------------------------------------------- */
    /* Public methods */
    /* --------------------------------------------------------------------- */

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#canGenerateReport()
     */
    @Override
    public boolean canGenerateReport() {
        // Run only at the execution root
        if (runOnlyAtExecutionRoot && !isThisTheExecutionRoot()) {
            getLog().info("Skipping the JIRA Report in this project because it's not the Execution Root");
            return false;
        }
        if (skip) {
            return false;
        }
        if (mockDownloader != null) {
            return true;
        }
        String message = ProjectUtils.validateIssueManagement(project, "JIRA", "JIRA Report");
        if (message != null) {
            getLog().warn(message);
        }
        return message == null;
    }

    @Override
    public void executeReport(Locale locale) throws MavenReportException {
        // Validate parameters
        List<Integer> columnIds = IssuesReportHelper.getColumnIds(columnNames, JIRA_COLUMNS);
        if (columnIds.isEmpty()) {
            // This can happen if the user has configured column names and they are all invalid
            throw new MavenReportException(
                    "maven-changes-plugin: None of the configured columnNames '" + columnNames + "' are valid.");
        }

        try {
            // Download issues
            AbstractJiraDownloader issueDownloader;
            if (mockDownloader != null) {
                issueDownloader = mockDownloader;
            } else {
                AdaptiveJiraDownloader downloader = new AdaptiveJiraDownloader();
                downloader.setForceClassic(forceRss);
                issueDownloader = downloader;
            }
            configureIssueDownloader(issueDownloader);
            issueDownloader.doExecute();

            List<Issue> issueList = issueDownloader.getIssueList();

            if (StringUtils.isNotEmpty(versionPrefix)) {
                int originalNumberOfIssues = issueList.size();
                issueList = IssueUtils.filterIssuesWithVersionPrefix(issueList, versionPrefix);
                getLog().debug("Filtered out " + issueList.size() + " issues of " + originalNumberOfIssues
                        + " that matched the versionPrefix '" + versionPrefix + "'.");
            }

            if (onlyCurrentVersion) {
                String version = (versionPrefix == null ? "" : versionPrefix) + project.getVersion();
                issueList = IssueUtils.getIssuesForVersion(issueList, version);
                getLog().info("The JIRA Report will contain issues only for the current version.");
            }

            // Generate the report
            IssuesReportGenerator report = new IssuesReportGenerator(IssuesReportHelper.toIntArray(columnIds));

            if (issueList.isEmpty()) {
                report.doGenerateEmptyReport(getBundle(locale), getSink());
            } else {
                report.doGenerateReport(getBundle(locale), getSink(), issueList);
            }
        } catch (Exception e) {
            getLog().warn(e);
        }
    }

    @Override
    public String getDescription(Locale locale) {
        return getBundle(locale).getString("report.issues.description");
    }

    @Override
    public String getName(Locale locale) {
        return getBundle(locale).getString("report.issues.name");
    }

    @Override
    public String getOutputName() {
        return "jira-report";
    }

    /* --------------------------------------------------------------------- */
    /* Private methods */
    /* --------------------------------------------------------------------- */

    private ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle("jira-report", locale, this.getClass().getClassLoader());
    }

    private void configureIssueDownloader(AbstractJiraDownloader issueDownloader) {
        issueDownloader.setLog(getLog());

        issueDownloader.setMavenProject(project);

        issueDownloader.setOutput(jiraXmlPath);

        issueDownloader.setNbEntries(maxEntries);

        issueDownloader.setComponent(component);

        issueDownloader.setFixVersionIds(fixVersionIds);

        issueDownloader.setStatusIds(statusIds);

        issueDownloader.setResolutionIds(resolutionIds);

        issueDownloader.setPriorityIds(priorityIds);

        issueDownloader.setSortColumnNames(sortColumnNames);

        issueDownloader.setFilter(filter);

        issueDownloader.setJiraDatePattern(jiraDatePattern);

        if (jiraServerId != null) {
            final Server server = mavenSession.getSettings().getServer(jiraServerId);
            issueDownloader.setJiraUser(server.getUsername());
            issueDownloader.setJiraPassword(server.getPassword());
        } else {
            issueDownloader.setJiraUser(jiraUser);
            issueDownloader.setJiraPassword(jiraPassword);
        }

        issueDownloader.setTypeIds(typeIds);

        issueDownloader.setWebUser(webUser);

        issueDownloader.setWebPassword(webPassword);

        issueDownloader.setSettings(settings);

        issueDownloader.setUseJql(useJql);

        issueDownloader.setOnlyCurrentVersion(onlyCurrentVersion);

        issueDownloader.setVersionPrefix(versionPrefix);
    }

    public void setMockDownloader(AbstractJiraDownloader mockDownloader) {
        this.mockDownloader = mockDownloader;
    }

    public AbstractJiraDownloader getMockDownloader() {
        return mockDownloader;
    }
}
