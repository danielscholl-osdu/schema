/* Copyright © Amazon Web Services

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/
package org.opengroup.osdu.schema.provider.aws.impl.messagebus;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.schema.provider.interfaces.messagebus.IMessageBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageBusImpl implements IMessageBus{

	@Autowired
	private JaxRsDpsLog logger;

	@Override
	public void publishMessage(String schemaId, String eventType) {
		// TODO Auto-generated method stub
		logger.warning("publish message not implemented yet");
	}

	@Override
	public void publishMessageForSystemSchema(String schemaId, String eventType) {
		// TODO Auto-generated method stub
		logger.warning("publish message not implemented yet");
	}

}
