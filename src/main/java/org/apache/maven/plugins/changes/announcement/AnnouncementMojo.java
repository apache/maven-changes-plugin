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
package org.apache.maven.plugins.changes.announcement;

import javax.inject.Inject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.changes.ChangesXML;
import org.apache.maven.plugins.changes.IssueAdapter;
import org.apache.maven.plugins.changes.ProjectUtils;
import org.apache.maven.plugins.changes.ReleaseUtils;
import org.apache.maven.plugins.changes.github.GitHubDownloader;
import org.apache.maven.plugins.changes.github.GitHubIssueManagementSystem;
import org.apache.maven.plugins.changes.issues.Issue;
import org.apache.maven.plugins.changes.issues.IssueManagementSystem;
import org.apache.maven.plugins.changes.issues.IssueUtils;
import org.apache.maven.plugins.changes.jira.JIRAIssueManagementSystem;
import org.apache.maven.plugins.changes.jira.RestJiraDownloader;
import org.apache.maven.plugins.changes.model.Release;
import org.apache.maven.plugins.changes.trac.TracDownloader;
import org.apache.maven.plugins.changes.trac.TracIssueManagmentSystem;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.tools.ToolManager;
import org.codehaus.plexus.velocity.VelocityComponent;

/**
 * Goal which generates an announcement from the announcement template.
 *
 * @author aramirez@exist.com
 * @version $Id$
 * @since 2.0-beta-2
 */
@Mojo(name = "announcement-generate", threadSafe = true)
public class AnnouncementMojo extends AbstractAnnouncementMojo {
    private static final String CHANGES_XML = "changes.xml";

    private static final String JIRA = "JIRA";

    private static final String TRAC = "Trac";

    private static final String GIT_HUB = "GitHub";

    /**
     * The name of the file which will contain the generated announcement. If no value is specified, the plugin will use
     * the name of the template.
     *
     * @since 2.4
     */
    @Parameter(property = "changes.announcementFile")
    private String announcementFile;

    /**
     * Map of custom parameters for the announcement. This Map will be passed to the template.
     *
     * @since 2.1
     */
    @Parameter
    private Map<Object, Object> announceParameters;

    /**
     */
    @Parameter(property = "project.artifactId", readonly = true)
    private String artifactId;

    /**
     * Name of the team that develops the artifact. This parameter will be passed to the template.
     */
    @Parameter(property = "changes.developmentTeam", defaultValue = "${project.name} team", required = true)
    private String developmentTeam;

    /**
     * The name of the artifact to be used in the announcement.
     */
    @Parameter(property = "changes.finalName", defaultValue = "${project.build.finalName}", required = true)
    private String finalName;

    /**
     */
    @Parameter(property = "project.groupId", readonly = true)
    private String groupId;

    /**
     * Short description or introduction of the released artifact. This parameter will be passed to the template.
     */
    @Parameter(defaultValue = "${project.description}")
    private String introduction;

    /**
     * A list of issue management systems to fetch releases from. This parameter replaces the parameters
     * <code>generateJiraAnnouncement</code> and <code>jiraMerge</code>.
     * <p>
     * Valid values are: <code>changes.xml</code> and <code>JIRA</code>.
     * </p>
     * <strong>Note:</strong> Only one issue management system that is configured in
     * &lt;project&gt;/&lt;issueManagement&gt; can be used. This currently means that you can combine a changes.xml file
     * with one other issue management system.
     *
     * @since 2.4
     */
    @Parameter
    private List<String> issueManagementSystems;

    /**
     * Maps issues types to action types for grouping issues in announcements. If issue types are not defined for a
     * action type then the default issue type will be applied.
     * <p>
     * Valid action types: <code>add</code>, <code>fix</code> and <code>update</code>.
     * </p>
     *
     * @since 2.6
     */
    @Parameter
    private Map<String, String> issueTypes;

    /**
     * Directory where the announcement file will be generated.
     *
     * @since 2.10
     */
    @Parameter(defaultValue = "${project.build.directory}/announcement", required = true)
    private File announcementDirectory;

    /**
     * Packaging structure for the artifact.
     */
    @Parameter(property = "project.packaging", readonly = true)
    private String packaging;

    /**
     * The Maven Project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The Velocity template used to format the announcement.
     */
    @Parameter(property = "changes.template", defaultValue = "announcement.vm", required = true)
    private String template;

    /**
     * Directory that contains the template.
     * <p>
     * <b>Note:</b> This directory must be a subdirectory of
     * <code>/src/main/resources/ or current project base directory</code>.
     * </p>
     */
    // CHECKSTYLE_OFF: LineLength
    @Parameter(
            property = "changes.templateDirectory",
            defaultValue = "org/apache/maven/plugins/changes/announcement",
            required = true)
    private String templateDirectory;
    // CHECKSTYLE_ON: LineLength

