/*
 * Copyright 2020-2022 Google LLC
 * Copyright 2020-2022 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.schema.configuration;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties(prefix = "gcp.schema-changed")
@Getter
@ConstructorBinding
public class EventMessagingPropertiesConfig {

  private final boolean messagingEnabled;
  private final String topicName;

  public EventMessagingPropertiesConfig(boolean messagingEnabled, String topicName) {
    if (messagingEnabled && StringUtils.isEmpty(topicName)) {
      throw new RuntimeException("Missing event messaging configuration.");
    }

    this.messagingEnabled = messagingEnabled;
    this.topicName = topicName;
  }

}
