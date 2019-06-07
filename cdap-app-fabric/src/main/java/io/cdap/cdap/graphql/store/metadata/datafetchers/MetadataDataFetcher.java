/*
 *
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
 */

package io.cdap.cdap.graphql.store.metadata.datafetchers;

import com.google.inject.Inject;
import graphql.schema.AsyncDataFetcher;
import graphql.schema.DataFetcher;
import io.cdap.cdap.api.metadata.MetadataEntity;
import io.cdap.cdap.client.MetadataClient;
import io.cdap.cdap.client.config.ClientConfig;
import io.cdap.cdap.common.metadata.MetadataRecord;
import io.cdap.cdap.graphql.cdap.schema.GraphQLFields;
import io.cdap.cdap.graphql.store.application.schema.ApplicationFields;
import io.cdap.cdap.graphql.store.metadata.dto.Metadata;
import io.cdap.cdap.graphql.store.metadata.dto.Tag;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Fetchers to get metadata
 */
public class MetadataDataFetcher {

  private static final MetadataDataFetcher INSTANCE = new MetadataDataFetcher();

  private final MetadataClient metadataClient;

  @Inject
  private MetadataDataFetcher() {
    // TODO the client config should, somehow, get passed
    this.metadataClient = new MetadataClient(ClientConfig.getDefault());
  }

  public static MetadataDataFetcher getInstance() {
    return INSTANCE;
  }

  /**
   * Fetcher to get MetadataRecords
   *
   * @return the data fetcher
   */
  public DataFetcher getMetadataDataFetcher() {
    return AsyncDataFetcher.async(
      dataFetchingEnvironment -> {
        Map<String, Object> localContext = dataFetchingEnvironment.getLocalContext();
        String namespace = (String) localContext.get(GraphQLFields.NAMESPACE);
        String application = (String) localContext.get(GraphQLFields.NAME);
        MetadataEntity metadataEntity = MetadataEntity.builder()
          .append(GraphQLFields.NAMESPACE, namespace)
          .append(ApplicationFields.APPLICATION, application)
          .build();

        Set<MetadataRecord> metadataRecords = metadataClient.getMetadata(metadataEntity);
        Set<Tag> tags = getTags(metadataRecords);

        return new Metadata(tags);
      }
    );
  }

  private Set<Tag> getTags(Set<MetadataRecord> metadataRecords) {
    Set<Tag> tags = new HashSet<>();

    for (MetadataRecord metadataRecord : metadataRecords) {
      Set<String> metadataRecordTags = metadataRecord.getTags();

      if (metadataRecordTags.isEmpty()) {
        continue;
      }

      String scope = metadataRecord.getScope().name();

      for (String metadataRecordTag : metadataRecordTags) {
        Tag tag = new Tag(metadataRecordTag, scope);
        tags.add(tag);
      }
    }

    return tags;
  }
}
