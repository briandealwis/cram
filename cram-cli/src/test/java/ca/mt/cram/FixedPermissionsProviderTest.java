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
import io.github.glytching.junit.extension.folder.TemporaryFolder;
import io.github.glytching.junit.extension.folder.TemporaryFolderExtension;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TemporaryFolderExtension.class)
class FixedPermissionsProviderTest {
  FilePermissions filesPermission;
  FilePermissions directoriesPermission;
  File file;
  File directory;

  @BeforeEach
  public void setUp(TemporaryFolder temporaryDirectory) throws IOException {
    filesPermission = FilePermissions.fromOctalString("444");
    directoriesPermission = FilePermissions.fromOctalString("555");
    file = temporaryDirectory.createFile("file");
    directory = temporaryDirectory.createDirectory("directory");
  }

  @Test
  void testApply_file() {
    FixedPermissionsProvider provider =
        new FixedPermissionsProvider(filesPermission, directoriesPermission);
    Assertions.assertEquals(
        filesPermission, provider.apply(file.toPath(), AbsoluteUnixPath.get("/")));
  }

  @Test
  void testApply_directory() {
    FixedPermissionsProvider provider =
        new FixedPermissionsProvider(filesPermission, directoriesPermission);
    Assertions.assertEquals(
        directoriesPermission, provider.apply(directory.toPath(), AbsoluteUnixPath.get("/")));
  }
}
