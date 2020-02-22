package org.apache.maven.plugins.changes;

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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.io.input.XmlStreamReader;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.changes.model.Release;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFileFilterRequest;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * Goal which creates a nicely formatted Changes Report in html format from a changes.xml file.
 *
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 */
@Mojo( name = "changes-report", threadSafe = true )
public class ChangesMojo
    extends AbstractChangesReport
{
    /**
     * A flag whether the report should also include changes from child modules. If set to <code>false</code>, only the
     * changes from current project will be written to the report.
     *
     * @since 2.5
     */
    @Parameter( defaultValue = "false" )
    private boolean aggregated;

    /**
     * A flag whether the report should also include the dates of individual actions. If set to <code>false</code>, only
     * the dates of releases will be written to the report.
     *
     * @since 2.1
     */
    @Parameter( property = "changes.addActionDate", defaultValue = "false" )
    private boolean addActionDate;

    /**
     * Whether HTML code within an action should be escaped. By changing this to <code>false</code> you can restore the
     * behavior that was in version 2.2 of this plugin, allowing you to use HTML code to format the content of an
     * action.
     * <p>
     * <strong>Note:</strong> If you use HTML code in an action you need to place it inside a CDATA section.
     * </p>
     * <strong>Note:</strong> Putting any kind of markup inside a CDATA section might mess up the Changes Report or
     * other generated documents, such as PDFs, that are based on your <code>changes.xml</code> file if you are not
     * careful.
     *
     * @since 2.4
     * @deprecated using markup inside CDATA sections does not work for all output formats!
     */
    @Parameter
    private Boolean escapeHTML;

    /**
     * The directory for interpolated changes.xml.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "${project.build.directory}/changes", required = true, readonly = true )
    private File filteredOutputDirectory;

    /**
     * applying filtering filtering "a la" resources plugin
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "false" )
    private boolean filteringChanges;

    /**
     * Template string that is used to discover the URL to use to display an issue report. There are 2 template tokens
     * you can use. <code>%URL%</code>: this is computed by getting the <code>&lt;issueManagement&gt;/&lt;url&gt;</code>
     * value from the POM, and removing the last '/' and everything that comes after it. <code>%ISSUE%</code>: this is
     * the issue number.
     * <p>
     * <strong>Note:</strong> In versions of this plugin prior to 2.0-beta-2 this parameter was called
     * <code>link_template</code>.
     * </p>
     *
     * @since 2.0-beta-2
     * @deprecated As of 2.1 use issueLinkTemplatePerSystem: this one will be with system default
     */
    @Parameter( property = "changes.issueLinkTemplate" )
    private String issueLinkTemplate;

    /**
     * Template strings per system that is used to discover the URL to use to display an issue report. Each key in this
     * map denotes the (case-insensitive) identifier of the issue tracking system and its value gives the URL template.
     * <p>
     * There are 2 template tokens you can use. <code>%URL%</code>: this is computed by getting the
     * <code>&lt;issueManagement&gt;/&lt;url&gt;</code> value from the POM, and removing the last '/' and everything
     * that comes after it. <code>%ISSUE%</code>: this is the issue number.
     * </p>
     * <p>
     * <strong>Note:</strong> The deprecated issueLinkTemplate will be used for a system called "default".
     * </p>
     * <p>
     * <strong>Note:</strong> Starting with version 2.4 you usually don't need to specify this, unless you need to link
     * to an issue management system in your Changes report that isn't supported out of the box. See the
     * <a href="./usage.html">Usage page</a> for more information.
     * </p>
     *
     * @since 2.1
     */
    @Parameter
    private Map<String, String> issueLinkTemplatePerSystem;

    /**
     * @since 2.2
     */
    @Component
    private MavenFileFilter mavenFileFilter;

    /**
     * Format to use for publishDate. The value will be available with the following expression ${publishDate}
     *
     * @see java.text.SimpleDateFormat
     * @since 2.2
     */
    @Parameter( defaultValue = "yyyy-MM-dd" )
    private String publishDateFormat;

    /**
     * Locale to use for publishDate when formatting
     *
     * @see java.util.Locale
     * @since 2.2
     */
    @Parameter( defaultValue = "en" )
    private String publishDateLocale;

    /**
     * @since 2.2
     */
    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    protected MavenSession session;

    /**
     * @since 2.4
     */
    @Parameter( defaultValue = "${project.issueManagement.system}", readonly = true )
    private String system;

    /**
     * The URI of a file containing all the team members. If this is set to the special value "none", no links will be
     * generated for the team members.
     *
     * @since 2.4
     */
    @Parameter( defaultValue = "team.html" )
    private String team;

    /**
     */
    @Parameter( defaultValue = "${project.issueManagement.url}", readonly = true )
    private String url;

    /**
     * The type of the feed to generate.
     * <p>
     * Supported values are: <code>"rss_0.9", "rss_0.91N" (RSS 0.91 Netscape), "rss_0.91U" (RSS 0.91 Userland),
     * "rss_0.92", "rss_0.93", "rss_0.94", "rss_1.0", "rss_2.0", "atom_0.3", "atom_1.0"</code>.
     * </p>
     * <p>
     * If not specified, no feed is generated.
     * </p>
     *
     * @since 2.9
     */
    @Parameter
    private String feedType;

    /**
     * The path of the <code>changes.xml</code> file that will be converted into an HTML report.
     */
    @Parameter( property = "changes.xmlPath", defaultValue = "src/changes/changes.xml" )
    private File xmlPath;

    private ReleaseUtils releaseUtils = new ReleaseUtils( getLog() );

    private CaseInsensitiveMap caseInsensitiveIssueLinkTemplatePerSystem;

    /* --------------------------------------------------------------------- */
    /* Public methods */
    /* --------------------------------------------------------------------- */

    public boolean canGenerateReport()
    {
        // Run only at the execution root
        if ( runOnlyAtExecutionRoot && !isThisTheExecutionRoot() )
        {
            getLog().info( "Skipping the Changes Report in this project because it's not the Execution Root" );
            return false;
        }
        return xmlPath.isFile();
    }

    public void executeReport( Locale locale )
        throws MavenReportException
    {
        failIfUsingDeprecatedParameter( escapeHTML, "escapeHTML",
                                        "Using markup inside CDATA sections does not work for all output formats!" );
        failIfUsingDeprecatedParameter( issueLinkTemplate, "issueLinkTemplate",
                                        "You must use 'issueLinkTemplatePerSystem' for the system '"
                                            + ChangesReportGenerator.DEFAULT_ISSUE_SYSTEM_KEY + "' instead." );
        Date now = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat( publishDateFormat, new Locale( publishDateLocale ) );
        Properties additionalProperties = new Properties();
        additionalProperties.put( "publishDate", simpleDateFormat.format( now ) );

        ChangesXML changesXml = getChangesFromFile( xmlPath, project, additionalProperties );
        if ( changesXml == null )
        {
            return;
        }

        if ( aggregated )
        {
            final String basePath = project.getBasedir().getAbsolutePath();
            final String absolutePath = xmlPath.getAbsolutePath();
            if ( !absolutePath.startsWith( basePath ) )
            {
                getLog().warn( "xmlPath should be within the project dir for aggregated changes report." );
                return;
            }
            final String relativePath = absolutePath.substring( basePath.length() );

            List<Release> releaseList = changesXml.getReleaseList();
            for ( Object o : project.getCollectedProjects() )
            {
                final MavenProject childProject = (MavenProject) o;
                final File changesFile = new File( childProject.getBasedir(), relativePath );
                final ChangesXML childXml = getChangesFromFile( changesFile, childProject, additionalProperties );
                if ( childXml != null )
                {
                    releaseList =
                        releaseUtils.mergeReleases( releaseList, childProject.getName(), childXml.getReleaseList() );
                }
            }
            changesXml.setReleaseList( releaseList );
        }

        ChangesReportGenerator report = new ChangesReportGenerator( changesXml.getReleaseList() );

        report.setAuthor( changesXml.getAuthor() );
        report.setTitle( changesXml.getTitle() );

        report.setEscapeHTML( true );

        // Create a case insensitive version of issueLinkTemplatePerSystem
        // We need something case insensitive to maintain backward compatibility
        if ( issueLinkTemplatePerSystem == null )
        {
            caseInsensitiveIssueLinkTemplatePerSystem = new CaseInsensitiveMap();
        }
        else
        {
            caseInsensitiveIssueLinkTemplatePerSystem = new CaseInsensitiveMap( issueLinkTemplatePerSystem );
        }

        // Set good default values for issue management systems here
        addIssueLinkTemplate( ChangesReportGenerator.DEFAULT_ISSUE_SYSTEM_KEY, "%URL%/ViewIssue.jspa?key=%ISSUE%" );
        addIssueLinkTemplate( "Bitbucket", "%URL%/issue/%ISSUE%" );
        addIssueLinkTemplate( "Bugzilla", "%URL%/show_bug.cgi?id=%ISSUE%" );
        addIssueLinkTemplate( "GitHub", "%URL%/%ISSUE%" );
        addIssueLinkTemplate( "GoogleCode", "%URL%/detail?id=%ISSUE%" );
        addIssueLinkTemplate( "JIRA", "%URL%/%ISSUE%" );
        addIssueLinkTemplate( "Mantis", "%URL%/view.php?id=%ISSUE%" );
        addIssueLinkTemplate( "MKS", "%URL%/viewissue?selection=%ISSUE%" );
        addIssueLinkTemplate( "Redmine", "%URL%/issues/show/%ISSUE%" );
        addIssueLinkTemplate( "Scarab", "%URL%/issues/id/%ISSUE%" );
        addIssueLinkTemplate( "SourceForge", "http://sourceforge.net/support/tracker.php?aid=%ISSUE%" );
        addIssueLinkTemplate( "SourceForge2", "%URL%/%ISSUE%" );
        addIssueLinkTemplate( "Trac", "%URL%/ticket/%ISSUE%" );
        addIssueLinkTemplate( "Trackplus", "%URL%/printItem.action?key=%ISSUE%" );
        addIssueLinkTemplate( "Tuleap", "%URL%/?aid=%ISSUE%" );
        addIssueLinkTemplate( "YouTrack", "%URL%/issue/%ISSUE%" );
        // @todo Add more issue management systems here
        // Remember to also add documentation in usage.apt.vm

        // Show the current issueLinkTemplatePerSystem configuration
        logIssueLinkTemplatePerSystem( caseInsensitiveIssueLinkTemplatePerSystem );

        report.setIssueLinksPerSystem( caseInsensitiveIssueLinkTemplatePerSystem );

        report.setSystem( system );

        report.setTeam( team );

        report.setUrl( url );

        report.setAddActionDate( addActionDate );

        if ( StringUtils.isEmpty( url ) )
        {
            getLog().warn( "No issue management URL defined in POM. Links to your issues will not work correctly." );
        }

        boolean feedGenerated = false;

        if ( StringUtils.isNotEmpty( feedType ) )
        {
            feedGenerated = generateFeed( changesXml, locale );
        }

        report.setLinkToFeed( feedGenerated );

        report.doGenerateReport( getBundle( locale ), getSink() );

        // Copy the images
        copyStaticResources();
    }

    private void failIfUsingDeprecatedParameter( Object value, String name, String message )
        throws MavenReportException
    {
        if ( value != null )
        {
            throw new MavenReportException( "You are using the old parameter '" + name + "'. " + message );
        }
    }

    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.issues.description" );
    }

    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.issues.name" );
    }

    public String getOutputName()
    {
        return "changes-report";
    }

    /* --------------------------------------------------------------------- */
    /* Private methods */
    /* --------------------------------------------------------------------- */

    /**
     * Parses specified changes.xml file. It also makes filtering if needed. If specified file doesn't exist it will log
     * warning and return <code>null</code>.
     *
     * @param changesXml changes xml file to parse
     * @param project maven project to parse changes for
     * @param additionalProperties additional properties used for filtering
     * @return parsed <code>ChangesXML</code> instance or null if file doesn't exist
     * @throws MavenReportException if any errors occurs while parsing
     */
    private ChangesXML getChangesFromFile( File changesXml, MavenProject project, Properties additionalProperties )
        throws MavenReportException
    {
        if ( !changesXml.exists() )
        {
            getLog().warn( "changes.xml file " + changesXml.getAbsolutePath() + " does not exist." );
            return null;
        }

        if ( filteringChanges )
        {
            if ( !filteredOutputDirectory.exists() )
            {
                filteredOutputDirectory.mkdirs();
            }
            XmlStreamReader xmlStreamReader = null;
            try
            {
                // so we get encoding from the file itself
                xmlStreamReader = new XmlStreamReader( changesXml );
                String encoding = xmlStreamReader.getEncoding();
                File resultFile = new File( filteredOutputDirectory,
                                            project.getGroupId() + "." + project.getArtifactId() + "-changes.xml" );

                final MavenFileFilterRequest mavenFileFilterRequest =
                    new MavenFileFilterRequest( changesXml, resultFile, true, project, Collections.<String>emptyList(),
                                                false, encoding, session, additionalProperties );
                mavenFileFilter.copyFile( mavenFileFilterRequest );
                changesXml = resultFile;
                xmlStreamReader.close();
                xmlStreamReader = null;
            }
            catch ( IOException | MavenFilteringException e )
            {
                throw new MavenReportException( "Exception during filtering changes file : " + e.getMessage(), e );
            }
            finally
            {
                IOUtil.close( xmlStreamReader );
            }

        }
        return new ChangesXML( changesXml, getLog() );
    }

    /**
     * Add the issue link template for the given issue management system, but only if it has not already been
     * configured.
     *
     * @param system The issue management system
     * @param issueLinkTemplate The issue link template to use
     * @since 2.4
     */
    private void addIssueLinkTemplate( String system, String issueLinkTemplate )
    {
        if ( caseInsensitiveIssueLinkTemplatePerSystem == null )
        {
            caseInsensitiveIssueLinkTemplatePerSystem = new CaseInsensitiveMap();
        }
        if ( !caseInsensitiveIssueLinkTemplatePerSystem.containsKey( system ) )
        {
            caseInsensitiveIssueLinkTemplatePerSystem.put( system, issueLinkTemplate );
        }
    }

    private void copyStaticResources()
        throws MavenReportException
    {
        final String pluginResourcesBase = "org/apache/maven/plugins/changes";
        String[] resourceNames = { "images/add.gif", "images/fix.gif", "images/icon_help_sml.gif", "images/remove.gif",
            "images/rss.png", "images/update.gif" };
        try
        {
            getLog().debug( "Copying static resources." );
            for ( String resourceName : resourceNames )
            {
                URL url = this.getClass().getClassLoader().getResource( pluginResourcesBase + "/" + resourceName );
                FileUtils.copyURLToFile( url, new File( getReportOutputDirectory(), resourceName ) );
            }
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Unable to copy static resources." );
        }
    }

    private ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "changes-report", locale, this.getClass().getClassLoader() );
    }

    protected String getTeam()
    {
        return team;
    }

    private void logIssueLinkTemplatePerSystem( Map<String, String> issueLinkTemplatePerSystem )
    {
        if ( getLog().isDebugEnabled() )
        {
            if ( issueLinkTemplatePerSystem == null )
            {
                getLog().debug( "No issueLinkTemplatePerSystem configuration was found" );
            }
            else
            {
                for ( Entry<String, String> entry : issueLinkTemplatePerSystem.entrySet() )
                {
                    getLog().debug( "issueLinkTemplatePerSystem[" + entry.getKey() + "] = " + entry.getValue() );
                }
            }
        }
    }

    private boolean generateFeed( final ChangesXML changesXml, final Locale locale )
    {
        getLog().debug( "Generating " + feedType + " feed." );

        boolean success = true;

        final FeedGenerator feed = new FeedGenerator( locale );
        feed.setLink( project.getUrl() + "/changes-report.html" ); // TODO: better way?
        feed.setTitle( project.getName() + ": " + changesXml.getTitle() );
        feed.setAuthor( changesXml.getAuthor() );
        feed.setDateFormat( new SimpleDateFormat( publishDateFormat, new Locale( publishDateLocale ) ) );

        Writer writer = null;

        try
        {
            writer = new FileWriter( new File( getReportOutputDirectory(), "changes.rss" ) );
            feed.export( changesXml.getReleaseList(), feedType, writer );
        }
        catch ( IOException ex )
        {
            success = false;
            getLog().warn( "Failed to create rss feed: " + ex.getMessage() );
            getLog().debug( ex );
        }
        finally
        {
            try
            {
                if ( writer != null )
                {
                    writer.close();
                }
            }
            catch ( IOException ex )
            {
                getLog().warn( "Failed to close writer: " + ex.getMessage() );
                getLog().debug( ex );
            }
        }

        return success;
    }
}
