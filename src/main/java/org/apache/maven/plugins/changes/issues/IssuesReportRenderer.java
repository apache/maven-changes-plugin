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
import java.text.SimpleDateFormat;
import java.util.List;
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
public class IssuesReportRenderer extends AbstractIssuesReportRenderer {
    /**
     * Fallback value that is used if date field are not available.
     */
    private static final String NOT_AVAILABLE = "n/a";

    /**
     * Holds the id:s for the columns to include in the report, in the order that they should appear in the report.
     */
    private final List<Integer> columns;

    private final List<Issue> issueList;

    /**
     * @param includedColumns The id:s of the columns to include in the report
     */
    public IssuesReportRenderer(
            Sink sink, ResourceBundle bundle, List<Integer> includedColumns, List<Issue> issueList) {
        super(sink, bundle);
        this.columns = includedColumns;
        this.issueList = issueList;
    }

    @Override
    public void renderBody() {
        if (issueList == null || issueList.isEmpty()) {
            paragraph(bundle.getString("report.issues.error"));
        } else {
            startTable();
            constructHeaderRow();
            constructDetailRows();
            endTable();
        }
    }

    private void constructHeaderRow() {

        sink.tableRow();

        for (int column : columns) {
            switch (column) {
                case IssuesReportHelper.COLUMN_ASSIGNEE:
                    tableHeaderCell(bundle.getString("report.issues.label.assignee"));
                    break;

                case IssuesReportHelper.COLUMN_COMPONENT:
                    tableHeaderCell(bundle.getString("report.issues.label.component"));
                    break;

                case IssuesReportHelper.COLUMN_CREATED:
                    tableHeaderCell(bundle.getString("report.issues.label.created"));
                    break;

                case IssuesReportHelper.COLUMN_FIX_VERSION:
                    tableHeaderCell(bundle.getString("report.issues.label.fixVersion"));
                    break;

                case IssuesReportHelper.COLUMN_ID:
                    tableHeaderCell(bundle.getString("report.issues.label.id"));
                    break;

                case IssuesReportHelper.COLUMN_KEY:
                    tableHeaderCell(bundle.getString("report.issues.label.key"));
                    break;

                case IssuesReportHelper.COLUMN_PRIORITY:
                    tableHeaderCell(bundle.getString("report.issues.label.priority"));
                    break;

                case IssuesReportHelper.COLUMN_REPORTER:
                    tableHeaderCell(bundle.getString("report.issues.label.reporter"));
                    break;

                case IssuesReportHelper.COLUMN_RESOLUTION:
                    tableHeaderCell(bundle.getString("report.issues.label.resolution"));
                    break;

                case IssuesReportHelper.COLUMN_STATUS:
                    tableHeaderCell(bundle.getString("report.issues.label.status"));
                    break;

                case IssuesReportHelper.COLUMN_SUMMARY:
                    tableHeaderCell(bundle.getString("report.issues.label.summary"));
                    break;

                case IssuesReportHelper.COLUMN_TYPE:
                    tableHeaderCell(bundle.getString("report.issues.label.type"));
                    break;

                case IssuesReportHelper.COLUMN_UPDATED:
                    tableHeaderCell(bundle.getString("report.issues.label.updated"));
                    break;

                case IssuesReportHelper.COLUMN_VERSION:
                    tableHeaderCell(bundle.getString("report.issues.label.version"));
                    break;

                default:
                    // Do not add a header for this column
                    break;
            }
        }

        sink.tableRow_();
    }

    private void constructDetailRows() {

        // Always use the international date format as recommended by the W3C:
        // http://www.w3.org/QA/Tips/iso-date
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        for (Issue issue : issueList) {

            sink.tableRow();

            for (int column : columns) {
                switch (column) {
                    case IssuesReportHelper.COLUMN_ASSIGNEE:
                        sinkCell(issue.getAssignee());
                        break;

                    case IssuesReportHelper.COLUMN_COMPONENT:
                        sinkCell(IssuesReportHelper.printValues(issue.getComponents()));
                        break;

                    case IssuesReportHelper.COLUMN_CREATED:
                        String created = NOT_AVAILABLE;
                        if (issue.getCreated() != null) {
                            created = df.format(issue.getCreated());
                        }
                        sinkCell(created);
                        break;

                    case IssuesReportHelper.COLUMN_FIX_VERSION:
                        sinkCell(IssuesReportHelper.printValues(issue.getFixVersions()));
                        break;

                    case IssuesReportHelper.COLUMN_ID:
                        sinkCellLink(issue.getId(), issue.getLink());
                        break;

                    case IssuesReportHelper.COLUMN_KEY:
                        sinkCellLink(issue.getKey(), issue.getLink());
                        break;

                    case IssuesReportHelper.COLUMN_PRIORITY:
                        sinkCell(issue.getPriority());
                        break;

                    case IssuesReportHelper.COLUMN_REPORTER:
                        sinkCell(issue.getReporter());
                        break;

                    case IssuesReportHelper.COLUMN_RESOLUTION:
                        sinkCell(issue.getResolution());
                        break;

                    case IssuesReportHelper.COLUMN_STATUS:
                        sinkCell(issue.getStatus());
                        break;

                    case IssuesReportHelper.COLUMN_SUMMARY:
                        sinkCell(issue.getSummary());
                        break;

                    case IssuesReportHelper.COLUMN_TYPE:
                        sinkCell(issue.getType());
                        break;

                    case IssuesReportHelper.COLUMN_UPDATED:
                        String updated = NOT_AVAILABLE;
                        if (issue.getUpdated() != null) {
                            updated = df.format(issue.getUpdated());
                        }
                        sinkCell(updated);
                        break;

                    case IssuesReportHelper.COLUMN_VERSION:
                        sinkCell(issue.getVersion());
                        break;

                    default:
                        // Do not add this column
                        break;
                }
            }

            sink.tableRow_();
        }
    }
}
