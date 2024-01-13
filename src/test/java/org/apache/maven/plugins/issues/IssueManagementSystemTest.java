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
package org.apache.maven.plugins.issues;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * @author Alan Parkinson
 * @version $Id$
 * @since 2.7
 */
public class IssueManagementSystemTest extends TestCase {

    private MockIssueManagementSystem ims;

    private static class MockIssueManagementSystem extends AbstractIssueManagementSystem {

        @Override
        public String getName() {
            return "Mock IMS";
        }
    }

    @Override
    protected void setUp() {
        ims = new MockIssueManagementSystem();
    }

    public void testApplyingValidCustomIssueTypes() {
        Map<String, String> issueTypes = new HashMap<>();
        issueTypes.put("add", "Story,Epic");
        issueTypes.put("fix", "Defect");
        issueTypes.put("update", "Improvement");

        try {
            ims.applyConfiguration(issueTypes);
        } catch (MojoExecutionException e) {
            fail();
        }
    }

    public void testApplyingInvalidCustomIssueTypes() {
        Map<String, String> issueTypes = new HashMap<>();
        issueTypes.put("new", "Story,Epic");

        try {
            ims.applyConfiguration(issueTypes);
            fail("Exception not thrown for invalid group name");
        } catch (MojoExecutionException ignored) {

        }
    }
}
