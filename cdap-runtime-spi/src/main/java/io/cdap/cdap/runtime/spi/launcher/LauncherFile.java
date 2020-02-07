/*
 * Copyright © 2020 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.cdap.runtime.spi.launcher;

import java.net.URI;

/**
 *
 */
public class LauncherFile {
  private final String name;
  private final URI uri;
  private final boolean isArchive;

  public LauncherFile(String name, URI uri, boolean isArchive) {
    this.name = name;
    this.uri = uri;
    this.isArchive = isArchive;
  }

  public String getName() {
    return name;
  }

  public URI getUri() {
    return uri;
  }

  public boolean isArchive() {
    return isArchive;
  }

  @Override
  public String toString() {
    return "LauncherFile{" +
      "name='" + name + '\'' +
      ", uri=" + uri +
      ", isArchive=" + isArchive +
      '}';
  }
}
