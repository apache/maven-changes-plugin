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

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugins.changes.issues.Issue;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class JiraUnicodeTestCase extends AbstractMojoTestCase {
    /*
     * Something in Doxia escapes all non-Ascii even when the charset is UTF-8. This test will fail if that ever
     * changes.
     */
    private static final String TEST_TURTLES = "&#x6d77;&#x9f9f;&#x4e00;&#x8def;&#x4e0b;&#x8dcc;&#x3002;";

    @Test
    public void testUnicodeReport() throws Exception {

        File pom = new File(getBasedir(), "/src/test/unit/jira-plugin-config.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        JiraChangesReport mojo = lookupMojo("jira-changes", pom);
        MavenProject project = new JiraUnicodeTestProjectStub();
        MavenSession session = newMavenSession(project);

        RepositorySystem repositorySystem = lookup(RepositorySystem.class);

        DefaultRepositorySystemSession repositorySystemSession =
                (DefaultRepositorySystemSession) session.getRepositorySession();
        repositorySystemSession.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(
                repositorySystemSession, new LocalRepository(System.getProperty("localRepository"))));

        // Test need to download a maven-fluido-skin if not present in local repo
        List<RemoteRepository> remoteRepositories = repositorySystem.newResolutionRepositories(
                repositorySystemSession,
                Collections.singletonList(
                        new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2")
                                .build()));

        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "reactorProjects", Collections.singletonList(project));
        setVariableValueToObject(mojo, "repoSession", repositorySystemSession);
        setVariableValueToObject(mojo, "remoteProjectRepositories", remoteRepositories);

        setVariableValueToObject(mojo, "siteDirectory", new File("non-existing"));
        setVariableValueToObject(mojo, "mavenSession", session);
        setVariableValueToObject(mojo, "mojoExecution", new MojoExecution(new Plugin(), "jira-changes", "default"));

        RestJiraDownloader mock = mock(RestJiraDownloader.class);
        Issue issue = new Issue();
        issue.setKey("PCSUNIT-2");
        issue.setLink("http://pcsjira.slg.gr/browse/PCSUNIT-2");
        issue.setSummary("海龟一路下跌。 Απεικόνιση σε EXCEL των data των φορμών. Περίπτωση με πολλά blocks");
        issue.setStatus("Closed");
        issue.setResolution("Fixed");
        issue.setAssignee("Nikolaos Stais");
        when(mock.getIssueList()).thenReturn(Collections.singletonList(issue));

        mojo.setMockDownloader(mock);
        File outputDir = new File("target/jira-test-output");
        outputDir.mkdirs();
        mojo.setReportOutputDirectory(outputDir);
        mojo.execute();
        String reportHtml = FileUtils.readFileToString(new File(outputDir, "jira-changes.html"), "utf-8");
        int turtleIndex = reportHtml.indexOf(TEST_TURTLES);
        assertTrue(turtleIndex >= 0);
    }
}
