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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.ArraySorter;
import org.apache.maven.plugin.logging.Log;

/**
 * Builder for a JIRA query using the JIRA query language. Only a limited set of JQL is supported.
 *
 * @author ton.swieb@finalist.com
 * @version $Id$
 * @since 2.8
 */
public class JqlQueryBuilder {

    /**
     * JQL <a href="https://confluence.atlassian.com/gsgtest/advanced-searching-815566220.html">reserved words</a>.
     */
    private static final String[] RESERVED_JQL_WORDS = ArraySorter.sort(new String[] {
        "abort",
        "access",
        "add",
        "after",
        "alias",
        "all",
        "alter",
        "and",
        "any",
        "as",
        "asc",
        "audit",
        "avg",
        "before",
        "begin",
        "between",
        "boolean",
        "break",
        "by",
        "byte",
        "catch",
        "cf",
        "char",
        "character",
        "check",
        "checkpoint",
        "collate",
        "collation",
        "column",
        "commit",
        "connect",
        "continue",
        "count",
        "create",
        "current",
        "date",
        "decimal",
        "declare",
        "decrement",
        "default",
        "defaults",
        "define",
        "delete",
        "delimiter",
        "desc",
        "difference",
        "distinct",
        "divide",
        "do",
        "double",
        "drop",
        "else",
        "empty",
        "encoding",
        "end",
        "equals",
        "escape",
        "exclusive",
        "exec",
        "execute",
        "exists",
        "explain",
        "false",
        "fetch",
        "file",
        "field",
        "first",
        "float",
        "for",
        "from",
        "function",
        "go",
        "goto",
        "grant",
        "greater",
        "group",
        "having",
        "identified",
        "if",
        "immediate",
        "in",
        "increment",
        "index",
        "initial",
        "inner",
        "inout",
        "input",
        "insert",
        "int",
        "integer",
        "intersect",
        "intersection",
        "into",
        "is",
        "isempty",
        "isnull",
        "join",
        "last",
        "left",
        "less",
        "like",
        "limit",
        "lock",
        "long",
        "max",
        "min",
        "minus",
        "mode",
        "modify",
        "modulo",
        "more",
        "multiply",
        "next",
        "noaudit",
        "not",
        "notin",
        "nowait",
        "null",
        "number",
        "object",
        "of",
        "on",
        "option",
        "or",
        "order",
        "outer",
        "output",
        "power",
        "previous",
        "prior",
        "privileges",
        "public",
        "raise",
        "raw",
        "remainder",
        "rename",
        "resource",
        "return",
        "returns",
        "revoke",
        "right",
        "row",
        "rowid",
        "rownum",
        "rows",
        "select",
        "session",
        "set",
        "share",
        "size",
        "sqrt",
        "start",
        "strict",
        "string",
        "subtract",
        "sum",
        "synonym",
        "table",
        "then",
        "to",
        "trans",
        "transaction",
        "trigger",
        "true",
        "uid",
        "union",
        "unique",
        "update",
        "user",
        "validate",
        "values",
        "view",
        "when",
        "whenever",
        "where",
        "while",
        "with"
    });

    private String filter = "";

    private boolean urlEncode = true;

    /**
     * Log for debug output.
     */
    private Log log;

    private StringBuilder orderBy = new StringBuilder();

    private StringBuilder query = new StringBuilder();

    public JqlQueryBuilder(Log log) {
        this.log = log;
    }

    public String build() {
        try {
            String jqlQuery;
            // If the user has defined a filter, use that
            if (filter != null && !filter.isEmpty()) {
                jqlQuery = filter;
            } else {
                jqlQuery = query.toString() + orderBy.toString();
            }

            if (urlEncode) {
                getLog().debug("Encoding JQL query " + jqlQuery);
                String encodedQuery = URLEncoder.encode(jqlQuery, "UTF-8");
                getLog().debug("Encoded JQL query " + encodedQuery);
                return encodedQuery;
            } else {
                return jqlQuery;
            }
        } catch (UnsupportedEncodingException e) {
            getLog().error("Unable to encode JQL query with UTF-8", e);
            throw new RuntimeException(e);
        }
    }

    public JqlQueryBuilder components(String components) {
        addCommaSeparatedValues("component", components);
        return this;
    }

    public JqlQueryBuilder components(List<String> components) {
        addValues("component", components);
        return this;
    }

    public JqlQueryBuilder filter(String filter) {
        this.filter = filter;
        return this;
    }

    /**
     * When both {@code #fixVersion(String)} and {@link #fixVersionIds(String)} are used, then you will probably end up
     * with a JQL query that is valid, but returns nothing. Unless they both only reference the same fixVersion
     *
     * @param fixVersion a single fix version
     * @return the builder.
     */
    public JqlQueryBuilder fixVersion(String fixVersion) {
        addSingleValue("fixVersion", fixVersion);
        return this;
    }

    /**
     * When both {@link #fixVersion(String)} and {@link #fixVersionIds(String)} are used then you will probably end up
     * with a JQL query that is valid, but returns nothing. Unless they both only reference the same fixVersion
     *
     * @param fixVersionIds a comma-separated list of version ids.
     * @return the builder.
     */
    public JqlQueryBuilder fixVersionIds(String fixVersionIds) {
        addCommaSeparatedValues("fixVersion", fixVersionIds);
        return this;
    }

