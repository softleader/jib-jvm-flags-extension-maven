/*
 * Copyright © 2024 SoftLeader
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
import static java.lang.Boolean.FALSE;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Jvm flag extension for Jib.
 *
 * @author Matt Ho
 */
public class JvmFlagsExtension implements JibMavenPluginExtension<Void> {

  public static final String CACHE_DIRECTORY_NAME = "jib-cache";
  public static final String LAYER_JVM_FLAGS = "jvm flags";
  public static final String JIB_JVM_FLAGS_FILE = "jib-jvm-flags-file";
  public static final String PROPERTY_SKIP_IF_EMPTY = "skipIfEmpty";
  public static final boolean DEFAULT_SKIP_IF_EMPTY = FALSE;
  public static final String PROPERTY_SEPARATOR = "separator";
  public static final String DEFAULT_SEPARATOR = " ";

  private AbsoluteUnixPath appRoot = AbsoluteUnixPath.get("/app");
  private List<String> jvmFlags = Collections.emptyList();
  private Path cacheDirectory =
      Paths.get(System.getProperty("java.io.tmpdir"), CACHE_DIRECTORY_NAME);

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
      if (jvmFlags.isEmpty() && skipIfEmpty(properties)) {
        logger.log(LogLevel.LIFECYCLE, "No JVM Flags are configured, skipping");
        return buildPlan;
      }
      var jvmFlagsJoined = join(separator(properties), jvmFlags);
      logger.log(LogLevel.LIFECYCLE, format("JVM Flags set to [%s]", jvmFlagsJoined));
      AbsoluteUnixPath fileInContainer = appRoot.resolve(JIB_JVM_FLAGS_FILE);
      logger.log(
          LogLevel.LIFECYCLE,
          format("Adding layer containing '%s' file to the image", fileInContainer.toString()));
      LayerObject layer = createJvmFlagsFilesLayer(cacheDirectory, jvmFlagsJoined, fileInContainer);
      return buildPlan.toBuilder().addLayer(layer).build();
    } catch (IOException ex) {
      throw new JibPluginExtensionException(getClass(), verifyNotNull(ex.getMessage()), ex);
    }
  }

  private String separator(@NonNull Map<String, String> properties) {
    return getProperty(properties, PROPERTY_SEPARATOR, identity(), DEFAULT_SEPARATOR);
  }

  private boolean skipIfEmpty(@NonNull Map<String, String> properties) {
    return getProperty(
        properties, PROPERTY_SKIP_IF_EMPTY, Boolean::parseBoolean, DEFAULT_SKIP_IF_EMPTY);
  }

  private <T> T getProperty(
      @NonNull Map<String, String> properties,
      String key,
      Function<String, T> downstream,
      T defaultIfNull) {
    return ofNullable(properties.get(key)).map(downstream).orElse(defaultIfNull);
  }

  /**
   * @param sourceDirectory 來源目錄
   * @param jvmFlags jvm flags 內容
   * @param pathInContainer 要寫到 image 中檔案的路徑
   */
  @VisibleForTesting
  static FileEntriesLayer createJvmFlagsFilesLayer(
      Path sourceDirectory, String jvmFlags, AbsoluteUnixPath pathInContainer) throws IOException {
    Path file = sourceDirectory.resolve(JIB_JVM_FLAGS_FILE);
    writeFileConservatively(file, jvmFlags);
    return FileEntriesLayer.builder()
        .setName(LAYER_JVM_FLAGS)
        .addEntry(file, pathInContainer)
        .build();
  }

  @VisibleForTesting
  static void writeFileConservatively(Path file, String content) throws IOException {
    if (Files.exists(file)) {
      String oldContent = Files.readString(file);
      if (oldContent.equals(content)) {
        return;
      }
    }
    Files.createDirectories(file.getParent());
    Files.write(file, content.getBytes(UTF_8));
  }

  private void readJibConfigurations(@NonNull MavenProject project) {
    cacheDirectory = Paths.get(project.getBuild().getDirectory(), CACHE_DIRECTORY_NAME);
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
                stream(jvmFlagsDom.getChildren())
                    .map(Xpp3Dom::getValue)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
          }
        }
      }
    }
  }
}
