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

import org.apache.maven.plugins.changes.IssueType;
import org.apache.maven.plugins.changes.issues.AbstractIssueManagementSystem;

/**
 * The JIRA issue management system.
 *
 * @version $Id$
 */
public class JIRAIssueManagementSystem extends AbstractIssueManagementSystem {

    public JIRAIssueManagementSystem() {
        super();
        // Add the standard issue types for JIRA
        issueTypeMap.put("Bug", IssueType.FIX);
        issueTypeMap.put("Dependency upgrade", IssueType.UPDATE);
        issueTypeMap.put("Improvement", IssueType.UPDATE);
        issueTypeMap.put("New Feature", IssueType.ADD);
        issueTypeMap.put("Task", IssueType.UPDATE);
        issueTypeMap.put("Wish", IssueType.UPDATE);
    }

    @Override
    public String getName() {
        return "JIRA";
    }
}