    /**
     * Add a sequence of version IDs already in a list.
     *
     * @param fixVersionIds the version ids.
     * @return the builder.
     */
    public JqlQueryBuilder fixVersionIds(List<String> fixVersionIds) {
        addValues("fixVersion", fixVersionIds);
        return this;
    }

    public Log getLog() {
        return log;
    }

    public JqlQueryBuilder priorityIds(String priorityIds) {
        addCommaSeparatedValues("priority", priorityIds);
        return this;
    }

    public JqlQueryBuilder priorityIds(List<String> priorityIds) {
        addValues("priority", priorityIds);
        return this;
    }

    public JqlQueryBuilder project(String project) {
        addSingleValue("project", project);
        return this;
    }

    public JqlQueryBuilder resolutionIds(String resolutionIds) {
        addCommaSeparatedValues("resolution", resolutionIds);
        return this;
    }

    public JqlQueryBuilder resolutionIds(List<String> resolutionIds) {
        addValues("resolution", resolutionIds);
        return this;
    }

    public JqlQueryBuilder sortColumnNames(String sortColumnNames) {
        if (sortColumnNames != null) {
            orderBy.append(" ORDER BY ");

            String[] sortColumnNamesArray = sortColumnNames.split(",");

            for (int i = 0; i < sortColumnNamesArray.length - 1; i++) {
                addSingleSortColumn(sortColumnNamesArray[i]);
                orderBy.append(", ");
            }
            addSingleSortColumn(sortColumnNamesArray[sortColumnNamesArray.length - 1]);
        }
        return this;
    }

    public JqlQueryBuilder statusIds(String statusIds) {
        addCommaSeparatedValues("status", statusIds);
        return this;
    }

    public JqlQueryBuilder statusIds(List<String> statusIds) {
        addValues("status", statusIds);
        return this;
    }

    public JqlQueryBuilder typeIds(String typeIds) {
        addCommaSeparatedValues("type", typeIds);
        return this;
    }

    public JqlQueryBuilder typeIds(List<String> typeIds) {
        addValues("type", typeIds);
        return this;
    }

    public JqlQueryBuilder urlEncode(boolean doEncoding) {
        urlEncode = doEncoding;
        return this;
    }

    public boolean urlEncode() {
        return urlEncode;
    }

    /* --------------------------------------------------------------------- */
    /* Private methods */
    /* --------------------------------------------------------------------- */

    private void addCommaSeparatedValues(String key, String values) {
        if (values != null) {
            if (query.length() > 0) {
                query.append(" AND ");
            }

            query.append(key).append(" in (");

            String[] valuesArr = values.split(",");

            for (int i = 0; i < (valuesArr.length - 1); i++) {
                trimAndQuoteValue(valuesArr[i]);
                query.append(", ");
            }
            trimAndQuoteValue(valuesArr[valuesArr.length - 1]);
            query.append(")");
        }
    }

    private void addValues(String key, List<String> values) {
        if (values != null && !values.isEmpty()) {
            if (query.length() > 0) {
                query.append(" AND ");
            }

            query.append(key).append(" in (");

            for (int i = 0; i < (values.size() - 1); i++) {
                trimAndQuoteValue(values.get(i));
                query.append(", ");
            }
            trimAndQuoteValue(values.get(values.size() - 1));
            query.append(")");
        }
    }

    private void addSingleSortColumn(String name) {
        boolean descending = false;
        name = name.trim().toLowerCase(Locale.ENGLISH);
        if (name.endsWith("desc")) {
            descending = true;
            name = name.substring(0, name.length() - 4).trim();
        } else if (name.endsWith("asc")) {
            descending = false;
            name = name.substring(0, name.length() - 3).trim();
        }
        // Strip any spaces from the column name, or it will trip up JIRA's JQL parser
        name = name.replaceAll(" ", "");
        orderBy.append(name);
        orderBy.append(descending ? " DESC" : " ASC");
    }

    private void addSingleValue(String key, String value) {
        if (value != null) {
            if (query.length() > 0) {
                query.append(" AND ");
            }
            query.append(key).append(" = ");
            trimAndQuoteValue(value);
        }
    }

    private void trimAndQuoteValue(String value) {
        String trimmedValue = value.trim();
        if (trimmedValue.contains(" ") || trimmedValue.contains(".") || isReservedJqlWord(trimmedValue)) {
            query.append("\"").append(trimmedValue).append("\"");
        } else {
            query.append(trimmedValue);
        }
    }

    /**
     * JQL <a href="https://confluence.atlassian.com/gsgtest/advanced-searching-815566220.html">reserved words</a>.
     *
     * @param value a string
     * @return whether the given string is a JQL reserved word.
     */
    private boolean isReservedJqlWord(String value) {
        return Arrays.binarySearch(RESERVED_JQL_WORDS, value.toLowerCase(Locale.ROOT)) > 0;
    }
}
