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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import junit.framework.TestCase;
import org.apache.maven.plugin.testing.SilentLog;

/**
 * Test class for {@link JqlQueryBuilder}
 *
 * @author ton.swieb@finalist.com
 * @version $Id$
 * @since 2.8
 */
public class JqlQueryBuilderTestCase extends TestCase {
    private static final String ENCODING = "UTF-8";

    private final JqlQueryBuilder builder = new JqlQueryBuilder(new SilentLog());

    public void testEmptyQuery() {
        String actual = builder.build();
        String expected = "";
        assertEquals(expected, actual);
    }

    public void testSingleParameterValue() throws UnsupportedEncodingException {
        String expected = URLEncoder.encode("project = DOXIA", ENCODING);

        String actual = builder.project("DOXIA").build();
        assertEquals(expected, actual);
    }

    public void testFixVersion() throws UnsupportedEncodingException {
        String expected = URLEncoder.encode("fixVersion = \"1.0\"", ENCODING);

        String actual = builder.fixVersion("1.0").build();
        assertEquals(expected, actual);
    }

    public void testFixVersionCombinedWithOtherParameters() throws UnsupportedEncodingException {
        String expected = URLEncoder.encode("project = DOXIA AND fixVersion = \"1.0\"", ENCODING);

        String actual = builder.project("DOXIA").fixVersion("1.0").build();
        assertEquals(expected, actual);
    }

    public void testSingleParameterSingleValue() throws UnsupportedEncodingException {
        String expected = URLEncoder.encode("priority in (Blocker)", ENCODING);

        String actual = builder.priorityIds("Blocker").build();
        assertEquals(expected, actual);

        actual = builder.priorityIds("  Blocker   ").build();
        assertEquals(expected, actual);
    }

    public void testSingleParameterMultipleValues() throws UnsupportedEncodingException {
        String expected = URLEncoder.encode("priority in (Blocker, Critical, Major)", ENCODING);

        String actual = builder.priorityIds("Blocker,Critical,Major").build();
        assertEquals(expected, actual);

        actual = builder.priorityIds("  Blocker  ,  Critical,  Major").build();
        assertEquals(expected, actual);
    }

    public void testMultipleParameterCombinedWithAND() throws UnsupportedEncodingException {
        String expected = URLEncoder.encode("priority in (Blocker) AND status in (Resolved)", ENCODING);

        String actual = builder.priorityIds("Blocker").statusIds("Resolved").build();
        assertEquals(expected, actual);
    }

    public void testValueWithSpacesAreQuoted() throws UnsupportedEncodingException {
        String expected = URLEncoder.encode("status in (\"In Progress\")", ENCODING);

        String actual = builder.statusIds("In Progress").build();
        assertEquals(expected, actual);
    }

    public void testSortSingleRowAscending() throws UnsupportedEncodingException {
        String expected = URLEncoder.encode("project = DOXIA ORDER BY key ASC", ENCODING);

        String actual = builder.project("DOXIA").sortColumnNames("key").build();
        assertEquals(expected, actual);

        actual = builder.project("DOXIA").sortColumnNames("key ASC").build();
        assertEquals(expected, actual);

        actual = builder.project("DOXIA").sortColumnNames("     key    ASC    ").build();
        assertEquals(expected, actual);
    }

    public void testSortSingleDescending() throws UnsupportedEncodingException {
        String expected = URLEncoder.encode("project = DOXIA ORDER BY key DESC", ENCODING);

        String actual = builder.project("DOXIA").sortColumnNames("key DESC").build();
        assertEquals(expected, actual);

        actual =
                builder.project("DOXIA").sortColumnNames("     key    DESC    ").build();
        assertEquals(expected, actual);
    }

    public void testSortMultipleColumns() throws UnsupportedEncodingException {
        String expected = URLEncoder.encode("project = DOXIA ORDER BY key ASC, assignee DESC, reporter ASC", ENCODING);

        String actual = builder.project("DOXIA")
                .sortColumnNames("key ASC,assignee DESC, reporter ASC")
                .build();
        assertEquals(expected, actual);
    }

    public void testOrderByIsLastElement() throws UnsupportedEncodingException {
        String expected = URLEncoder.encode("project = DOXIA ORDER BY key ASC, assignee DESC, reporter ASC", ENCODING);

        String actual = builder.sortColumnNames("key ASC,assignee DESC, reporter ASC")
                .project("DOXIA")
                .build();
        assertEquals(expected, actual);
    }
}
