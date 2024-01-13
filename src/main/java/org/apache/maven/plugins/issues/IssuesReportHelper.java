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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.maven.plugin.logging.Log;

/**
 * A helper class for generation of reports based on issues.
 *
 * @author Dennis Lundberg
 * @version $Id$
 * @since 2.4
 */
public class IssuesReportHelper {
    public static final int COLUMN_ASSIGNEE = 0;

    public static final int COLUMN_COMPONENT = 1;

    public static final int COLUMN_CREATED = 2;

    public static final int COLUMN_FIX_VERSION = 3;

    public static final int COLUMN_ID = 4;

    public static final int COLUMN_KEY = 5;

    public static final int COLUMN_PRIORITY = 6;

    public static final int COLUMN_REPORTER = 7;

    public static final int COLUMN_RESOLUTION = 8;

    public static final int COLUMN_STATUS = 9;

    public static final int COLUMN_SUMMARY = 10;

    public static final int COLUMN_TYPE = 11;

    public static final int COLUMN_UPDATED = 12;

    public static final int COLUMN_VERSION = 13;

    /**
     * Get a list of id:s for the columns that are to be included in the report.
     *
     * @param columnNames The names of the columns
     * @param allColumns A mapping from column name to column id
     * @return A List of column id:s
     */
    public static List<Integer> getColumnIds(String columnNames, Map<String, Integer> allColumns) {
        return getColumnIds(columnNames, allColumns, null, null);
    }

    /**
     * Get a list of id:s for the columns that are to be included in the report. This method also handles deprecated
     * column names, which will still work. If deprecated column names are used they generate a warning, indicating the
     * replacement column name.
     *
     * @param columnNames The names of the columns
     * @param allColumns A mapping from column name to column id
     * @param deprecatedColumns A mapping from deprecated column name to column id
     * @param log A log
     * @return A List of column id:s
     */
    public static List<Integer> getColumnIds(
            String columnNames, Map<String, Integer> allColumns, Map<String, Integer> deprecatedColumns, Log log) {
        DualHashBidiMap bidiColumns = null;
        List<Integer> columnIds = new ArrayList<>();
        String[] columnNamesArray = columnNames.split(",");

        if (deprecatedColumns != null) {
            bidiColumns = new DualHashBidiMap(allColumns);
        }

        // Loop through the names of the columns, to validate each of them and add their id to the list
        for (String aColumnNamesArray : columnNamesArray) {
            String columnName = aColumnNamesArray.trim();
            if (allColumns.containsKey(columnName)) {
                columnIds.add(allColumns.get(columnName));
            } else if (deprecatedColumns != null && deprecatedColumns.containsKey(columnName)) {
                Integer columnId = deprecatedColumns.get(columnName);
                columnIds.add(columnId);
                if (log != null) {
                    log.warn("The columnName '" + columnName + "' has been deprecated." + " Please use "
                            + "the columnName '" + bidiColumns.getKey(columnId) + "' instead.");
                }
            }
        }
        return columnIds;
    }

    /**
     * Print a list of values separated by commas.
     *
     * @param values The values to print
     * @return A nicely formatted string of values.
     */
    public static String printValues(List<String> values) {
        StringBuilder sb = new StringBuilder();
        if (values != null) {
            Iterator<String> iterator = values.iterator();
            while (iterator.hasNext()) {
                String value = iterator.next();
                sb.append(value);
                if (iterator.hasNext()) {
                    sb.append(", ");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Convert a List of Integers to an int array.
     *
     * @param list The List to convert
     * @return An in array
     */
    public static int[] toIntArray(List<Integer> list) {
        int[] intArray = new int[list.size()];
        for (int j = 0; j < intArray.length; j++) {
            intArray[j] = list.get(j);
        }
        return intArray;
    }
}
