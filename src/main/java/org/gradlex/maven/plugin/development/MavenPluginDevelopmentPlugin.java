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

package org.gradlex.maven.plugin.development;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.VerificationType;
import org.gradle.api.file.Directory;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.gradlex.maven.plugin.development.internal.GAV;
import org.gradlex.maven.plugin.development.internal.MavenPluginDescriptor;
import org.gradlex.maven.plugin.development.task.DependencyDescriptor;
import org.gradlex.maven.plugin.development.task.GenerateHelpMojoSourcesTask;
import org.gradlex.maven.plugin.development.task.GenerateMavenPluginDescriptorTask;
import org.gradlex.maven.plugin.development.task.UpstreamProjectDescriptor;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

public class MavenPluginDevelopmentPlugin implements Plugin<Project> {

    public static final String TASK_GROUP_NAME = "Maven Plugin Development";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);

        Provider<Directory> pluginOutputDirectory = project.getLayout().getBuildDirectory().dir("mavenPlugin");
        Provider<Directory> descriptorDir = pluginOutputDirectory.map(it -> it.dir("descriptor"));
        Provider<Directory> helpMojoDir = pluginOutputDirectory.map(it -> it.dir("helpMojo"));

        MavenPluginDevelopmentExtension extension = project.getExtensions().create(MavenPluginDevelopmentExtension.NAME, MavenPluginDevelopmentExtension.class);
        extension.getGroupId().convention(project.provider(() -> project.getGroup().toString()));
        extension.getArtifactId().convention(project.provider(project::getName));
        extension.getVersion().convention(project.provider(() -> project.getVersion().toString()));
        extension.getName().convention(project.provider(project::getName));
        extension.getDescription().convention(project.provider(project::getDescription));
        extension.getDependencies().convention(project.getConfigurations().getByName("runtimeClasspath"));

        TaskProvider<GenerateHelpMojoSourcesTask> generateHelpMojoTask = project.getTasks().register("generateMavenPluginHelpMojoSources", GenerateHelpMojoSourcesTask.class, task -> {
            task.setGroup(TASK_GROUP_NAME);
            task.setDescription("Generates a Maven help mojo that documents the usage of the Maven plugin");

            // capture helpMojoPackage property here for configuration cache compatibility
            Property<String> helpMojoPkg = extension.getHelpMojoPackage();
            task.onlyIf(t -> helpMojoPkg.isPresent());

            task.getHelpMojoPackage().set(extension.getHelpMojoPackage());
            task.getOutputDirectory().set(helpMojoDir);
            task.getHelpPropertiesFile().set(pluginOutputDirectory.map(it -> it.file("maven-plugin-help.properties")));
            task.getPluginDescriptor().set(project.provider(() ->
                    new MavenPluginDescriptor(
                            extension.getGroupId().get(),
                            extension.getArtifactId().get(),
                            extension.getVersion().get(),
                            extension.getName().get(),
                            extension.getDescription().getOrElse(""),
                            extension.getGoalPrefix().getOrNull()
                    )
            ));
            task.getRuntimeDependencies().set(
                    extension.getDependencies().map(it ->
                            it.getResolvedConfiguration().getResolvedArtifacts().stream()
                                    .map(artifact ->
                                            new DependencyDescriptor(
                                                    artifact.getModuleVersion().getId().getGroup(),
                                                    artifact.getModuleVersion().getId().getName(),
                                                    artifact.getModuleVersion().getId().getVersion(),
                                                    artifact.getExtension())
                                    ).collect(Collectors.toList())
                    )
            );
        });

        SourceSet main = project.getExtensions().getByType(SourceSetContainer.class).getByName("main");
        TaskProvider<GenerateMavenPluginDescriptorTask> generateTask = project.getTasks().register("generateMavenPluginDescriptor", GenerateMavenPluginDescriptorTask.class, task -> {
            task.setGroup(TASK_GROUP_NAME);
            task.setDescription("Generates the Maven plugin descriptor file");

            task.getClassesDirs().from(main.getOutput().getClassesDirs());
            task.getSourcesDirs().from(main.getJava().getSourceDirectories());
            task.getUpstreamProjects().convention(project.provider(() -> {
                Configuration compileClasspath = project.getConfigurations().getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME);
                String group = project.getGroup().toString();
                // classes
                Map<GAV, File> classesDirectoriesByGAV = compileClasspath.getIncoming()
                        .artifactView(vc -> {
                            vc.componentFilter(ci -> ci instanceof ProjectComponentIdentifier);
                        })
                        .getArtifacts().getArtifacts().stream()
                        .collect(Collectors.toMap(
                                a -> {
                                    ProjectComponentIdentifier m = (ProjectComponentIdentifier) a.getId().getComponentIdentifier();
                                    return new GAV(group, m.getProjectName(), null);
                                },
                                ResolvedArtifactResult::getFile
                        ));
                // sources per artifact view with variant reselection
                ObjectFactory objectFactory = project.getObjects();
                Map<GAV, File> sourcesDirectoriesByGAV = compileClasspath.getIncoming()
                        .artifactView(vc -> {
                            vc.componentFilter(ci -> ci instanceof ProjectComponentIdentifier);
                            vc.attributes(attributes -> {
                                attributes.attribute(Category.CATEGORY_ATTRIBUTE, objectFactory.named(Category.class, Category.VERIFICATION));
                                attributes.attribute(VerificationType.VERIFICATION_TYPE_ATTRIBUTE, objectFactory.named(VerificationType.class, VerificationType.MAIN_SOURCES));
                            });
                            vc.withVariantReselection();
                        })
                        .getArtifacts().getArtifacts().stream()
                        .collect(Collectors.toMap(
                                a -> {
                                    ProjectComponentIdentifier m = (ProjectComponentIdentifier) a.getId().getComponentIdentifier();
                                    return new GAV(group, m.getProjectName(), null);
                                },
                                ResolvedArtifactResult::getFile,
                                (l, r) -> l.getName().equals("java") ? l : r
                        ));
                return classesDirectoriesByGAV.entrySet().stream().collect(ArrayList::new, (acc, e) -> {
                    acc.add(new UpstreamProjectDescriptor(
                            e.getKey().group,
                            e.getKey().name,
                            e.getKey().version,
                            e.getValue(),
                            sourcesDirectoriesByGAV.get(e.getKey())
                    ));
                },
                        ArrayList::addAll);
            }));
            task.getOutputDirectory().set(descriptorDir);
            task.getPluginDescriptor().set(project.provider(() ->
                    new MavenPluginDescriptor(
                            extension.getGroupId().get(),
                            extension.getArtifactId().get(),
                            extension.getVersion().get(),
                            extension.getName().get(),
                            extension.getDescription().getOrElse(""),
                            extension.getGoalPrefix().getOrNull()
                    )
            ));
            task.getRuntimeDependencies().set(extension.getDependencies().map(c ->
                    c.getResolvedConfiguration().getResolvedArtifacts().stream()
                            .map(artifact -> new DependencyDescriptor(
                                            artifact.getModuleVersion().getId().getGroup(),
                                            artifact.getModuleVersion().getId().getName(),
                                            artifact.getModuleVersion().getId().getVersion(),
                                            artifact.getExtension()
                                    )
                            )
                            .collect(Collectors.toList())
            ));

            task.dependsOn(main.getOutput(), generateHelpMojoTask);
        });

        project.afterEvaluate(p -> {
            Jar jarTask = (Jar) p.getTasks().findByName(main.getJarTaskName());
            jarTask.from(generateTask);
            main.getJava().srcDir(generateHelpMojoTask.map(GenerateHelpMojoSourcesTask::getOutputDirectory));
        });
    }
}
