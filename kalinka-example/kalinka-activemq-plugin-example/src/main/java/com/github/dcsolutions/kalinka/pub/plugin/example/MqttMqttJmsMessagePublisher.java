/*
 * Copyright [2017] [DCS <Info-dcs@diehl.com>] Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */

package com.github.dcsolutions.kalinka.pub.plugin.example;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.activemq.command.Message;
import com.github.dcsolutions.kalinka.pub.publisher.IMessagePublisher;
import com.github.dcsolutions.kalinka.pub.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.style.ToStringCreator;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * @author michas <michas@jarmoni.org>
 *
 * This is an example user-implementation. It matches the topic names and topic-mapping-semantics described in README.md
 *
 */
public class MqttMqttJmsMessagePublisher implements IMessagePublisher<Message, String, byte[]> {

	private static final Logger LOG = LoggerFactory.getLogger(MqttMqttJmsMessagePublisher.class);

	private static final Pattern REGEX_PATTERN = Pattern.compile("mqtt[\\./](\\S+)[\\./]mqtt[\\./](\\S+).pub");

	private static final String KAFKA_DEST_TOPIC = "{p}.mqtt.mqtt";

	private final int numLogicalPartitions;

	public MqttMqttJmsMessagePublisher(final int numLogicalPartitions) {

		this.numLogicalPartitions = numLogicalPartitions;
	}

	@Override
	public void publish(final Message message, final KafkaTemplate<String, byte[]> kafkaTemplate) {

		try {
			final byte[] effectivePayload = message.getContent().getData();
			final MessageContainer messageContainer = this.createMessageContainer(message.getDestination().getPhysicalName(), effectivePayload);
			LOG.debug("Will send message={}", messageContainer);
			kafkaTemplate.send(messageContainer.topic, messageContainer.key, messageContainer.content);
		} catch (final Throwable t) {
			LOG.error("Exception occured", t);
		}
	}

	MessageContainer createMessageContainer(final String rawSourceTopic, final byte[] effectivePayload) {

		LOG.debug("Creating MessageContainer for rawSourceTopic={}, effectivePayload={}", rawSourceTopic,
				effectivePayload != null ? new String(effectivePayload) : null);

		final SrcDestId srcDestIds = this.getSourceAndDestId(rawSourceTopic);
		if (srcDestIds == null) {
			throw new IllegalStateException("Could not get sourceId and destId from topic=" + rawSourceTopic);
		}
		final byte[] payload = this.getEnrichedPayload(effectivePayload, srcDestIds.getSrcId());
		final String destTopic = this.getDestTopic(srcDestIds.getSrcId());
		return new MessageContainer(destTopic, srcDestIds.getDestId(), payload);
	}

	String getDestTopic(final String sourceId) {

		final int logicalPartition = HashUtil.hashKey(sourceId, this.numLogicalPartitions);
		final String destTopic = KAFKA_DEST_TOPIC.replace("{p}", String.valueOf(logicalPartition));
		return destTopic;
	}


	SrcDestId getSourceAndDestId(final String rawTopic) {

		final Matcher m = this.getSourceTopicRegex().matcher(rawTopic);
		if (m.find()) {
			return new SrcDestId(m.group(1), m.group(2));
		}
		return null;
	}

	byte[] getEnrichedPayload(final byte[] effectivePayload, final String srcId) {

		final byte[] headerBytes = ("{\"srcId\": \"" + srcId + "\"}").getBytes(StandardCharsets.UTF_8);
		final byte[] header = new byte[64];
		System.arraycopy(headerBytes, 0, header, 0, headerBytes.length);
		final byte[] enriched = new byte[header.length + effectivePayload.length];
		System.arraycopy(header, 0, enriched, 0, header.length);
		System.arraycopy(effectivePayload, 0, enriched, header.length, effectivePayload.length);
		return enriched;
	}

	@Override
	public Pattern getSourceTopicRegex() {

		return REGEX_PATTERN;
	}

	public static class MessageContainer {

		private final String topic;
		private final String key;
		private final byte[] content;

		public MessageContainer(final String topic, final String key, final byte[] content) {
			super();
			this.topic = topic;
			this.key = key;
			this.content = content;
		}

		public String getTopic() {
			return topic;
		}

		public String getKey() {
			return key;
		}

		public byte[] getContent() {
			return content;
		}

		@Override
		public String toString() {
			return new ToStringCreator(this).append("topic", this.topic).append("key", this.key)
					.append("content", this.content != null ? new String(this.content) : null).toString();
		}
	}

	public static class SrcDestId {

		private final String srcId;
		private final String destId;

		public SrcDestId(final String srcId, final String destId) {

			this.srcId = srcId;
			this.destId = destId;
		}

		public String getSrcId() {
			return srcId;
		}

		public String getDestId() {
			return destId;
		}


	}
}
