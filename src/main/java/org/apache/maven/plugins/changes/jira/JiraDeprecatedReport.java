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

import javax.inject.Inject;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.settings.crypto.SettingsDecrypter;

/**
 * Goal which downloads issues from the Issue Tracking System and generates a report.
 *
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @since 2.0
 * @deprecated use {@code jira-changes} goal
 */
@Deprecated
@Mojo(name = "jira-report", threadSafe = true)
public class JiraDeprecatedReport extends JiraChangesReport {

    @Inject
    public JiraDeprecatedReport(SettingsDecrypter settingsDecrypter) {
        super(settingsDecrypter);
    }
}
