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

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.filtering.MavenFileFilter;

/**
 * Goal which creates a nicely formatted Changes Report in html format from a changes.xml file.
 *
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @deprecated use {@code change} goal
 */
@Deprecated
@Mojo(name = "changes-report", threadSafe = true)
public class ChangesDeprecatedReport extends ChangesReport {

    @Inject
    public ChangesDeprecatedReport(MavenFileFilter mavenFileFilter) {
        super(mavenFileFilter);
    }
}
