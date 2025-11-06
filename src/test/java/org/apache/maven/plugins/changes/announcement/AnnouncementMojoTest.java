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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Olivier Lamy
 * @version $Id$
 */
@MojoTest
public class AnnouncementMojoTest {
    @InjectMojo(goal = "announcement-generate", pom = "src/test/unit/plugin-config.xml")
    @MojoParameter(name = "xmlPath", value = "src/test/unit/announce-changes.xml")
    @MojoParameter(name = "announcementDirectory", value = "target/test")
    @MojoParameter(name = "version", value = "1.1")
    @MojoParameter(name = "template", value = "announcement.vm")
    @MojoParameter(
            name = "templateDirectory",
            value = "src/main/resources/org/apache/maven/plugins/changes/announcement/")
    @MojoParameter(name = "introduction", value = "Nice library")
    @Test
    public void testAnnounceGeneration(AnnouncementMojo mojo) throws Exception {
        File announcementDirectory = prepareAnnouncementDirectory();
        mojo.execute();

        String result =
                new String(Files.readAllBytes(announcementDirectory.toPath().resolve("announcement.vm")));

        assertContains("Nice library", result);
        assertContains("Changes in this version include:", result);
        assertContains("New features:", result);
        assertContains("o Added additional documentation on how to configure the plugin.", result);
        assertContains("Fixed Bugs:", result);
        assertContains("o Enable retrieving component-specific issues.  Issue: MCHANGES-88.", result);
        assertContains("Changes:", result);
        assertContains("o Handle different issue systems.", result);
        assertContains("o Updated dependencies.", result);
        assertContains("Removed:", result);
        assertContains("o The element type \" link \" must be terminated by the matching end-tag.", result);
        assertContains("Deleted the erroneous code.", result);
    }

    private File prepareAnnouncementDirectory() throws IOException {
        File announcementDirectory = new File(getBasedir(), "target/test");

        if (announcementDirectory.exists()) {
            FileUtils.deleteDirectory(announcementDirectory);
            announcementDirectory.mkdirs();
        } else {
            announcementDirectory.mkdirs();
        }
        return announcementDirectory;
    }

    private static void assertContains(String content, String announce) {
        assertTrue(announce.indexOf(content) > 0);
    }
}
