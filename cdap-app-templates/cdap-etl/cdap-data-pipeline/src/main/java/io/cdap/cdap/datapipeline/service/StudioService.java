/*
 * Copyright © 2019 Cask Data, Inc.
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
 *
 */

package io.cdap.cdap.datapipeline.service;

import io.cdap.cdap.api.service.AbstractSystemService;
import io.cdap.cdap.datapipeline.draft.DraftStore;

/**
 * Service that handles pipeline studio operations, like validation and schema propagation.
 */
public class StudioService extends AbstractSystemService {
  public static final String NAME = "studio";

  @Override
  protected void configure() {

    setName(NAME);
    setDescription("Handles pipeline studio operations, like validation and schema propagation.");
    addHandler(new ValidationHandler());
    addHandler(new DraftHandler());
    createTable(DraftStore.TABLE_SPEC);
  }
}
