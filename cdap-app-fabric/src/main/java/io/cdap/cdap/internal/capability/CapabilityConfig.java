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

package io.cdap.cdap.internal.capability;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Configuration for applying an action for a capability
 */
public class CapabilityConfig {

  private final String label;
  private final CapabilityActionType type;
  private final String capability;
  private final List<SystemApplication> applications;
  private final List<SystemProgram> programs;

  public CapabilityConfig(String label, String type, String capability, @Nullable List<SystemApplication> applications,
                          @Nullable List<SystemProgram> programs) {
    this.label = label;
    this.type = CapabilityActionType.valueOf(type);
    this.capability = capability;
    this.applications = (applications == null) ? Collections.emptyList() : applications;
    this.programs = (programs == null) ? Collections.emptyList() : programs;
  }

  /**
   * @return label {@link String}
   */
  public String getLabel() {
    return label;
  }

  /**
   * @return {@link CapabilityActionType}
   */
  public CapabilityActionType getType() {
    return type;
  }

  /**
   * @return {@link String} capability
   */
  public String getCapability() {
    return capability;
  }

  /**
   * @return {@link List} of {@link SystemApplication} for this capability. Could be null.
   */
  public List<SystemApplication> getApplications() {
    return applications;
  }

  /**
   * @return {@link List} of {@link SystemProgram} for this capability. Could be null.
   */
  public List<SystemProgram> getPrograms() {
    return programs;
  }
}