    /**
     * The template encoding.
     *
     * @since 2.1
     */
    @Parameter(property = "changes.templateEncoding", defaultValue = "${project.build.sourceEncoding}")
    private String templateEncoding;

    /**
     * Obsolete, since REST queries always use JQL.
     *
     * @since 2.10
     * @deprecated ignored; remove from your configs
     */
    @Deprecated
    @Parameter(property = "changes.useJql", defaultValue = "false")
    private boolean useJql;

    /**
     * Distribution URL of the artifact. This parameter will be passed to the template.
     */
    @Parameter(property = "project.url")
    private String url;

    /**
     * URL where the artifact can be downloaded. If not specified, no URL is used. This parameter will be passed to the
     * template.
     */
    @Parameter
    private String urlDownload;

    /**
     * Version of the artifact.
     */
    @Parameter(property = "changes.version", defaultValue = "${project.version}", required = true)
    private String version;

    /**
     * The path of the changes.xml file.
     */
    @Parameter(defaultValue = "${basedir}/src/changes/changes.xml")
    private File xmlPath;

    // =======================================//
    // JIRA-Announcement Needed Parameters //
    // =======================================//

    /**
     * Defines the filter parameters to restrict which issues are retrieved from JIRA. The filter parameter uses the
     * same format of url parameters that is used in a JIRA search.
     *
     * @since 2.4
     */
    @Parameter
    private String filter;

    /**
     * Defines the JIRA password for authentication into a private JIRA installation.
     *
     * @since 2.1
     */
    @Parameter(property = "changes.jiraPassword")
    private String jiraPassword;

    /**
     * Defines the JIRA username for authentication into a private JIRA installation.
     *
     * @since 2.1
     */
    @Parameter(property = "changes.jiraUser")
    private String jiraUser;

    /**
     * The settings.xml server id to be used for authentication into a private JIRA installation.
     *
     * @since 3.0.0
     */
    @Parameter(property = "changes.jiraServerId")
    private String jiraServerId;

    /**
     * The maximum number of issues to fetch from JIRA.
     */
    @Parameter(property = "changes.maxEntries", defaultValue = "25", required = true)
    private int maxEntries;

    /**
     * If you only want to show issues from JIRA for the current version in the report. The current version being used is
     * <code>${project.version}</code> minus any "-SNAPSHOT" suffix.
     *
     * @since 3.0.0
     */
    @Parameter(defaultValue = "false")
    private boolean onlyCurrentVersion;

    /**
     * Include issues from JIRA with these resolution ids. Multiple resolution ids can be specified as a comma separated
     * list of ids.
     * <p>
     * <b>Note:</b> In versions 2.0-beta-3 and earlier this parameter was called "resolutionId".
     * </p>
     */
    @Parameter(property = "changes.resolutionIds", defaultValue = "Fixed")
    private String resolutionIds;

    /**
     * Settings XML configuration.
     */
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    private Settings settings;

    /**
     * Include issues from JIRA with these status ids. Multiple status ids can be specified as a comma separated list of
     * ids.
     */
    @Parameter(property = "changes.statusIds", defaultValue = "Closed")
    private String statusIds;

    /**
     * Defines the http user for basic authentication into the JIRA webserver.
     *
     * @since 2.4
     * @deprecated use {@link #jiraUser} or {@link #jiraServerId}
     */
    @Deprecated
    @Parameter(property = "changes.webUser")
    private String webUser;

    /**
     * Defines the http password for basic authentication into the JIRA webserver.
     *
     * @since 2.4
     * @deprecated use {@link #jiraPassword} or {@link #jiraServerId}
     */
    @Deprecated
    @Parameter(property = "changes.webPassword")
    private String webPassword;

    /**
     * The prefix used when naming versions in JIRA.
     * <p>
     * If you have a project in JIRA with several components that have different release cycles, it is an often used
     * pattern to prefix the version with the name of the component, e.g. maven-filtering-1.0 etc. To fetch issues from
     * JIRA for a release of the "maven-filtering" component you would need to set this parameter to "maven-filtering-".
     * </p>
     *
     * @since 2.5
     */
    @Parameter(property = "changes.versionPrefix")
    private String versionPrefix;

    /**
     * Defines the connection timeout in milliseconds when accessing JIRA's REST-API.
     * <p>
     * Might help when you have a lot of different resolutions in your JIRA instance.
     * </p>
     *
     * @since 2.11
     */
    @Parameter(property = "changes.jiraConnectionTimeout", defaultValue = "36000")
    private int jiraConnectionTimeout;

