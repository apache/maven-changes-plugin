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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;

/**
 * JIRA 3.x way of constructing a search query based on URL parameters.
 *
 * @author ton.swieb@finalist.com
 * @version $Id$
 * @since 2.8
 */
public class ParameterQueryBuilder implements JiraQueryBuilder {
    private String filter = "";

    /** Log for debug output. */
    private Log log;

    private StringBuilder query = new StringBuilder();

    /** Mapping containing all allowed JIRA priority values. */
    private final Map<String, String> priorityMap = new HashMap<>(8);

    /** Mapping containing all allowed JIRA resolution values. */
    private final Map<String, String> resolutionMap = new HashMap<>(8);

    /** Mapping containing all allowed JIRA status values. */
    private final Map<String, String> statusMap = new HashMap<>(8);

    /** Mapping containing all allowed JIRA type values. */
    private final Map<String, String> typeMap = new HashMap<>(8);

    public ParameterQueryBuilder(Log log) {
        this.log = log;

        priorityMap.put("Blocker", "1");
        priorityMap.put("Critical", "2");
        priorityMap.put("Major", "3");
        priorityMap.put("Minor", "4");
        priorityMap.put("Trivial", "5");

        resolutionMap.put("Unresolved", "-1");
        resolutionMap.put("Fixed", "1");
        resolutionMap.put("Won't Fix", "2");
        resolutionMap.put("Duplicate", "3");
        resolutionMap.put("Incomplete", "4");
        resolutionMap.put("Cannot Reproduce", "5");

        statusMap.put("Open", "1");
        statusMap.put("In Progress", "3");
        statusMap.put("Reopened", "4");
        statusMap.put("Resolved", "5");
        statusMap.put("Closed", "6");

        typeMap.put("Bug", "1");
        typeMap.put("New Feature", "2");
        typeMap.put("Task", "3");
        typeMap.put("Improvement", "4");
        typeMap.put("Wish", "5");
        typeMap.put("Test", "6");
        typeMap.put("Sub-task", "7");
    }

    public String build() {
        // If the user has defined a filter - use that
        if ((this.filter != null) && (this.filter.length() > 0)) {
            return this.filter;
        } else {
            return query.toString();
        }
    }

    public JiraQueryBuilder components(String components) {
        // add components
        if (components != null) {
            String[] componentsArr = components.split(",");

            for (String component : componentsArr) {
                component = component.trim();
                if (component.length() > 0) {
                    query.append("&component=").append(component);
                }
            }
        }
        return this;
    }

    public JiraQueryBuilder components(List<String> components) {
        // add components
        if (components != null) {
            for (String component : components) {
                component = component.trim();
                if (component.length() > 0) {
                    query.append("&component=").append(component);
                }
            }
        }
        return this;
    }

    public JiraQueryBuilder filter(String filter) {
        this.filter = filter;
        return this;
    }

    /**
     * This method has no effect in this implementation.
     */
    public JiraQueryBuilder fixVersion(String fixVersion) {
        return this;
    }

    public JiraQueryBuilder fixVersionIds(String fixVersionIds) {
        // add fix versions
        if (fixVersionIds != null) {
            String[] fixVersions = fixVersionIds.split(",");

            for (String fixVersion : fixVersions) {
                if (fixVersion.length() > 0) {
                    query.append("&fixfor=").append(fixVersion.trim());
                }
            }
        }
        return this;
    }

    public JiraQueryBuilder fixVersionIds(List<String> fixVersionIds) {
        throw new RuntimeException("fixVersionIds(List) not supported for very old parameter queries.");
    }

    public Log getLog() {
        return log;
    }

    public JiraQueryBuilder priorityIds(String priorityIds) {
        // get the Priority Ids
        if (priorityIds != null) {
            String[] prios = priorityIds.split(",");

            for (String prio : prios) {
                prio = prio.trim();
                String priorityParam = priorityMap.get(prio);

                if (priorityParam != null) {
                    query.append("&priorityIds=").append(priorityParam);
                }
            }
        }
        return this;
    }

    public JiraQueryBuilder priorityIds(List<String> priorityIds) {
        throw new RuntimeException("priorityIds(List) not supported for old parameter queries.");
    }

    /**
     * This method has no effect in this implementation.
     */
    public JiraQueryBuilder project(String project) {
        return this;
    }

