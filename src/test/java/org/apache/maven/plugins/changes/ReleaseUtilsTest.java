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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugins.changes.model.Release;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dennis Lundberg
 * @version $Id$
 * @since 2.4
 */
public class ReleaseUtilsTest {

    @Test
    public void testMergeReleases() {

        List<Release> firstReleases = new ArrayList<>();
        List<Release> secondReleases = new ArrayList<>();
        List<Release> mergedReleases;

        mergedReleases = ReleaseUtils.mergeReleases(firstReleases, secondReleases);
        assertEquals(0, mergedReleases.size(), "Both empty");

        Release release = new Release();
        release.setVersion("1.0");
        firstReleases.add(release);

        mergedReleases = ReleaseUtils.mergeReleases(firstReleases, secondReleases);
        assertEquals(1, mergedReleases.size(), "One release in first");

        release = new Release();
        release.setVersion("1.1");
        secondReleases.add(release);

        mergedReleases = ReleaseUtils.mergeReleases(firstReleases, secondReleases);
        assertEquals(2, mergedReleases.size(), "One release each");

        release = new Release();
        release.setVersion("1.1");
        firstReleases.add(release);

        mergedReleases = ReleaseUtils.mergeReleases(firstReleases, secondReleases);
        assertEquals(
                2,
                mergedReleases.size(),
                "Two releases in first, one release in second with one version being the same");

        release = new Release();
        release.setVersion("1.2");
        secondReleases.add(release);

        mergedReleases = ReleaseUtils.mergeReleases(firstReleases, secondReleases);
        assertEquals(3, mergedReleases.size(), "Two releases each with one version being the same");
    }
}
