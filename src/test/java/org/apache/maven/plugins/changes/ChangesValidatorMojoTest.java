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

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Olivier Lamy
 * @since 29 juil. 2008
 * @version $Id$
 */
@MojoTest
public class ChangesValidatorMojoTest {

    @InjectMojo(goal = "changes-validate", pom = "src/test/unit/plugin-config.xml")
    @MojoParameter(name = "changesXsdVersion", value = "2.0.0")
    @MojoParameter(name = "xmlPath", value = "src/test/unit/changes.xml")
    @MojoParameter(name = "failOnError", value = "true")
    @Test
    public void testValidationSuccess(ChangesValidatorMojo mojo) throws Exception {
        mojo.execute();
    }

    @InjectMojo(goal = "changes-validate", pom = "src/test/unit/plugin-config.xml")
    @MojoParameter(name = "changesXsdVersion", value = "2.0.0")
    @MojoParameter(name = "xmlPath", value = "src/test/unit/non-valid-changes.xml")
    @MojoParameter(name = "failOnError", value = "true")
    @Test
    public void testValidationFailedWithMojoFailure(ChangesValidatorMojo mojo) throws Exception {
        try {
            mojo.execute();
            fail(" A MojoExecutionException should occur here. Changes file is not valid and failOnError is true ");
        } catch (MojoExecutionException e) {
            // expected
        }
    }

    @InjectMojo(goal = "changes-validate", pom = "src/test/unit/plugin-config.xml")
    @MojoParameter(name = "changesXsdVersion", value = "1.0.0")
    @MojoParameter(name = "xmlPath", value = "src/test/unit/non-valid-changes.xml")
    @MojoParameter(name = "failOnError", value = "false")
    @Test
    public void testValidationFailedWithNoMojoFailure(ChangesValidatorMojo mojo) throws Exception {
        mojo.execute();
    }
}