    public JiraQueryBuilder resolutionIds(String resolutionIds) {
        // get the Resolution Ids
        if (resolutionIds != null) {
            String[] resos = resolutionIds.split(",");

            for (String reso : resos) {
                reso = reso.trim();
                String resoParam = resolutionMap.get(reso);

                if (resoParam != null) {
                    query.append("&resolutionIds=").append(resoParam);
                }
            }
        }
        return this;
    }

    public JiraQueryBuilder resolutionIds(List<String> resolutionIds) {
        throw new RuntimeException("resolutionIds(List) not supported for old ParameterQueryBuilder");
    }

    public JiraQueryBuilder sortColumnNames(String sortColumnNames) {
        // get the Sort order
        int validSortColumnNames = 0;
        if (sortColumnNames != null) {
            String[] sortColumnNamesArray = sortColumnNames.split(",");
            // N.B. Add in reverse order (it's the way JIRA 3 likes it!!)
            for (int i = sortColumnNamesArray.length - 1; i >= 0; i--) {
                String lowerColumnName = sortColumnNamesArray[i].trim().toLowerCase(Locale.ENGLISH);
                boolean descending = false;
                String fieldName = null;
                if (lowerColumnName.endsWith("desc")) {
                    descending = true;
                    lowerColumnName = lowerColumnName
                            .substring(0, lowerColumnName.length() - 4)
                            .trim();
                } else if (lowerColumnName.endsWith("asc")) {
                    descending = false;
                    lowerColumnName = lowerColumnName
                            .substring(0, lowerColumnName.length() - 3)
                            .trim();
                }

                switch (lowerColumnName) {
                    case "key":
                        fieldName = "issuekey";
                        break;
                    case "summary":
                        fieldName = lowerColumnName;
                        break;
                    case "status":
                        fieldName = lowerColumnName;
                        break;
                    case "resolution":
                        fieldName = lowerColumnName;
                        break;
                    case "assignee":
                        fieldName = lowerColumnName;
                        break;
                    case "reporter":
                        fieldName = lowerColumnName;
                        break;
                    case "type":
                        fieldName = "issuetype";
                        break;
                    case "priority":
                        fieldName = lowerColumnName;
                        break;
                    case "version":
                        fieldName = "versions";
                        break;
                    case "fix version":
                        fieldName = "fixVersions";
                        break;
                    case "component":
                        fieldName = "components";
                        break;
                    case "created":
                        fieldName = lowerColumnName;
                        break;
                    case "updated":
                        fieldName = lowerColumnName;
                        break;
                    default:
                        // skip
                }
                if (fieldName != null) {
                    query.append("&sorter/field=");
                    query.append(fieldName);
                    query.append("&sorter/order=");
                    query.append(descending ? "DESC" : "ASC");
                    validSortColumnNames++;
                } else {
                    // Error in the configuration
                    getLog().error("maven-changes-plugin: The configured value '" + lowerColumnName
                            + "' for sortColumnNames is not correct.");
                }
            }
            if (validSortColumnNames == 0) {
                // Error in the configuration
                getLog().error("maven-changes-plugin: None of the configured sortColumnNames '" + sortColumnNames
                        + "' are correct.");
            }
        }
        return this;
    }

    public JiraQueryBuilder statusIds(String statusIds) {
        // get the Status Ids
        if (statusIds != null) {
            String[] stats = statusIds.split(",");
            for (String stat : stats) {
                stat = stat.trim();
                String statusParam = statusMap.get(stat);

                if (statusParam != null) {
                    query.append("&statusIds=").append(statusParam);
                } else {
                    // if it's numeric we can handle it too.
                    try {
                        Integer.parseInt(stat);
                        query.append("&statusIds=").append(stat);
                    } catch (NumberFormatException nfe) {
                        getLog().error("maven-changes-plugin: invalid statusId " + stat);
                    }
                }
            }
        }
        return this;
    }

    public JiraQueryBuilder statusIds(List<String> statusIds) {
        throw new RuntimeException("statusIds(List) not supported for old parameter queries.");
    }

    public JiraQueryBuilder typeIds(String typeIds) {
        // get the Type Ids
        if (typeIds != null) {
            String[] types = typeIds.split(",");

            for (String type : types) {
                String typeParam = typeMap.get(type.trim());

                if (typeParam != null) {
                    query.append("&type=").append(typeParam);
                }
            }
        }
        return this;
    }

    public JiraQueryBuilder typeIds(List<String> typeIds) {
        throw new RuntimeException("typeIds(List) not supported for old ParameterQueryBuilder");
    }
}
