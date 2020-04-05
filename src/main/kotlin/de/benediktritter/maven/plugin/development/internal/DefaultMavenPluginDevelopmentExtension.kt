/*
 * Copyright 2020 Benedikt Ritter
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

package de.benediktritter.maven.plugin.development.internal

import de.benediktritter.maven.plugin.development.MavenPluginDevelopmentExtension
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.the
import javax.inject.Inject

open class DefaultMavenPluginDevelopmentExtension @Inject constructor(project: Project) : MavenPluginDevelopmentExtension {

    override val pluginSourceSet: Property<SourceSet> = project.objects.property<SourceSet>()
            .convention(project.provider { project.the<SourceSetContainer>()["main"] })

    override val groupId: Property<String> = project.objects.property<String>()
            .convention(project.provider { project.group.toString() })

    override val artifactId: Property<String> = project.objects.property<String>()
            .convention(project.provider { project.name })

    override val version: Property<String> = project.objects.property<String>()
            .convention(project.provider { project.version.toString() })

    override val name: Property<String> = project.objects.property<String>()
            .convention(project.provider { project.name })

    override val description: Property<String> = project.objects.property<String>()
            .convention(project.provider { project.description })

    override val goalPrefix: Property<String> = project.objects.property()

    override val generateHelpMojo: Property<Boolean> = project.objects.property<Boolean>()
            .convention(false)
}