    /**
     * Defines the receive timeout in milliseconds when accessing JIRA's REST-API.
     * <p>
     * Might help when you have a lot of different resolutions in your JIRA instance.
     * </p>
     *
     * @since 2.11
     */
    @Parameter(property = "changes.jiraReceiveTimout", defaultValue = "32000")
    private int jiraReceiveTimout;

    // =======================================//
    // Trac Parameters //
    // =======================================//

    /**
     * Defines the Trac password for authentication into a private Trac installation.
     *
     * @since 2.4
     */
    @Parameter(property = "changes.tracPassword")
    private String tracPassword;

    /**
     * Defines the Trac query for searching for tickets.
     *
     * @since 2.4
     */
    @Parameter(defaultValue = "order=id")
    private String tracQuery;

    /**
     * Defines the Trac username for authentication into a private Trac installation.
     *
     * @since 2.4
     */
    @Parameter(property = "changes.tracUser")
    private String tracUser;

    // =======================================//
    // Github Parameters //
    // =======================================//

    /**
     * The settings.xml server id to be used to authenticate into GitHub Api.
     * <br>
     * Since 3.x - only password item is used as authentication token with {@code Authorization: Bearer YOUR-TOKEN}
     * <a href="https://docs.github.com/en/rest/authentication/authenticating-to-the-rest-api">Authenticating to the REST API</a>
     *
     * @since 2.12
     */
    @Parameter(defaultValue = "github")
    private String githubAPIServerId;

    /**
     * Boolean which says if we should include open github issues in the announcement.
     */
    @Parameter(defaultValue = "false")
    private boolean includeOpenIssues;

    private ChangesXML xml;

    /**
     * Velocity Component.
     */
    private VelocityComponent velocity;

    /**
     * Component used to decrypt server information.
     */
    private final SettingsDecrypter settingsDecrypter;

    @Inject
    public AnnouncementMojo(VelocityComponent velocity, SettingsDecrypter settingsDecrypter) {
        this.velocity = velocity;
        this.settingsDecrypter = settingsDecrypter;
    }

    // =======================================//
    // announcement-generate execution //
    // =======================================//

    /**
     * Generate the template
     *
     * @throws MojoExecutionException in case of errors
     */
    public void execute() throws MojoExecutionException {
        // Run only at the execution root
        if (runOnlyAtExecutionRoot && !isThisTheExecutionRoot()) {
            getLog().info("Skipping the announcement generation in this project because it's not the Execution Root");
        } else {
            if (issueManagementSystems == null) {
                issueManagementSystems = new ArrayList<>();
            }

            if (issueManagementSystems.isEmpty()) {
                issueManagementSystems.add(CHANGES_XML);
            }

            // Fetch releases from the configured issue management systems
            List<Release> releases = null;
            if (issueManagementSystems.contains(CHANGES_XML)) {
                if (getXmlPath().exists()) {
                    ChangesXML changesXML = new ChangesXML(getXmlPath(), getLog());
                    List<Release> changesReleases = changesXML.getReleaseList();
                    releases = ReleaseUtils.mergeReleases(null, changesReleases);
                    getLog().info("Including issues from file " + getXmlPath() + " in announcement...");
                } else {
                    getLog().warn("changes.xml file " + getXmlPath().getAbsolutePath() + " does not exist.");
                }
            }

            if (issueManagementSystems.contains(JIRA)) {
                String message = ProjectUtils.validateIssueManagement(project, JIRA, "JIRA announcement");
                if (message == null) {
                    List<Release> jiraReleases = getJiraReleases();
                    releases = ReleaseUtils.mergeReleases(releases, jiraReleases);
                    getLog().info("Including issues from JIRA in announcement...");
                } else {
                    throw new MojoExecutionException(
                            "Something is wrong with the Issue Management section. " + message);
                }
            }

            if (issueManagementSystems.contains(TRAC)) {
                getLog().warn(
                                "Trac integration is prepared for removal in next major version due to lack of maintainers");
                String message = ProjectUtils.validateIssueManagement(project, TRAC, "Trac announcement");
                if (message == null) {
                    List<Release> tracReleases = getTracReleases();
                    releases = ReleaseUtils.mergeReleases(releases, tracReleases);
                    getLog().info("Including issues from Trac in announcement...");
                } else {
                    throw new MojoExecutionException(
                            "Something is wrong with the Issue Management section. " + message);
                }
            }

            if (issueManagementSystems.contains(GIT_HUB)) {
                String message = ProjectUtils.validateIssueManagement(project, GIT_HUB, "GitHub announcement");
                if (message == null) {
                    List<Release> gitHubReleases = getGitHubReleases();
                    releases = ReleaseUtils.mergeReleases(releases, gitHubReleases);
                    getLog().info("Including issues from GitHub in announcement...");
                } else {
                    throw new MojoExecutionException(
                            "Something is wrong with the Issue Management section. " + message);
                }
            }

            // @todo Add more issue management systems here.

            // Follow these steps:
            // 1. Add a constant for the name of the issue management system
            // 2. Add the @parameters needed to configure the issue management system
            // 3. Add a protected List get<IMSname>Releases() method that retrieves a list of releases
            // 4. Merge those releases into the "releases" variable
            // For help with these steps, you can have a look at how this has been done for JIRA or Trac

            // Generate the report
            if (releases == null || releases.isEmpty()) {
                throw new MojoExecutionException(
                        "No releases found in any of the " + "configured issue management systems.");
            } else {
                doGenerate(releases);
            }
        }
    }

