/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.codehaus.mojo.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Named;
import javax.inject.Singleton;

import com.google.common.base.Strings;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Lifecycle participant that is meant to manage configuration.
 */
@Named
@Singleton
public class ConfigLifecycleParticipant
    extends AbstractMavenLifecycleParticipant
{
  private static final Logger logger = LoggerFactory.getLogger(ConfigLifecycleParticipant.class);

  private static final String TEMPLATE_TAG = "configurationTemplate";

  private static final String NAME_ATTRIBUTE = "name";

  /**
   * Flag that shows is this lifecycle participant invoked or not. If {@code false}, plugin is most probably
   * mis-configured, is not set to be extension.
   */
  static Boolean ACTIVATED = Boolean.FALSE;

  final String groupId;

  final String artifactId;

  public ConfigLifecycleParticipant() throws IOException {
    ACTIVATED = true;
    Properties p = new Properties();
    p.load(getClass().getResourceAsStream(
        "/" + getClass().getPackage().getName().replace('.', '/') + "/coordinates.properties"
    ));
    this.groupId = checkNotNull(p.getProperty("groupId"), "Cannot get groupId");
    this.artifactId = checkNotNull(p.getProperty("artifactId"), "Cannot get groupId");
  }

  @Override
  public void afterProjectsRead(final MavenSession session)
      throws MavenExecutionException
  {
    try {
      final int totalModules = session.getProjects().size();
      logger.info("Introspecting model with total of " + totalModules + " modules...");

      for (MavenProject project : session.getProjects()) {
        Build build = project.getModel().getBuild();
        final Map<String, Xpp3Dom> projectConfigurations = getProjectConfigurations(project);
        if (!projectConfigurations.isEmpty()) {
          logger.debug("{}", projectConfigurations);
          processPlugins(project, projectConfigurations, build.getPluginManagement());
          processPlugins(project, projectConfigurations, build);
        }
      }
    }
    catch (IllegalArgumentException e) {
      throw new MavenExecutionException(groupId + ":" + artifactId + " error:\n " + e.getMessage(), e);
    }
  }

  /**
   * Processes plugin of given container.
   */
  private void processPlugins(final MavenProject project,
      final Map<String, Xpp3Dom> projectConfigurations,
      final PluginContainer pluginContainer)
  {
    for (Plugin plugin : pluginContainer.getPlugins()) {
      if (!(groupId.equals(plugin.getGroupId()) && artifactId.equals(plugin.getArtifactId()))) {
        Xpp3Dom pluginConfiguration = (Xpp3Dom) plugin.getConfiguration();
        if (pluginConfiguration != null) {
          Xpp3Dom templateReference = pluginConfiguration.getChild(TEMPLATE_TAG);
          if (templateReference != null) {
            String name = templateReference.getAttribute(NAME_ATTRIBUTE);
            checkArgument(name != null,
                "Found configurationTemplate without %s attribute, plugin %s:%s\nPOM file: %s",
                NAME_ATTRIBUTE, plugin.getGroupId(), plugin.getArtifactId(), project.getFile().getAbsolutePath());
            logger.debug("plugin: {} {} needs template {}", plugin.getGroupId(), plugin.getArtifactId(), name);
            Xpp3Dom projectConfiguration = projectConfigurations.get(name);
            checkArgument(projectConfiguration != null,
                "No template defined with name '%s', required by plugin %s:%s configurationTemplate\nPOM file: %s",
                name, plugin.getGroupId(), plugin.getArtifactId(), project.getFile().getAbsoluteFile());

            addUnique(projectConfiguration, pluginConfiguration);
            addUnique(templateReference, pluginConfiguration);
            // TODO: cleanup, remove template configurationTemplate from pluginConfiguration
          }
        }
      }
    }
  }

  /**
   * Adds uniquely DOM child.
   */
  private void addUnique(final Xpp3Dom source, final Xpp3Dom target) {
    // TODO: add source children uniquely, if target has same named children, remove it
    for (Xpp3Dom sourceElement : source.getChildren()) {
      target.addChild(sourceElement);
    }
  }

  /**
   * Returns the available configuration templates for given project.
   */
  private Map<String, Xpp3Dom> getProjectConfigurations(final MavenProject mavenProject) {
    final Map<String, Xpp3Dom> result = new HashMap<>();
    final Plugin configurationMavenPlugin =
        mavenProject.getModel().getBuild() != null
            ? getConfigurationMavenPluginFromContainer(mavenProject.getModel().getBuild())
            : null;
    if (configurationMavenPlugin != null) {
      Xpp3Dom configuration = (Xpp3Dom) configurationMavenPlugin.getConfiguration();
      for (Xpp3Dom child : configuration.getChildren()) {
        if (TEMPLATE_TAG.equals(child.getName())) {
          String name = child.getAttribute(NAME_ATTRIBUTE);
          if (!Strings.isNullOrEmpty(name)) {
            result.put(name, child);
          }
        }
      }
    }
    return result;
  }

  /**
   * Returns the configuration-maven-plugin from pluginContainer or {@code null} if not present.
   */
  private Plugin getConfigurationMavenPluginFromContainer(final PluginContainer pluginContainer) {
    return getPluginByGAFromContainer(groupId, artifactId, pluginContainer);
  }

  private Plugin getPluginByGAFromContainer(final String groupId, final String artifactId,
      final PluginContainer pluginContainer)
  {
    Plugin result = null;
    for (Plugin plugin : pluginContainer.getPlugins()) {
      if (Strings.nullToEmpty(groupId).equals(Strings.nullToEmpty(plugin.getGroupId()))
          && Strings.nullToEmpty(artifactId).equals(Strings.nullToEmpty(plugin.getArtifactId()))) {
        if (result != null) {
          throw new IllegalStateException("The build contains multiple versions of plugin " + groupId + ":"
              + artifactId);
        }
        result = plugin;
      }

    }
    return result;
  }
}
