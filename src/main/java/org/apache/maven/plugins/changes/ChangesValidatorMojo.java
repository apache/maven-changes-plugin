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

import javax.inject.Inject;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.changes.schema.ChangesSchemaValidator;
import org.apache.maven.plugins.changes.schema.SchemaValidatorException;
import org.apache.maven.plugins.changes.schema.XmlValidationHandler;
import org.xml.sax.SAXParseException;

/**
 * Goal which validates the <code>changes.xml</code> file.
 *
 * @author Olivier Lamy
 * @version $Id$
 * @since 2.1
 */
@Mojo(name = "changes-validate", threadSafe = true)
public class ChangesValidatorMojo extends AbstractChangesMojo {

    /**
     * The changes xsd version.
     */
    @Parameter(property = "changes.xsdVersion", defaultValue = "2.0.0")
    private String changesXsdVersion;

    /**
     * Mojo failure if validation failed. If false and validation failed, only a warning will be logged.
     */
    @Parameter(property = "changes.validate.failed", defaultValue = "false")
    private boolean failOnError;

    /**
     * The path of the <code>changes.xml</code> file that will be validated.
     */
    @Parameter(property = "changes.xmlPath", defaultValue = "src/changes/changes.xml")
    private File xmlPath;

    private ChangesSchemaValidator changesSchemaValidator;

    @Inject
    public ChangesValidatorMojo(ChangesSchemaValidator changesSchemaValidator) {
        this.changesSchemaValidator = changesSchemaValidator;
    }

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException {
        // Run only at the execution root
        if (runOnlyAtExecutionRoot && !isThisTheExecutionRoot()) {
            getLog().info("Skipping the changes validate in this project because it's not the Execution Root");
        } else {
            if (!xmlPath.exists()) {
                getLog().warn("changes.xml file " + xmlPath.getAbsolutePath() + " does not exist.");
                return;
            }

            try {
                XmlValidationHandler xmlValidationHandler =
                        changesSchemaValidator.validateXmlWithSchema(xmlPath, changesXsdVersion, failOnError);
                boolean hasErrors = !xmlValidationHandler.getErrors().isEmpty();
                if (hasErrors) {
                    logSchemaValidation(xmlValidationHandler.getErrors());
                    if (failOnError) {
                        throw new MojoExecutionException("changes.xml file " + xmlPath.getAbsolutePath()
                                + " is not valid. See previous errors.");
                    } else {
                        getLog().info(" skip previous validation errors due to failOnError=false.");
                    }
                }
            } catch (SchemaValidatorException e) {
                if (failOnError) {
                    throw new MojoExecutionException(
                            "changes.xml file is not valid: " + xmlPath.getAbsolutePath() + ": " + e.getMessage(), e);
                }
            }
        }
    }

    private void logSchemaValidation(List<SAXParseException> errors) {
        getLog().warn("changes.xml file is not valid: " + xmlPath.getAbsolutePath());
        getLog().warn("validation errors: ");
        for (SAXParseException error : errors) {
            getLog().warn(error.getMessage());
        }
    }
}
