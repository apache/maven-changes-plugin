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

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit tests for {@link JiraChangesReport}.
 *
 * @author jrh3k5
 * @since 2.8
 */
@MojoTest
public class JiraChangesReportTest {

    /**
     * If the mojo has been marked to be skipped, then it should indicate that the report cannot be generated.
     *
     * @throws Exception If any errors occur during the test run.
     */
    @Test
    @InjectMojo(goal = "jira-changes")
    @MojoParameter(name = "skip", value = "true")
    public void testCanGenerateReportSkipped(JiraChangesReport mojo) {
        assertFalse(mojo.canGenerateReport());
    }
}
