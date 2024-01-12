/*
 * Copyright © 2022 SoftLeader
 *
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
package tw.com.softleader.cloud.tools.jib.maven;

import static com.google.common.base.Verify.verifyNotNull;
import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.join;

import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.ContainerBuildPlan;
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer;
import com.google.cloud.tools.jib.api.buildplan.LayerObject;
import com.google.cloud.tools.jib.maven.extension.JibMavenPluginExtension;
import com.google.cloud.tools.jib.maven.extension.MavenData;
import com.google.cloud.tools.jib.plugins.extension.ExtensionLogger;
import com.google.cloud.tools.jib.plugins.extension.ExtensionLogger.LogLevel;
import com.google.cloud.tools.jib.plugins.extension.JibPluginExtensionException;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class JvmFlagsCalculatorExtension implements JibMavenPluginExtension<Void> {

  public static final String LAYER_TYPE_JVM_FLAGS = "jvm flags";
  public static final String JIB_JVM_FLAGS_FILE = "jib-jvm-flags-file";

  private AbsoluteUnixPath appRoot = AbsoluteUnixPath.get("/app");
  private List<String> jvmFlags = Collections.emptyList();

  @Override
  public Optional<Class<Void>> getExtraConfigType() {
    return Optional.empty();
  }

  @Override
  public ContainerBuildPlan extendContainerBuildPlan(
      ContainerBuildPlan buildPlan,
      Map<String, String> properties,
      Optional<Void> config,
      MavenData mavenData,
      ExtensionLogger logger)
      throws JibPluginExtensionException {
    try {
      logger.log(LogLevel.LIFECYCLE, "Running JVM Flags Jib extension");
      readJibConfigurations(mavenData.getMavenProject());
      if (jvmFlags.isEmpty() && parseBoolean(properties.getOrDefault("skipIfEmpty", "false"))) {
        logger.log(LogLevel.LIFECYCLE, "No jvmFlags are configured, skipping");
        return buildPlan;
      }
      AbsoluteUnixPath file = appRoot.resolve(JIB_JVM_FLAGS_FILE);
      logger.log(
          LogLevel.LIFECYCLE,
          format("Adding layer containing '%s' file to the image", file.toString()));
      LayerObject layer = createFileLayer(LAYER_TYPE_JVM_FLAGS, file, join(jvmFlags, " "));
      return buildPlan.toBuilder().addLayer(layer).build();
    } catch (IOException ex) {
      throw new JibPluginExtensionException(getClass(), verifyNotNull(ex.getMessage()), ex);
    }
  }

  @VisibleForTesting
  static FileEntriesLayer createFileLayer(String layerName, AbsoluteUnixPath path, String content)
      throws IOException {
    Path file = Files.createTempFile(JIB_JVM_FLAGS_FILE + "-", "");
    Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    return FileEntriesLayer.builder().setName(layerName).addEntry(file, path).build();
  }

  private void readJibConfigurations(MavenProject project) {
    Plugin jibPlugin = project.getPlugin("com.google.cloud.tools:jib-maven-plugin");
    if (jibPlugin != null) {
      Xpp3Dom configurationDom = (Xpp3Dom) jibPlugin.getConfiguration();
      if (configurationDom != null) {
        Xpp3Dom containerDom = configurationDom.getChild("container");
        if (containerDom != null) {
          Xpp3Dom appRootDom = containerDom.getChild("appRoot");
          if (appRootDom != null) {
            appRoot = AbsoluteUnixPath.get(appRootDom.getValue());
          }
          Xpp3Dom jvmFlagsDom = containerDom.getChild("jvmFlags");
          if (jvmFlagsDom != null) {
            jvmFlags =
                Arrays.asList(jvmFlagsDom.getChildren()).stream()
                    .map(Xpp3Dom::getValue)
                    .collect(Collectors.toList());
          }
        }
      }
    }
  }
}
