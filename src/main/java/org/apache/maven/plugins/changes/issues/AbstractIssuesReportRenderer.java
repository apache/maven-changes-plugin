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
package org.apache.maven.plugins.changes.issues;

import java.util.ResourceBundle;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.reporting.AbstractMavenReportRenderer;

/**
 * An abstract super class that helps when generating a report on issues.
 *
 * @author Dennis Lundberg
 * @version $Id$
 * @since 2.4
 */
public abstract class AbstractIssuesReportRenderer extends AbstractMavenReportRenderer {

    protected final ResourceBundle bundle;

    protected AbstractIssuesReportRenderer(Sink sink, ResourceBundle bundle) {
        super(sink);
        this.bundle = bundle;
    }

    public String getAuthor() {
        return null;
    }

    @Override
    public String getTitle() {
        return bundle.getString("report.issues.header");
    }

    @Override
    public void render() {

        String title = getTitle();
        sink.head();
        sink.title();
        text(title);
        sink.title_();

        String author = getAuthor();
        if (author != null) {
            sink.author();
            text(author);
            sink.author_();
        }

        sink.head_();

        sink.body();
        startSection(title);

        renderBody();

        endSection();
        sink.body_();
        sink.flush();
        sink.close();
    }

    protected void sinkCell(String text) {
        sink.tableCell();

        if (text != null) {
            text(text);
        } else {
            sink.nonBreakingSpace();
        }

        sink.tableCell_();
    }

    protected void sinkCellLink(String text, String link) {
        sink.tableCell();
        link(link, text);
        sink.tableCell_();
    }

    protected void sinkShowTypeText(String type) {
        String key;

        if (type == null) {
            key = "unknown";
        } else if (type.equals("fix")) {
            key = "fix";
        } else if (type.equals("update")) {
            key = "update";
        } else if (type.equals("add")) {
            key = "add";
        } else if (type.equals("remove")) {
            key = "remove";
        } else {
            key = "unknown";
        }

        sink.tableCell();
        text(bundle.getString("report.changes.label.type." + key));
        sink.tableCell_();
    }
}
