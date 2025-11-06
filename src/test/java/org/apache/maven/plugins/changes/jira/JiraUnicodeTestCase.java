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

import javax.inject.Inject;

import java.io.File;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugins.changes.issues.Issue;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
@MojoTest
public class JiraUnicodeTestCase {
    /*
     * Something in Doxia escapes all non-Ascii even when the charset is UTF-8. This test will fail if that ever
     * changes.
     */
    private static final String TEST_TURTLES = "&#x6d77;&#x9f9f;&#x4e00;&#x8def;&#x4e0b;&#x8dcc;&#x3002;";

    /**
     * The project to test.
     */
    @Inject
    private MavenProject testMavenProject;

    @Inject
    private MavenSession mavenSession;

    @Inject
    private DefaultRepositorySystemSessionFactory repoSessionFactory;

    @Inject
    private MojoExecution mojoExecution;

    @BeforeEach
    public void setUp() throws Exception {
        // prepare realistic repository session
        ArtifactRepository localRepo = mock(ArtifactRepository.class);
        when(localRepo.getBasedir()).thenReturn(new File(System.getProperty("localRepository")).getAbsolutePath());

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setLocalRepository(localRepo);

        RemoteRepository centralRepo =
                new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build();

        DefaultRepositorySystemSession systemSession = repoSessionFactory.newRepositorySession(request);
        when(mavenSession.getRepositorySession()).thenReturn(systemSession);
        when(testMavenProject.getRemoteProjectRepositories()).thenReturn(Collections.singletonList(centralRepo));
        when(mojoExecution.getPlugin()).thenReturn(new Plugin());
    }

    @InjectMojo(goal = "jira-changes", pom = "src/test/unit/jira-plugin-config.xml")
    @MojoParameter(name = "siteDirectory", value = "non-existing" )
    @Test
    public void testUnicodeReport(JiraChangesReport mojo) throws Exception {
        RestJiraDownloader mock = mock(RestJiraDownloader.class);
        when(mock.getIssueList()).thenReturn(Collections.singletonList(testIssue()));
        mojo.setMockDownloader(mock);

        File outputDir = new File("target/jira-test-output");
        mojo.setReportOutputDirectory(outputDir);

        mojo.execute();

        String reportHtml = FileUtils.readFileToString(new File(outputDir, "jira-changes.html"), "utf-8");
        int turtleIndex = reportHtml.indexOf(TEST_TURTLES);
        assertTrue(turtleIndex >= 0);
    }

    private Issue testIssue() {
        Issue issue = new Issue();
        issue.setKey("PCSUNIT-2");
        issue.setLink("http://pcsjira.slg.gr/browse/PCSUNIT-2");
        issue.setSummary("海龟一路下跌。 Απεικόνιση σε EXCEL των data των φορμών. Περίπτωση με πολλά blocks");
        issue.setStatus("Closed");
        issue.setResolution("Fixed");
        issue.setAssignee("Nikolaos Stais");
        return issue;
    }
}
