/*
 * Copyright 2022 the GradleX team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradlex.maven.plugin.development.internal;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;

public final class MavenPluginDescriptor {

    private final GAV gav;
    private final String name;
    private final String description;
    private final String goalPrefix;

    public MavenPluginDescriptor(GAV gav, String name, String description, String goalPrefix) {
        this.gav = gav;
        this.name = name;
        this.description = description;
        this.goalPrefix = goalPrefix;
    }

    @Nested
    public GAV getGav() {
        return gav;
    }

    @Input
    public String getName() {
        return name;
    }

    @Input
    public String getDescription() {
        return description;
    }

    @Optional
    @Input
    public String getGoalPrefix() {
        return goalPrefix;
    }
}
