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
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManager;
import org.sonatype.aether.util.DefaultRepositorySystemSession;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @version $Id$
 */
public class JiraUnicodeTestCase extends AbstractMojoTestCase {
    /*
     * Something in Doxia escapes all non-Ascii even when the charset is UTF-8. This test will fail if that ever
     * changes.
     */
    private static final String TEST_TURTLES = "&#x6d77;&#x9f9f;&#x4e00;&#x8def;&#x4e0b;&#x8dcc;&#x3002;";

    public void testUnicodeReport() throws Exception {

        File pom = new File(getBasedir(), "/src/test/unit/jira-plugin-config.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        JiraMojo mojo = (JiraMojo) lookupMojo("jira-report", pom);
        MavenProject project = new JiraUnicodeTestProjectStub();
        MavenSession session = newMavenSession(project);
        DefaultRepositorySystemSession repoSession = (DefaultRepositorySystemSession) session.getRepositorySession();
        repoSession.setLocalRepositoryManager(new SimpleLocalRepositoryManager("target/local-repo"));
        setVariableValueToObject(mojo, "project", project);
        setVariableValueToObject(mojo, "mavenSession", session);
        String jiraXml;
        try (InputStream testJiraXmlStream =
                JiraUnicodeTestCase.class.getResourceAsStream("unicode-jira-results.xml")) {
            jiraXml = IOUtils.toString(testJiraXmlStream, UTF_8);
        }

        MockJiraDownloader mockDownloader = new MockJiraDownloader();
        mockDownloader.setJiraXml(jiraXml);
        mojo.setMockDownloader(mockDownloader);
        File outputDir = new File("target/jira-test-output");
        outputDir.mkdirs();
        mojo.setReportOutputDirectory(outputDir);
        mojo.execute();
        String reportHtml = FileUtils.readFileToString(new File(outputDir, "jira-report.html"), "utf-8");
        int turtleIndex = reportHtml.indexOf(TEST_TURTLES);
        assertTrue(turtleIndex >= 0);
    }
}
