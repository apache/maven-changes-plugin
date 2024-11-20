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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.reporting.AbstractMavenReport;

/**
 * Base class with the things that should be in AbstractMavenReport anyway. Note: This file was copied from r415312 of
 * AbstractProjectInfoReport in maven-project-info-reports, as a work-around to MCHANGES-88.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractChangesReport extends AbstractMavenReport {
    /**
     * The current project base directory.
     *
     * @since 2.10
     */
    @Parameter(property = "basedir", required = true)
    protected String basedir;

    /**
     * This will cause the execution to be run only at the top of a given module tree. That is, run in the project
     * contained in the same folder where the mvn execution was launched.
     *
     * @since 2.10
     */
    @Parameter(property = "changes.runOnlyAtExecutionRoot", defaultValue = "false")
    protected boolean runOnlyAtExecutionRoot;

    /**
     * The Maven Session.
     *
     * @since 2.10
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession mavenSession;

    /**
     * Returns <code>true</code> if the current project is located at the Execution Root Directory (where mvn was
     * launched).
     *
     * @return <code>true</code> if the current project is at the Execution Root
     */
    protected boolean isThisTheExecutionRoot() {
        return getProject().isExecutionRoot();
    }
}
