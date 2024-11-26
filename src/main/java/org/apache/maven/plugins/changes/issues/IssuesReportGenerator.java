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

import java.text.DateFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.maven.doxia.sink.Sink;

/**
 * Generates a report on issues.
 *
 * @author Noriko Kinugasa
 * @author Dennis Lundberg
 * @version $Id$
 * @since 2.4
 */
public class IssuesReportGenerator extends AbstractIssuesReportGenerator {
    /**
     * Fallback value that is used if date field are not available.
     */
    private static final String NOT_AVAILABLE = "n/a";

    /**
     * Holds the id:s for the columns to include in the report, in the order that they should appear in the report.
     */
    private int[] columns;

    /**
     * @param includedColumns The id:s of the columns to include in the report
     */
    public IssuesReportGenerator(int[] includedColumns) {
        this.columns = includedColumns;
    }

    public void doGenerateEmptyReport(ResourceBundle bundle, Sink sink) {
        sinkBeginReport(sink, bundle);

        sink.paragraph();

        sink.text(bundle.getString("report.issues.error"));

        sink.paragraph_();

        sinkEndReport(sink);
    }

    public void doGenerateReport(ResourceBundle bundle, Sink sink, List<Issue> issueList) {
        sinkBeginReport(sink, bundle);

        constructHeaderRow(sink, issueList, bundle);

        // Always use the international date format as recommended by the W3C:
        // http://www.w3.org/QA/Tips/iso-date
        // This date format is used in the Swedish locale.
        constructDetailRows(sink, issueList, bundle, new Locale("sv"));

        sinkEndReport(sink);
    }

    private void constructHeaderRow(Sink sink, List<Issue> issueList, ResourceBundle bundle) {
        if (issueList == null) {
            return;
        }

        sink.table();
        sink.tableRows();

        sink.tableRow();

        for (int column : columns) {
            switch (column) {
                case IssuesReportHelper.COLUMN_ASSIGNEE:
                    sinkHeader(sink, bundle.getString("report.issues.label.assignee"));
                    break;

                case IssuesReportHelper.COLUMN_COMPONENT:
                    sinkHeader(sink, bundle.getString("report.issues.label.component"));
                    break;

                case IssuesReportHelper.COLUMN_CREATED:
                    sinkHeader(sink, bundle.getString("report.issues.label.created"));
                    break;

                case IssuesReportHelper.COLUMN_FIX_VERSION:
                    sinkHeader(sink, bundle.getString("report.issues.label.fixVersion"));
                    break;

                case IssuesReportHelper.COLUMN_ID:
                    sinkHeader(sink, bundle.getString("report.issues.label.id"));
                    break;

                case IssuesReportHelper.COLUMN_KEY:
                    sinkHeader(sink, bundle.getString("report.issues.label.key"));
                    break;

                case IssuesReportHelper.COLUMN_PRIORITY:
                    sinkHeader(sink, bundle.getString("report.issues.label.priority"));
                    break;

                case IssuesReportHelper.COLUMN_REPORTER:
                    sinkHeader(sink, bundle.getString("report.issues.label.reporter"));
                    break;

                case IssuesReportHelper.COLUMN_RESOLUTION:
                    sinkHeader(sink, bundle.getString("report.issues.label.resolution"));
                    break;

                case IssuesReportHelper.COLUMN_STATUS:
                    sinkHeader(sink, bundle.getString("report.issues.label.status"));
                    break;

                case IssuesReportHelper.COLUMN_SUMMARY:
                    sinkHeader(sink, bundle.getString("report.issues.label.summary"));
                    break;

                case IssuesReportHelper.COLUMN_TYPE:
                    sinkHeader(sink, bundle.getString("report.issues.label.type"));
                    break;

                case IssuesReportHelper.COLUMN_UPDATED:
                    sinkHeader(sink, bundle.getString("report.issues.label.updated"));
                    break;

                case IssuesReportHelper.COLUMN_VERSION:
                    sinkHeader(sink, bundle.getString("report.issues.label.version"));
                    break;

                default:
                    // Do not add a header for this column
                    break;
            }
        }

        sink.tableRow_();
    }

    private void constructDetailRows(Sink sink, List<Issue> issueList, ResourceBundle bundle, Locale locale) {
        if (issueList == null) {
            return;
        }

        for (Issue issue : issueList) {
            // Use a DateFormat based on the Locale
            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, locale);

            sink.tableRow();

            for (int column : columns) {
                switch (column) {
                    case IssuesReportHelper.COLUMN_ASSIGNEE:
                        sinkCell(sink, issue.getAssignee());
                        break;

                    case IssuesReportHelper.COLUMN_COMPONENT:
                        sinkCell(sink, IssuesReportHelper.printValues(issue.getComponents()));
                        break;

                    case IssuesReportHelper.COLUMN_CREATED:
                        String created = NOT_AVAILABLE;
                        if (issue.getCreated() != null) {
                            created = df.format(issue.getCreated());
                        }
                        sinkCell(sink, created);
                        break;

                    case IssuesReportHelper.COLUMN_FIX_VERSION:
                        sinkCell(sink, IssuesReportHelper.printValues(issue.getFixVersions()));
                        break;

                    case IssuesReportHelper.COLUMN_ID:
                        sink.tableCell();
                        sink.link(issue.getLink());
                        sink.text(issue.getId());
                        sink.link_();
                        sink.tableCell_();
                        break;

                    case IssuesReportHelper.COLUMN_KEY:
                        sink.tableCell();
                        sink.link(issue.getLink());
                        sink.text(issue.getKey());
                        sink.link_();
                        sink.tableCell_();
                        break;

                    case IssuesReportHelper.COLUMN_PRIORITY:
                        sinkCell(sink, issue.getPriority());
                        break;

                    case IssuesReportHelper.COLUMN_REPORTER:
                        sinkCell(sink, issue.getReporter());
                        break;

                    case IssuesReportHelper.COLUMN_RESOLUTION:
                        sinkCell(sink, issue.getResolution());
                        break;

                    case IssuesReportHelper.COLUMN_STATUS:
                        sinkCell(sink, issue.getStatus());
                        break;

                    case IssuesReportHelper.COLUMN_SUMMARY:
                        sinkCell(sink, issue.getSummary());
                        break;

                    case IssuesReportHelper.COLUMN_TYPE:
                        sinkCell(sink, issue.getType());
                        break;

                    case IssuesReportHelper.COLUMN_UPDATED:
                        String updated = NOT_AVAILABLE;
                        if (issue.getUpdated() != null) {
                            updated = df.format(issue.getUpdated());
                        }
                        sinkCell(sink, updated);
                        break;

                    case IssuesReportHelper.COLUMN_VERSION:
                        sinkCell(sink, issue.getVersion());
                        break;

                    default:
                        // Do not add this column
                        break;
                }
            }

            sink.tableRow_();
        }
        sink.tableRows_();
        sink.table_();
    }
}
