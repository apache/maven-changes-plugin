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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract calls with helper methods for {@link Release}
 */
public abstract class AbstractRelease {

    private final List<Component> components = new ArrayList<>();

    public abstract List<Action> getActions();

    /**
     * Retrieve action list by given type
     *
     * @param type action type
     * @return an action list
     */
    public List<Action> getActions(String type) {
        return getActions().stream()
                .filter(a -> a.getType() != null)
                .filter(a -> a.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    public void addComponent(String name, Release release) {
        final Component component = new Component();
        component.setName(name);
        component.setDescription(release.getDescription());
        component.setActions(release.getActions());
        components.add(component);
    }

    public List<Component> getComponents() {
        return components;
    }
}
