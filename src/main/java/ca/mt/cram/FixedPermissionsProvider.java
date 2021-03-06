/*
 * Copyright 2019 Google Inc.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiFunction;

class FixedPermissionsProvider
    implements BiFunction<Path, AbsoluteUnixPath, FilePermissions> {
  private final FilePermissions filesPermission;
  private final FilePermissions directoriesPermission;

  FixedPermissionsProvider(
      FilePermissions filesPermission, FilePermissions directoriesPermission) {
    this.filesPermission = filesPermission;
    this.directoriesPermission = directoriesPermission;
  }

  @Override
  public FilePermissions apply(Path local, AbsoluteUnixPath inContainer) {
    return Files.isDirectory(local) ? directoriesPermission : filesPermission;
  }
}