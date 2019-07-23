/*
 * Copyright 2019 Manumitting Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.mt.cram;

import com.google.cloud.tools.jib.api.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.FilePermissions;
import com.google.cloud.tools.jib.api.LayerConfiguration;
import com.google.cloud.tools.jib.api.LayerEntry;
import io.github.glytching.junit.extension.folder.TemporaryFolder;
import io.github.glytching.junit.extension.folder.TemporaryFolderExtension;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import picocli.CommandLine;

/** Test for {@link LayerDefinitionParser}. */
@ExtendWith(TemporaryFolderExtension.class)
class LayerDefinitionParserTest {
  private LayerDefinitionParser fixture = new LayerDefinitionParser();

  private TemporaryFolder temporaryFolder;

  @BeforeEach
  public void prepare(TemporaryFolder temporaryFolder) {
    this.temporaryFolder = temporaryFolder;
  }

  @Test
  void testSource() throws Exception {
    LayerConfiguration result = fixture.convert("foo");
    Assertions.assertEquals("", result.getName());
    Assertions.assertEquals(1, result.getLayerEntries().size());
    LayerEntry layerEntry = result.getLayerEntries().get(0);
    Assertions.assertEquals(Paths.get("foo"), layerEntry.getSourceFile());
    Assertions.assertEquals(AbsoluteUnixPath.get("/"), layerEntry.getExtractionPath());
    Assertions.assertEquals(
        Instant.EPOCH.plus(Duration.ofSeconds(1)), layerEntry.getLastModifiedTime());
    Assertions.assertEquals(FilePermissions.DEFAULT_FILE_PERMISSIONS, layerEntry.getPermissions());
  }

  @Test
  void testSourceDestination() throws Exception {
    LayerConfiguration result = fixture.convert("foo:/dest");
    Assertions.assertEquals("", result.getName());
    Assertions.assertEquals(1, result.getLayerEntries().size());
    LayerEntry layerEntry = result.getLayerEntries().get(0);
    Assertions.assertEquals(Paths.get("foo"), layerEntry.getSourceFile());
    Assertions.assertEquals(AbsoluteUnixPath.get("/dest"), layerEntry.getExtractionPath());
    Assertions.assertEquals(
        Instant.EPOCH.plus(Duration.ofSeconds(1)), layerEntry.getLastModifiedTime());
    Assertions.assertEquals(FilePermissions.DEFAULT_FILE_PERMISSIONS, layerEntry.getPermissions());
  }

  @Test
  void testSourceDestinationWithInvalidDirective() throws Exception {
    try {
      fixture.convert("foo:/dest:baz=bop");
      Assertions.fail("Should have errored on invalid attribute");
    } catch (CommandLine.TypeConversionException ex) {
      Assertions.assertEquals("unknown layer configuration directive: baz", ex.getMessage());
    }
  }

  @Test
  void testSourceDestinationName() throws Exception {
    LayerConfiguration result = fixture.convert("foo:/dest:name=name=name");
    Assertions.assertEquals("name=name", result.getName());
    Assertions.assertEquals(1, result.getLayerEntries().size());
    LayerEntry layerEntry = result.getLayerEntries().get(0);
    Assertions.assertEquals(Paths.get("foo"), layerEntry.getSourceFile());
    Assertions.assertEquals(AbsoluteUnixPath.get("/dest"), layerEntry.getExtractionPath());
    Assertions.assertEquals(
        Instant.EPOCH.plus(Duration.ofSeconds(1)), layerEntry.getLastModifiedTime());
    Assertions.assertEquals(FilePermissions.DEFAULT_FILE_PERMISSIONS, layerEntry.getPermissions());
  }

  @Test
  void testSourceDestinationPermissions() throws Exception {
    File root = temporaryFolder.getRoot();
    File subdir = new File(root, "sub");
    Assertions.assertTrue(subdir.mkdir());
    File file = new File(subdir, "file.txt");
    Files.copy(new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8)), file.toPath());

    LayerConfiguration result = fixture.convert(root.toString() + ":/dest:permissions=111/222");
    Assertions.assertEquals(3, result.getLayerEntries().size());

    LayerEntry layerEntry = result.getLayerEntries().get(0);
    Assertions.assertEquals(root.toPath(), layerEntry.getSourceFile());
    Assertions.assertEquals(AbsoluteUnixPath.get("/dest"), layerEntry.getExtractionPath());
    Assertions.assertEquals(
        Instant.EPOCH.plus(Duration.ofSeconds(1)), layerEntry.getLastModifiedTime());
    Assertions.assertEquals(FilePermissions.fromOctalString("222"), layerEntry.getPermissions());

    layerEntry = result.getLayerEntries().get(1);
    Assertions.assertEquals(subdir.toPath(), layerEntry.getSourceFile());
    Assertions.assertEquals(AbsoluteUnixPath.get("/dest/sub"), layerEntry.getExtractionPath());
    Assertions.assertEquals(
        Instant.EPOCH.plus(Duration.ofSeconds(1)), layerEntry.getLastModifiedTime());
    Assertions.assertEquals(FilePermissions.fromOctalString("222"), layerEntry.getPermissions());

    layerEntry = result.getLayerEntries().get(2);
    Assertions.assertEquals(file.toPath(), layerEntry.getSourceFile());
    Assertions.assertEquals(
        AbsoluteUnixPath.get("/dest/sub/file.txt"), layerEntry.getExtractionPath());
    Assertions.assertEquals(
        Instant.EPOCH.plus(Duration.ofSeconds(1)), layerEntry.getLastModifiedTime());
    Assertions.assertEquals(FilePermissions.fromOctalString("111"), layerEntry.getPermissions());
  }

  @Test
  void testSourceDestinationTimestamps() throws Exception {
    File root = temporaryFolder.getRoot();
    File subdir = new File(root, "sub");
    Assertions.assertTrue(subdir.mkdir());
    File file = new File(subdir, "file.txt");
    Files.copy(new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8)), file.toPath());

    LayerConfiguration result = fixture.convert(root.toString() + ":/dest:timestamps=actual");
    Assertions.assertEquals(3, result.getLayerEntries().size());

    LayerEntry layerEntry = result.getLayerEntries().get(0);
    Assertions.assertEquals(root.toPath(), layerEntry.getSourceFile());
    Assertions.assertEquals(AbsoluteUnixPath.get("/dest"), layerEntry.getExtractionPath());
    Assertions.assertEquals(root.lastModified(), layerEntry.getLastModifiedTime().toEpochMilli());

    layerEntry = result.getLayerEntries().get(1);
    Assertions.assertEquals(subdir.toPath(), layerEntry.getSourceFile());
    Assertions.assertEquals(AbsoluteUnixPath.get("/dest/sub"), layerEntry.getExtractionPath());
    Assertions.assertEquals(subdir.lastModified(), layerEntry.getLastModifiedTime().toEpochMilli());

    layerEntry = result.getLayerEntries().get(2);
    Assertions.assertEquals(file.toPath(), layerEntry.getSourceFile());
    Assertions.assertEquals(
        AbsoluteUnixPath.get("/dest/sub/file.txt"), layerEntry.getExtractionPath());
    Assertions.assertEquals(file.lastModified(), layerEntry.getLastModifiedTime().toEpochMilli());
  }
}
