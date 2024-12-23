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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.util.DoxiaUtils;
import org.apache.maven.plugins.changes.issues.AbstractIssuesReportRenderer;
import org.apache.maven.plugins.changes.model.Action;
import org.apache.maven.plugins.changes.model.Component;
import org.apache.maven.plugins.changes.model.DueTo;
import org.apache.maven.plugins.changes.model.Release;

/**
 * Generates a changes report.
 *
 * @version $Id$
 */
public class ChangesReportRenderer extends AbstractIssuesReportRenderer {

    /**
     * The token in {@link #issueLinksPerSystem} denoting the base URL for the issue management.
     */
    private static final String URL_TOKEN = "%URL%";

    /**
     * The token in {@link #issueLinksPerSystem} denoting the issue ID.
     */
    private static final String ISSUE_TOKEN = "%ISSUE%";

    static final String DEFAULT_ISSUE_SYSTEM_KEY = "default";

    private static final String NO_TEAM = "none";

    private final ChangesXML changesXML;

    /**
     * The issue management system to use, for actions that do not specify a system.
     *
     * @since 2.4
     */
    private String system;

    private String team;

    private String url;

    private Map<String, String> issueLinksPerSystem;

    private boolean addActionDate;

    private boolean linkToFeed;

    public ChangesReportRenderer(Sink sink, ResourceBundle bundleName, ChangesXML changesXML) {
        super(sink, bundleName);
        this.issueLinksPerSystem = new HashMap<>();
        this.changesXML = changesXML;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public void setTeam(final String team) {
        this.team = team;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setIssueLinksPerSystem(Map<String, String> issueLinksPerSystem) {
        if (this.issueLinksPerSystem != null && issueLinksPerSystem == null) {
            return;
        }
        this.issueLinksPerSystem = issueLinksPerSystem;
    }

    public void setAddActionDate(boolean addActionDate) {
        this.addActionDate = addActionDate;
    }

    public void setLinkToFeed(boolean generateLinkTofeed) {
        this.linkToFeed = generateLinkTofeed;
    }

    /**
     * Checks whether links to the issues can be generated for the given system.
     *
     * @param system The issue management system
     * @return <code>true</code> if issue links can be generated, <code>false</code> otherwise.
     */
    private boolean canGenerateIssueLinks(String system) {
        if (!this.issueLinksPerSystem.containsKey(system)) {
            return false;
        }
        String issueLink = this.issueLinksPerSystem.get(system);

        // If the issue link entry is blank then no links are possible
        if (StringUtils.isBlank(issueLink)) {
            return false;
        }

        // If the %URL% token is used then the issue management system URL must be set.
        if (issueLink.contains(URL_TOKEN) && StringUtils.isBlank(url)) {
            return false;
        }
        return true;
    }

    @Override
    protected void renderBody() {
        constructReleaseHistory();
        constructReleases();
    }

    @Override
    public String getTitle() {
        String title = changesXML.getTitle();
        if (title == null) {
            title = bundle.getString("report.issues.header");
        }
        return title;
    }

    /**
     * Constructs table row for specified action with all calculated content (e.g. issue link).
     *
     * @param action Action to generate content for
     */
    private void constructAction(Action action) {
        sink.tableRow();

        sinkShowTypeIcon(action.getType());

        sink.tableCell();

        String actionDescription = action.getAction();

        text(actionDescription);

        // no null check needed classes from modello return a new ArrayList
        if (StringUtils.isNotEmpty(action.getIssue())
                || (!action.getFixedIssues().isEmpty())) {
            if (StringUtils.isNotBlank(actionDescription) && !actionDescription.endsWith(".")) {
                text(".");
            }
            text(" " + bundle.getString("report.changes.text.fixes") + " ");

            // Try to get the issue management system specified in the changes.xml file
            String system = action.getSystem();
            // Try to get the issue management system configured in the POM
            if (StringUtils.isEmpty(system)) {
                system = this.system;
            }
            // Use the default issue management system
            if (StringUtils.isEmpty(system)) {
                system = DEFAULT_ISSUE_SYSTEM_KEY;
            }
            if (!canGenerateIssueLinks(system)) {
                constructIssueText(action.getIssue(), action.getFixedIssues());
            } else {
                constructIssueLink(action.getIssue(), system, action.getFixedIssues());
            }
            text(".");
        }

        if (!action.getDueTos().isEmpty()) {
            constructDueTo(action);
        }

        sink.tableCell_();

        if (NO_TEAM.equals(team) || action.getDev() == null || action.getDev().isEmpty()) {
            sinkCell(action.getDev());
        } else {
            sinkCellLink(action.getDev(), team + "#" + action.getDev());
        }

        if (addActionDate) {
            sinkCell(action.getDate());
        }

        sink.tableRow_();
    }

    /**
     * Construct a text or link that mention the people that helped with an action.
     *
     * @param action The action that was done
     */
    private void constructDueTo(Action action) {

        text(" " + bundle.getString("report.changes.text.thanx") + " ");
        int i = 0;
        for (DueTo dueTo : action.getDueTos()) {
            i++;

            if (StringUtils.isNotEmpty(dueTo.getEmail())) {
                String text = dueTo.getName();
                link("mailto:" + dueTo.getEmail(), text);
            } else {
                text(dueTo.getName());
            }

            if (i < action.getDueTos().size()) {
                text(", ");
            }
        }

        text(".");
    }

    /**
     * Construct links to the issues that were solved by an action.
     *
     * @param issue The issue specified by attributes
     * @param system The issue management system
     * @param fixes The List of issues specified as fixes elements
     */
    private void constructIssueLink(String issue, String system, List<String> fixes) {
        if (StringUtils.isNotEmpty(issue)) {
            link(parseIssueLink(issue, system), issue);
            if (!fixes.isEmpty()) {
                text(", ");
            }
        }

        for (Iterator<String> iterator = fixes.iterator(); iterator.hasNext(); ) {
            String currentIssueId = iterator.next();
            if (StringUtils.isNotEmpty(currentIssueId)) {
                link(parseIssueLink(currentIssueId, system), currentIssueId);
            }

            if (iterator.hasNext()) {
                text(", ");
            }
        }
    }

    /**
     * Construct a text that references (but does not link to) the issues that were solved by an action.
     *
     * @param issue The issue specified by attributes
     * @param fixes The List of issues specified as fixes elements
     */
    private void constructIssueText(String issue, List<String> fixes) {
        if (StringUtils.isNotEmpty(issue)) {
            text(issue);

            if (!fixes.isEmpty()) {
                text(", ");
            }
        }

        for (Iterator<String> iterator = fixes.iterator(); iterator.hasNext(); ) {
            String currentIssueId = iterator.next();
            if (StringUtils.isNotEmpty(currentIssueId)) {
                text(currentIssueId);
            }

            if (iterator.hasNext()) {
                text(", ");
            }
        }
    }

    private void constructReleaseHistory() {
        startSection(bundle.getString("report.changes.label.releasehistory"));

        startTable();

        tableHeader(new String[] {
            bundle.getString("report.issues.label.fixVersion"),
            bundle.getString("report.changes.label.releaseDate"),
            bundle.getString("report.changes.label.releaseDescription")
        });

        for (Release release : changesXML.getReleaseList()) {
            sink.tableRow();
            sinkCellLink(release.getVersion(), "#" + DoxiaUtils.encodeId(release.getVersion()));
            sinkCell(release.getDateRelease());
            sinkCell(release.getDescription());
            sink.tableRow_();
        }

        endTable();

        // MCHANGES-46
        if (linkToFeed) {
            sink.paragraph();
            text(bundle.getString("report.changes.text.rssfeed"));
            sink.nonBreakingSpace();
            sink.link("changes.rss");
            sinkFigure("images/rss.png", "rss feed");
            sink.link_();
            sink.paragraph_();
        }

        endSection();
    }

    /**
     * Constructs document sections for each of specified releases.
     */
    private void constructReleases() {
        for (Release release : changesXML.getReleaseList()) {
            constructRelease(release);
        }
    }

    /**
     * Constructs document section for specified release.
     *
     * @param release Release to create document section for
     */
    private void constructRelease(Release release) {

        final String date = (release.getDateRelease() == null) ? "" : " \u2013 " + release.getDateRelease();

        startSection(
                bundle.getString("report.changes.label.release") + " " + release.getVersion() + date,
                DoxiaUtils.encodeId(release.getVersion()));

        if (isReleaseEmpty(release)) {
            sink.paragraph();
            text(bundle.getString("report.changes.text.no.changes"));
            sink.paragraph_();
        } else {
            startTable();

            sink.tableRow();
            tableHeaderCell(bundle.getString("report.issues.label.type"));
            tableHeaderCell(bundle.getString("report.issues.label.summary"));
            tableHeaderCell(bundle.getString("report.issues.label.assignee"));
            if (addActionDate) {
                tableHeaderCell(bundle.getString("report.issues.label.updated"));
            }
            sink.tableRow_();

            for (Action action : release.getActions()) {
                constructAction(action);
            }

            for (Component component : release.getComponents()) {
                constructComponent(component);
            }

            endTable();
        }

        endSection();
    }

    /**
     * Constructs table rows for specified release component. It will create header row for component name and action
     * rows for all component issues.
     *
     * @param component Release component to generate content for.
     */
    private void constructComponent(Component component) {
        if (!component.getActions().isEmpty()) {
            sink.tableRow();

            sink.tableHeaderCell();
            sink.tableHeaderCell_();

            sink.tableHeaderCell();
            text(component.getName());
            sink.tableHeaderCell_();

            sink.tableHeaderCell();
            sink.tableHeaderCell_();

            if (addActionDate) {
                sink.tableHeaderCell();
                sink.tableHeaderCell_();
            }

            sink.tableRow_();

            for (Action action : component.getActions()) {
                constructAction(action);
            }
        }
    }

    /**
     * Checks if specified release contains own issues or issues inside the child components.
     *
     * @param release Release to check
     * @return <code>true</code> if release doesn't contain any issues, <code>false</code> otherwise
     */
    private boolean isReleaseEmpty(Release release) {
        if (!release.getActions().isEmpty()) {
            return false;
        }

        for (Component component : release.getComponents()) {
            if (!component.getActions().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Replace tokens in the issue link template with the real values.
     *
     * @param issue The issue identifier
     * @param system The issue management system
     * @return An interpolated issue link
     */
    private String parseIssueLink(String issue, String system) {
        String parseLink;
        String issueLink = this.issueLinksPerSystem.get(system);
        parseLink = issueLink.replaceFirst(ISSUE_TOKEN, issue);
        if (parseLink.contains(URL_TOKEN)) {
            String url = this.url.substring(0, this.url.lastIndexOf("/"));
            parseLink = parseLink.replaceFirst(URL_TOKEN, url);
        }

        return parseLink;
    }
}
