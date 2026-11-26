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
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.maven.doxia.sink.impl.Xhtml5BaseSink;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders the changes report to a Doxia sink and checks the generated markup.
 */
public class ChangesReportRendererTest {

    private static String render(String changesXml, String system, String url, Map<String, String> links) {
        StringWriter out = new StringWriter();
        Xhtml5BaseSink sink = new Xhtml5BaseSink(out);
        ChangesXML xml = new ChangesXML(new File(changesXml), new SystemStreamLog());
        ChangesReportRenderer renderer =
                new ChangesReportRenderer(sink, ResourceBundle.getBundle("changes-report", Locale.ENGLISH), xml);
        renderer.setIssueLinksPerSystem(links);
        renderer.setSystem(system);
        renderer.setUrl(url);
        renderer.setTeam("team.html");
        renderer.setAddActionDate(true);
        renderer.setEscapeText(true);
        renderer.render();
        return out.toString();
    }

    @Test
    public void testIssueLinksPerSystemWithConfiguredSystem() {
        Map<String, String> links = new HashMap<>();
        links.put("jira", "http://myjira/browse/%ISSUE%");
        links.put("qc", "http://localhost/%ISSUE%");

        String html = render("src/test/unit/report/system-changes.xml", "jira", "http://localhost", links);

        assertTrue(html.contains("href=\"http://myjira/browse/MCHANGES-88\""), html);
        assertTrue(html.contains("href=\"http://localhost/1212\""), html);
        assertTrue(html.contains("2008-01-01"), "action date missing");
    }

    @Test
    public void testIssueLinksPerSystemWithoutIssueManagement() {
        Map<String, String> links = new HashMap<>();
        links.put("default", "http://myjira/browse/%ISSUE%");
        links.put("qc", "http://localhost/%ISSUE%");
        links.put("redmine", "http://www.redmine.org/show/issue/%ISSUE%");

        String html = render("src/test/unit/report/system-changes.xml", null, null, links);

        assertTrue(html.contains("href=\"http://myjira/browse/MCHANGES-88\""), html);
        assertTrue(html.contains("href=\"http://localhost/1212\""), html);
        assertTrue(html.contains("href=\"http://www.redmine.org/show/issue/88\""), html);
        assertTrue(html.contains("2008-01-01"), "action date missing");
    }

    @Test
    public void testNoLinkForSystemWithoutTemplate() {
        String html = render("src/test/unit/report/system-changes.xml", null, null, new HashMap<>());

        assertFalse(html.contains("href=\"http://myjira"), html);
        assertFalse(html.contains("href=\"http://localhost/1212\""), html);
        assertTrue(html.contains("MCHANGES-88"), html);
    }
}
