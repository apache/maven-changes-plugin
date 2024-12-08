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
package org.apache.maven.plugins.changes;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.plugins.changes.model.Action;
import org.apache.maven.plugins.changes.model.Release;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Olivier Lamy
 * @since 27 juil. 2008
 * @version $Id$
 */
public class ChangesXMLTest {

    private String getBasedir() {
        final String path = System.getProperty("basedir");
        return path != null ? path : new File("").getAbsolutePath();
    }

    @Test
    public void testParseChangesFile() {
        File changesFile = new File(getBasedir() + "/src/test/unit/changes.xml");
        ChangesXML changesXML = new ChangesXML(changesFile, new SilentLog());
        assertNotNull(changesXML.getChangesDocument());
        assertEquals("Changes report Project", changesXML.getTitle());

        List<Release> releases = changesXML.getReleaseList();
        assertEquals(3, releases.size());
        for (Release release : releases) {
            if ("1.0".equals(release.getVersion())) {
                Action action = release.getActions().get(0);
                assertEquals(2, action.getFixedIssues().size());
                assertEquals("JIRA-XXX", action.getFixedIssues().get(0));
                assertEquals("JIRA-YYY", action.getFixedIssues().get(1));
                assertEquals(2, action.getDueTos().size());
                assertEquals("John Doe", action.getDueTos().get(0).getName());
                assertEquals("john@doe.com", action.getDueTos().get(0).getEmail());
                assertEquals("John Doe", action.getDueTos().get(1).getName());
                assertEquals("", action.getDueTos().get(1).getEmail());
                assertEquals("Uploaded documentation on how to use the plugin.", action.getAction());
            }
            if ("2.0".equals(release.getVersion())) {
                Action action = release.getActions().get(0);
                assertEquals(2, action.getDueTos().size());
                assertEquals("John Doe", action.getDueTos().get(0).getName());
                assertEquals("", action.getDueTos().get(0).getEmail());
                assertEquals("John Doe", action.getDueTos().get(1).getName());
                assertEquals("john@doe.com", action.getDueTos().get(1).getEmail());
            }
        }
    }

    @Test
    public void testParseInvalidChangesFile() {
        File changesFile = new File(getBasedir() + "/src/test/unit/invalid-changes.xml");

        try {
            new ChangesXML(changesFile, new SilentLog());
            fail("Should have thrown a ChangesXMLRuntimeException due to the invalid changes.xml file");
        } catch (ChangesXMLRuntimeException e) {
            assertEquals("An error occurred when parsing the changes.xml file", e.getMessage());
        }
    }
}
