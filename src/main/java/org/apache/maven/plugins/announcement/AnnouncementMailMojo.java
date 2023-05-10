package org.apache.maven.plugins.announcement;

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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.maven.model.Developer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.announcement.mailsender.ProjectJavamailMailSender;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.mailsender.MailMessage;
import org.codehaus.plexus.mailsender.MailSenderException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;

/**
 * Goal which sends an announcement through email.
 *
 * @author aramirez@exist.com
 * @version $Id$
 * @since 2.0-beta-2
 */
@Mojo( name = "announcement-mail", threadSafe = true )
@Execute( goal = "announcement-generate" )
public class AnnouncementMailMojo
    extends AbstractAnnouncementMojo
{
    // =========================================
    // announcement-mail goal fields
    // =========================================

    /**
     * Possible senders.
     */
    @Parameter( property = "project.developers", required = true, readonly = true )
    private List<Developer> from;

    /**
     * The id of the developer sending the announcement mail. Only used if the {@code mailSender} attribute is not set.
     * In this case, this should match the id of one of the developers in the pom. If a matching developer is not found,
     * then the first developer in the pom will be used.
     */
    @Parameter( property = "changes.fromDeveloperId" )
    private String fromDeveloperId;

    /**
     * Mail content type to use.
     *
     * @since 2.1
     */
    @Parameter( defaultValue = "text/plain", required = true )
    private String mailContentType;

    /**
     * Defines the sender of the announcement email. This takes precedence over the list of developers specified in the
     * POM. if the sender is not a member of the development team. Note that since this is a bean type, you cannot
     * specify it from command level with
     * 
     * <pre>
     * -D
     * </pre>
     * 
     * . Use
     * 
     * <pre>
     * -Dchanges.sender='Your Name &lt;you@domain>'
     * </pre>
     * 
     * instead.
     */
    @Parameter( property = "changes.mailSender" )
    private MailSender mailSender;

    /**
     * Defines the sender of the announcement. This takes precedence over both ${changes.mailSender} and the list of
     * developers in the POM.
     * <p/>
     * This parameter parses an email address in standard RFC822 format, e.g.
     * 
     * <pre>
     * -Dchanges.sender='Your Name &lt;you@domain>'
     * </pre>
     * 
     * .
     *
     * @since 2.7
     */
    @Parameter( property = "changes.sender" )
    private String senderString;

    /**
     * The password used to send the email.
     */
    @Parameter( property = "changes.password" )
    private String password;

    /**
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    /**
     * Smtp Server.
     */
    @Parameter( property = "changes.smtpHost", required = true )
    private String smtpHost;

    /**
     * Port.
     */
    @Parameter( property = "changes.smtpPort", defaultValue = "25", required = true )
    private int smtpPort;

    /**
     * If the email should be sent in SSL mode.
     */
    @Parameter( property = "changes.sslMode", defaultValue = "false" )
    private boolean sslMode;

    /**
     * If the option startTls should be used.
     *
     * @since 2.10
     */
    @Parameter( property = "changes.startTls", defaultValue = "false" )
    private boolean startTls;

    /**
     * Subject for the email.
     */
    // CHECKSTYLE_OFF: LineLength
    @Parameter( property = "changes.subject", defaultValue = "[ANNOUNCEMENT] - ${project.name} ${project.version} released", required = true )
    private String subject;
    // CHECKSTYLE_ON: LineLength

    /**
     * The file that contains the generated announcement.
     *
     * @since 2.10
     */
    @Parameter( property = "changes.announcementFile", defaultValue = "announcement.vm", required = true )
    private String announcementFile;

    /**
     * Directory where the generated announcement file exists.
     *
     * @since 2.10
     */
    @Parameter( defaultValue = "${project.build.directory}/announcement", required = true )
    private File announcementDirectory;

    /**
     * The encoding used in the announcement template.
     *
     * @since 2.10
     */
    @Parameter( property = "changes.templateEncoding", defaultValue = "${project.build.sourceEncoding}" )
    private String templateEncoding;

    /**
     * Directory which contains the template for announcement email.
     *
     * @deprecated Starting with version 2.10 this parameter is no longer used. You must use
     *             {@link #announcementDirectory} instead.
     */
    @Parameter
    private File templateOutputDirectory;

    /**
     * Recipient email address.
     */
    @Parameter( required = true )
    private List<Object> toAddresses;

    /**
     * Recipient cc email address.
     *
     * @since 2.5
     */
    @Parameter
    private List<Object> ccAddresses;

    /**
     * Recipient bcc email address.
     *
     * @since 2.5
     */
    @Parameter
    private List<Object> bccAddresses;

    /**
     * The username used to send the email.
     */
    @Parameter( property = "changes.username" )
    private String username;

    private ProjectJavamailMailSender mailer = new ProjectJavamailMailSender();

    public void execute()
        throws MojoExecutionException
    {
        // Fail build fast if it is using deprecated parameters
        if ( templateOutputDirectory != null )
        {
            throw new MojoExecutionException( "You are using the old parameter 'templateOutputDirectory'. "
                + "You must use 'announcementDirectory' instead." );
        }

        // Run only at the execution root
        if ( runOnlyAtExecutionRoot && !isThisTheExecutionRoot() )
        {
            getLog().info( "Skipping the announcement mail in this project because it's not the Execution Root" );
        }
        else
        {
            File file = new File( announcementDirectory, announcementFile );

            ConsoleLogger logger = new ConsoleLogger( Logger.LEVEL_INFO, "base" );

            if ( getLog().isDebugEnabled() )
            {
                logger.setThreshold( Logger.LEVEL_DEBUG );
            }

            mailer.enableLogging( logger );

            mailer.setSmtpHost( getSmtpHost() );

            mailer.setSmtpPort( getSmtpPort() );

            mailer.setSslMode( sslMode, startTls );

            if ( username != null )
            {
                mailer.setUsername( username );
            }

            if ( password != null )
            {
                mailer.setPassword( password );
            }

            mailer.initialize();

            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "fromDeveloperId: " + getFromDeveloperId() );
            }

            if ( file.isFile() )
            {
                getLog().info( "Connecting to Host: " + getSmtpHost() + ":" + getSmtpPort() );

                sendMessage();
            }
            else
            {
                throw new MojoExecutionException( "Announcement file " + file + " not found..." );
            }
        }
    }

    /**
     * Send the email.
     *
     * @throws MojoExecutionException if the mail could not be sent
     */
    protected void sendMessage()
        throws MojoExecutionException
    {
        File file = new File( announcementDirectory, announcementFile );
        String email = "";
        final MailSender ms = getActualMailSender();
        final String fromName = ms.getName();
        final String fromAddress = ms.getEmail();
        if ( fromAddress == null || fromAddress.equals( "" ) )
        {
            throw new MojoExecutionException( "Invalid mail sender: name and email is mandatory (" + ms + ")." );
        }
        getLog().info( "Using this sender for email announcement: " + fromAddress + " < " + fromName + " > " );
        try
        {
            MailMessage mailMsg = new MailMessage();
            mailMsg.setSubject( getSubject() );
            mailMsg.setContent( readAnnouncement( file ) );
            mailMsg.setContentType( this.mailContentType );
            mailMsg.setFrom( fromAddress, fromName );

            for ( Object o1 : getToAddresses() )
            {
                email = o1.toString();
                getLog().info( "Sending mail to " + email + "..." );
                mailMsg.addTo( email, "" );
            }

            if ( getCcAddresses() != null )
            {
                for ( Object o : getCcAddresses() )
                {
                    email = o.toString();
                    getLog().info( "Sending cc mail to " + email + "..." );
                    mailMsg.addCc( email, "" );
                }
            }

            if ( getBccAddresses() != null )
            {
                for ( Object o : getBccAddresses() )
                {
                    email = o.toString();
                    getLog().info( "Sending bcc mail to " + email + "..." );
                    mailMsg.addBcc( email, "" );
                }
            }

            mailer.send( mailMsg );
            getLog().info( "Sent..." );
        }
        catch ( MailSenderException e )
        {
            throw new MojoExecutionException( "Failed to send email < " + email + " >", e );
        }
    }

    /**
     * Read the content of the generated announcement file.
     *
     * @param file the file to be read
     * @return Return the announcement text
     * @throws MojoExecutionException if the file could not be found, or if the encoding is unsupported
     */
    protected String readAnnouncement( File file )
        throws MojoExecutionException
    {
        try
        {
            if ( templateEncoding == null || templateEncoding.isEmpty() )
            {
                templateEncoding = ReaderFactory.FILE_ENCODING;
                getLog().warn( "File encoding has not been set, using platform encoding '" + templateEncoding
                                   + "', i.e. build is platform dependent!" );

            }

            try ( InputStreamReader reader = new InputStreamReader( new FileInputStream( file ), templateEncoding ) )
            {
                return IOUtil.toString( reader );
            }
        }
        catch ( FileNotFoundException fnfe )
        {
            throw new MojoExecutionException( "File not found. " + file );
        }
        catch ( UnsupportedEncodingException uee )
        {
            throw new MojoExecutionException( "Unsupported encoding: '" + templateEncoding + "'" );
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Failed to read the announcement file.", ioe );
        }
    }

    /**
     * Returns the identify of the mail sender according to the plugin's configuration:
     * <ul>
     * <li>if the {@code mailSender} parameter is set, it is returned</li>
     * <li>if no {@code fromDeveloperId} is set, the first developer in the list is returned</li>
     * <li>if a {@code fromDeveloperId} is set, the developer with that id is returned</li>
     * <li>if the developers list is empty or if the specified id does not exist, an exception is thrown</li>
     * </ul>
     *
     * @return the mail sender to use
     * @throws MojoExecutionException if the mail sender could not be retrieved
     */
    protected MailSender getActualMailSender()
        throws MojoExecutionException
    {
        if ( senderString != null )
        {
            try
            {
                InternetAddress ia = new InternetAddress( senderString, true );
                return new MailSender( ia.getPersonal(), ia.getAddress() );
            }
            catch ( AddressException e )
            {
                throw new MojoExecutionException( "Invalid value for change.sender: ", e );
            }
        }
        if ( mailSender != null && mailSender.getEmail() != null )
        {
            return mailSender;
        }
        else if ( from == null || from.isEmpty() )
        {
            throw new MojoExecutionException( "The <developers> section in your pom should not be empty. "
                + "Add a <developer> entry or set the mailSender parameter." );
        }
        else if ( fromDeveloperId == null )
        {
            final Developer dev = from.get( 0 );
            return new MailSender( dev.getName(), dev.getEmail() );
        }
        else
        {
            for ( Developer developer : from )
            {
                if ( fromDeveloperId.equals( developer.getId() ) )
                {
                    return new MailSender( developer.getName(), developer.getEmail() );
                }
            }
            throw new MojoExecutionException( "Missing developer with id '" + fromDeveloperId
                + "' in the <developers> section in your pom." );
        }
    }

    // ================================
    // announcement-mail accessors
    // ================================

    public List<Object> getBccAddresses()
    {
        return bccAddresses;
    }

    public void setBccAddresses( List<Object> bccAddresses )
    {
        this.bccAddresses = bccAddresses;
    }

    public List<Object> getCcAddresses()
    {
        return ccAddresses;
    }

    public void setCcAddresses( List<Object> ccAddresses )
    {
        this.ccAddresses = ccAddresses;
    }

    public List<Developer> getFrom()
    {
        return from;
    }

    public void setFrom( List<Developer> from )
    {
        this.from = from;
    }

    public String getFromDeveloperId()
    {
        return fromDeveloperId;
    }

    public void setFromDeveloperId( String fromDeveloperId )
    {
        this.fromDeveloperId = fromDeveloperId;
    }

    public MailSender getMailSender()
    {
        return mailSender;
    }

    public void setMailSender( MailSender mailSender )
    {
        this.mailSender = mailSender;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    public String getSmtpHost()
    {
        return smtpHost;
    }

    public void setSmtpHost( String smtpHost )
    {
        this.smtpHost = smtpHost;
    }

    public int getSmtpPort()
    {
        return smtpPort;
    }

    public void setSmtpPort( int smtpPort )
    {
        this.smtpPort = smtpPort;
    }

    public boolean isSslMode()
    {
        return sslMode;
    }

    public void setSslMode( boolean sslMode )
    {
        this.sslMode = sslMode;
    }

    public boolean isStartTls()
    {
        return startTls;
    }

    public void setStartTls( boolean startTls )
    {
        this.startTls = startTls;
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject( String subject )
    {
        this.subject = subject;
    }

    public String getAnnouncementFile()
    {
        return announcementFile;
    }

    public void setAnnouncementFile( String announcementFile )
    {
        this.announcementFile = announcementFile;
    }

    public File getAnnouncementDirectory()
    {
        return announcementDirectory;
    }

    public void setAnnouncementDirectory( File announcementDirectory )
    {
        this.announcementDirectory = announcementDirectory;
    }

    public List<Object> getToAddresses()
    {
        return toAddresses;
    }

    public void setToAddresses( List<Object> toAddresses )
    {
        this.toAddresses = toAddresses;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername( String username )
    {
        this.username = username;
    }
}