    /**
     * Add the parameters to velocity context
     *
     * @param releases A <code>List</code> of <code>Release</code>s
     * @throws MojoExecutionException in case of errors
     */
    public void doGenerate(List<Release> releases) throws MojoExecutionException {
        String version = (versionPrefix == null ? "" : versionPrefix) + getVersion();

        getLog().debug("Generating announcement for version [" + version + "]. Found these releases: "
                + ReleaseUtils.toString(releases));

        doGenerate(releases, ReleaseUtils.getLatestRelease(releases, version));
    }

    protected void doGenerate(List<Release> releases, Release release) throws MojoExecutionException {
        try {
            ToolManager toolManager = new ToolManager(true);
            Context context = toolManager.createContext();

            if (getIntroduction() == null || getIntroduction().isEmpty()) {
                setIntroduction(getUrl());
            }

            context.put("releases", releases);

            context.put("groupId", getGroupId());

            context.put("artifactId", getArtifactId());

            context.put("version", getVersion());

            context.put("packaging", getPackaging());

            context.put("url", getUrl());

            context.put("release", release);

            context.put("introduction", getIntroduction());

            context.put("developmentTeam", getDevelopmentTeam());

            context.put("finalName", getFinalName());

            context.put("urlDownload", getUrlDownload());

            context.put("project", project);

            if (announceParameters == null) {
                // empty Map to prevent NPE in velocity execution
                context.put("announceParameters", Collections.emptyMap());
            } else {
                context.put("announceParameters", announceParameters);
            }

            processTemplate(context, announcementDirectory, template, announcementFile);
        } catch (ResourceNotFoundException rnfe) {
            throw new MojoExecutionException("Resource not found.", rnfe);
        } catch (VelocityException ve) {
            throw new MojoExecutionException(ve.toString(), ve);
        }
    }

