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
package org.apache.maven.plugins.changes.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract class with helper methods for {@link Action}.
 */
public abstract class AbstractAction {

    private List<DueTo> dueTosList;

    private List<String> fixedIssueList;

    public abstract String getDueTo();

    public abstract String getDueToEmail();

    public abstract String getFixedIssuesString();

    /**
     * Parse due-to and due-to-email attributes.
     *
     * @return a List of due-to person
     */
    public List<DueTo> getDueTos() {
        if (dueTosList != null) {
            return dueTosList;
        }
        List<DueTo> result = new ArrayList<>();
        List<String> dueTos = new ArrayList<>();
        List<String> dueToEmails = new ArrayList<>();

        if (getDueTo() != null) {
            Arrays.stream(getDueTo().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(dueTos::add);
        }

        if (getDueToEmail() != null) {
            Arrays.stream(getDueToEmail().split(",")).map(String::trim).forEach(dueToEmails::add);
        }

        while (dueToEmails.size() < dueTos.size()) {
            dueToEmails.add("");
        }

        for (int i = 0; i < dueTos.size(); i++) {
            DueTo dueTo = new DueTo();
            dueTo.setName(dueTos.get(i));
            dueTo.setEmail(dueToEmails.get(i));
            result.add(dueTo);
        }
        dueTosList = Collections.unmodifiableList(result);
        return dueTosList;
    }

    /**
     * Parse getFixedIssues attribute.
     *
     * @return a list of fixed issues
     */
    public List<String> getFixedIssues() {
        if (fixedIssueList != null) {
            return fixedIssueList;
        }
        List<String> result;
        if (getFixedIssuesString() != null) {
            result = Arrays.stream(getFixedIssuesString().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } else {
            result = Collections.emptyList();
        }

        fixedIssueList = Collections.unmodifiableList(result);
        return fixedIssueList;
    }
}
