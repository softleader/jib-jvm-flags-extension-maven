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

import static org.assertj.core.api.Assertions.assertThat;
import static tw.com.softleader.cloud.tools.jib.maven.JvmFlagsExtension.*;

import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer;
import com.google.cloud.tools.jib.api.buildplan.FileEntry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JvmFlagsExtensionTest {

  @Test
  void testAddJvmArgFilesLayer(@TempDir Path tempDir) throws IOException {
    String jvmFlags = "-server -XshowSettings:vm";
    FileEntriesLayer layer =
        createJvmFlagsFilesLayer(tempDir, jvmFlags, AbsoluteUnixPath.get("/app"));

    Path jvmFlagsFile = tempDir.resolve(JIB_JVM_FLAGS_FILE);
    String jvmFlagsRead = Files.readString(jvmFlagsFile);

    assertThat(jvmFlagsRead).isEqualTo(jvmFlags);
    assertThat(layer.getName()).isEqualTo(LAYER_JVM_FLAGS);
    assertThat(layer.getEntries()).hasSize(1);
    FileEntry file = layer.getEntries().get(0);
    assertThat(file.getSourceFile()).isNotNull();
    assertThat(Files.readAllLines(file.getSourceFile(), StandardCharsets.UTF_8))
        .hasSize(1)
        .first()
        .isEqualTo(jvmFlags);
  }

  @Test
  void testWriteFileConservatively(@TempDir Path tempDir) throws IOException {
    Path file = tempDir.resolve("file.txt");
    writeFileConservatively(file, "some content");
    String content = Files.readString(file);
    assertThat(content).isEqualTo("some content");
  }
}