    /**
     * Create the Velocity template.
     *
     * @param context velocity context that has the parameter values
     * @param outputDirectory directory where the file will be generated
     * @param template velocity template which will the context be merged
     * @param announcementFile the file name of the generated announcement
     * @throws VelocityException in case of error processing the Velocity template
     * @throws MojoExecutionException in case of errors
     */
    public void processTemplate(Context context, File outputDirectory, String template, String announcementFile)
            throws VelocityException, MojoExecutionException {

        // Use the name of the template as a default value
        if (announcementFile == null || announcementFile.isEmpty()) {
            announcementFile = template;
        }

        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                throw new MojoExecutionException("Failed to create directory " + outputDirectory);
            }
        }

        File f = new File(outputDirectory, announcementFile);

        VelocityEngine engine = velocity.getEngine();

        engine.setApplicationAttribute("baseDirectory", basedir);

        if (templateEncoding == null || templateEncoding.isEmpty()) {
            templateEncoding = Charset.defaultCharset().name();
            getLog().warn("File encoding has not been set, using platform encoding " + templateEncoding
                    + "; build is platform dependent!");
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(f), templateEncoding)) {
            Template velocityTemplate = engine.getTemplate(templateDirectory + "/" + template, templateEncoding);
            velocityTemplate.merge(context, writer);
            getLog().info("Created template " + f);
        } catch (ResourceNotFoundException ex) {
            throw new ResourceNotFoundException(
                    "Template not found. ( " + templateDirectory + "/" + template + " )", ex);
        } catch (VelocityException ve) {
            throw ve;
        } catch (RuntimeException | IOException e) {
            throw new MojoExecutionException(e.toString(), e);
        }
    }

    protected List<Release> getJiraReleases() throws MojoExecutionException {
        RestJiraDownloader jiraDownloader = new RestJiraDownloader();

        jiraDownloader.setLog(getLog());

        jiraDownloader.setStatusIds(statusIds);

        jiraDownloader.setResolutionIds(resolutionIds);

        jiraDownloader.setMavenProject(project);

        jiraDownloader.setSettings(settings);
        jiraDownloader.setSettingsDecrypter(settingsDecrypter);

        jiraDownloader.setNbEntries(maxEntries);
        jiraDownloader.setOnlyCurrentVersion(onlyCurrentVersion);
        jiraDownloader.setVersionPrefix(versionPrefix);

        jiraDownloader.setFilter(filter);

        jiraDownloader.setJiraServerId(jiraServerId);
        if (jiraUser != null) {
            jiraDownloader.setJiraUser(jiraUser);
            jiraDownloader.setJiraPassword(jiraPassword);
        } else if (webUser != null) {
            jiraDownloader.setJiraUser(webUser);
            jiraDownloader.setJiraPassword(webPassword);
        }

        jiraDownloader.setConnectionTimeout(jiraConnectionTimeout);

        jiraDownloader.setReceiveTimout(jiraReceiveTimout);

        try {
            jiraDownloader.doExecute();

            List<Issue> issueList = jiraDownloader.getIssueList();

            if (versionPrefix != null && !versionPrefix.isEmpty()) {
                int originalNumberOfIssues = issueList.size();
                issueList = IssueUtils.filterIssuesWithVersionPrefix(issueList, versionPrefix);
                getLog().debug("Filtered out " + issueList.size() + " issues of " + originalNumberOfIssues
                        + " that matched the versionPrefix '" + versionPrefix + "'.");
            }

            if (onlyCurrentVersion) {
                String version = (versionPrefix == null ? "" : versionPrefix) + project.getVersion();
                issueList = IssueUtils.getIssuesForVersion(issueList, version);
                getLog().debug("The JIRA Report will contain issues only for the current version.");
            }

            return getReleases(issueList, new JIRAIssueManagementSystem());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to extract issues from JIRA.", e);
        }
    }

    private List<Release> getReleases(List<Issue> issues, IssueManagementSystem ims) throws MojoExecutionException {
        if (issueTypes != null) {
            ims.applyConfiguration(issueTypes);
        }
        if (issues.isEmpty()) {
            return Collections.emptyList();
        } else {
            IssueAdapter adapter = new IssueAdapter(ims);
            return adapter.getReleases(issues);
        }
    }

    protected List<Release> getTracReleases() throws MojoExecutionException {
        TracDownloader issueDownloader = new TracDownloader();

        issueDownloader.setProject(project);

        issueDownloader.setQuery(tracQuery);

        issueDownloader.setTracPassword(tracPassword);

        issueDownloader.setTracUser(tracUser);

        try {
            return getReleases(issueDownloader.getIssueList(), new TracIssueManagmentSystem());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to extract issues from Trac.", e);
        }
    }

    protected List<Release> getGitHubReleases() throws MojoExecutionException {
        try {
            GitHubDownloader issueDownloader = new GitHubDownloader(project, includeOpenIssues, true);

            issueDownloader.configureAuthentication(settingsDecrypter, githubAPIServerId, settings, getLog());

            return getReleases(issueDownloader.getIssueList(), new GitHubIssueManagementSystem());
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to extract issues from GitHub.", e);
        }
    }

    /*
     * accessors
     */

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getDevelopmentTeam() {
        return developmentTeam;
    }

    public void setDevelopmentTeam(String developmentTeam) {
        this.developmentTeam = developmentTeam;
    }

    public String getFinalName() {
        return finalName;
    }

    public void setFinalName(String finalName) {
        this.finalName = finalName;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public void setIssueTypes(Map<String, String> issueTypes) {
        this.issueTypes = issueTypes;
    }

    public Map<String, String> getIssueTypes() {
        return issueTypes;
    }

    public File getAnnouncementDirectory() {
        return announcementDirectory;
    }

    public void setAnnouncementDirectory(File announcementDirectory) {
        this.announcementDirectory = announcementDirectory;
    }

    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrlDownload() {
        return urlDownload;
    }

    public void setUrlDownload(String urlDownload) {
        this.urlDownload = urlDownload;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ChangesXML getXml() {
        return xml;
    }

    public void setXml(ChangesXML xml) {
        this.xml = xml;
    }

    public File getXmlPath() {
        return xmlPath;
    }

    public void setXmlPath(File xmlPath) {
        this.xmlPath = xmlPath;
    }
}
