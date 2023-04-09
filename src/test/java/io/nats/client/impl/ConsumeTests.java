// Copyright 2023 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.client.impl;

import io.nats.client.*;
import io.nats.client.api.ConsumerConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.nats.client.BaseConsumeOptions.*;
import static org.junit.jupiter.api.Assertions.*;

public class ConsumeTests extends JetStreamTestBase {

    @Test
    public void testFetch() throws Exception {
        runInJsServer(nc -> {
            createDefaultTestStream(nc);
            JetStream js = nc.jetStream();
            for (int x = 1; x <= 20; x++) {
                js.publish(SUBJECT, ("test-fetch-msg-" + x).getBytes());
            }

            // 1. Different fetch sizes demonstrate expiration behavior

            // 1A. equal number of messages than the fetch size
            _testFetch(nc, 20, 0);

            // 1B. more messages than the fetch size
            _testFetch(nc, 10, 0);

            // 1C. fewer messages than the fetch size
            _testFetch(nc, 40, 0);

            // don't test bytes before 2.9.1
            if (nc.getServerInfo().isOlderThanVersion("2.9.1")) {
                return;
            }

            // 2. Different max bytes sizes demonstrate expiration behavior
            //    - each test message is approximately 100 bytes

            // 2A. max bytes is reached before message count
            _testFetch(nc, 20, 750);

            // 2B. fetch size is reached before byte count
            _testFetch(nc, 10, 1500);

            // 2C. fewer bytes than the byte count
            _testFetch(nc, 40, 3000);
        });
    }

    private static void _testFetch(Connection nc, int maxMessages, int maxBytes) throws IOException, JetStreamApiException, InterruptedException {
        JetStreamManagement jsm = nc.jetStreamManagement();
        JetStream js = nc.jetStream();

        String name = generateConsumerName(maxMessages, maxBytes);

        // Pre define a consumer
        ConsumerConfiguration cc = ConsumerConfiguration.builder().durable(name).build();
        jsm.addOrUpdateConsumer(STREAM, cc);

        // Consumer[Context]
        ConsumerContext consumerContext = js.getConsumerContext(STREAM, name);

        // Custom consume options
        FetchConsumeOptions fetchConsumeOptions = FetchConsumeOptions.builder()
            .maxMessages(maxMessages)        // usually you would use only one or the other
            .maxBytes(maxBytes, maxMessages) // /\                                    /\
            .expiresIn(1500)
            .build();

        long start = System.currentTimeMillis();

        // create the consumer then use it
        FetchConsumer consumer = consumerContext.fetch(fetchConsumeOptions);
        int rcvd = 0;
        Message msg = consumer.nextMessage();
        while (msg != null) {
            ++rcvd;
            msg.ack();
            msg = consumer.nextMessage();
        }
        long elapsed = System.currentTimeMillis() - start;

        if (maxBytes > 0) {
            if (maxMessages > 20) {
                assertTrue(rcvd < maxMessages);
                assertTrue(elapsed >= 1500);
            }
            else if (maxMessages * 100 > maxBytes) {
                assertTrue(rcvd < maxMessages);
                assertTrue(elapsed < 250);
            }
            else {
                assertEquals(maxMessages, rcvd);
                assertTrue(elapsed < 250);
            }
        }
        else if (maxMessages > 20) {
            assertTrue(rcvd < maxMessages);
            assertTrue(elapsed >= 1500);
        }
        else {
            assertEquals(maxMessages, rcvd);
            assertTrue(elapsed < 250);
        }
    }

    private static String generateConsumerName(int maxMessages, int maxBytes) {
        return maxBytes == 0
            ? NAME + "-" + maxMessages + "msgs"
            : NAME + "-" + maxBytes + "bytes-" + maxMessages + "msgs";
    }

    @Test
    public void testFetchConsumeOptionsBuilder() {
        FetchConsumeOptions fco = FetchConsumeOptions.builder().build();
        assertEquals(DEFAULT_MESSAGE_COUNT, fco.getMaxMessages());
        assertEquals(DEFAULT_EXPIRES_IN_MS, fco.getExpires());
        assertEquals(Math.max(1, DEFAULT_MESSAGE_COUNT * DEFAULT_THRESHOLD_PERCENT / 100), fco.getThresholdMessages());
        assertEquals(0, fco.getMaxBytes());
        assertEquals(0, fco.getThresholdBytes());
        assertEquals(DEFAULT_EXPIRES_IN_MS * MAX_IDLE_HEARTBEAT_PCT / 100, fco.getIdleHeartbeat());

        fco = FetchConsumeOptions.builder().maxMessages(1000).build();
        assertEquals(1000, fco.getMaxMessages());
        assertEquals(1000 * DEFAULT_THRESHOLD_PERCENT / 100, fco.getThresholdMessages());
        assertEquals(0, fco.getMaxBytes());
        assertEquals(0, fco.getThresholdBytes());

        fco = FetchConsumeOptions.builder().maxMessages(1000).thresholdPercent(50).build();
        assertEquals(1000, fco.getMaxMessages());
        assertEquals(500, fco.getThresholdMessages());
        assertEquals(0, fco.getMaxBytes());
        assertEquals(0, fco.getThresholdBytes());

        fco = FetchConsumeOptions.builder().maxBytes(1000, 100).build();
        assertEquals(100, fco.getMaxMessages());
        assertEquals(100 * DEFAULT_THRESHOLD_PERCENT / 100, fco.getThresholdMessages());
        assertEquals(1000, fco.getMaxBytes());
        assertEquals(1000 * DEFAULT_THRESHOLD_PERCENT / 100, fco.getThresholdBytes());

        fco = FetchConsumeOptions.builder().maxBytes(1000, 100).thresholdPercent(50).build();
        assertEquals(100, fco.getMaxMessages());
        assertEquals(50, fco.getThresholdMessages());
        assertEquals(1000, fco.getMaxBytes());
        assertEquals(500, fco.getThresholdBytes());
    }

    @Test
    public void testConsumeOptionsBuilder() {
        ConsumeOptions co = ConsumeOptions.builder().build();
        assertEquals(DEFAULT_MESSAGE_COUNT, co.getBatchSize());
        assertEquals(DEFAULT_EXPIRES_IN_MS, co.getExpires());
        assertEquals(Math.max(1, DEFAULT_MESSAGE_COUNT * DEFAULT_THRESHOLD_PERCENT / 100), co.getThresholdMessages());
        assertEquals(0, co.getBatchBytes());
        assertEquals(0, co.getThresholdBytes());
        assertEquals(DEFAULT_EXPIRES_IN_MS * MAX_IDLE_HEARTBEAT_PCT / 100, co.getIdleHeartbeat());

        co = ConsumeOptions.builder().batchSize(1000).build();
        assertEquals(1000, co.getBatchSize());
        assertEquals(1000 * DEFAULT_THRESHOLD_PERCENT / 100, co.getThresholdMessages());
        assertEquals(0, co.getBatchBytes());
        assertEquals(0, co.getThresholdBytes());

        co = ConsumeOptions.builder().batchSize(1000).thresholdPercent(50).build();
        assertEquals(1000, co.getBatchSize());
        assertEquals(500, co.getThresholdMessages());
        assertEquals(0, co.getBatchBytes());
        assertEquals(0, co.getThresholdBytes());

        co = ConsumeOptions.builder().batchBytes(1000, 100).build();
        assertEquals(100, co.getBatchSize());
        assertEquals(100 * DEFAULT_THRESHOLD_PERCENT / 100, co.getThresholdMessages());
        assertEquals(1000, co.getBatchBytes());
        assertEquals(1000 * DEFAULT_THRESHOLD_PERCENT / 100, co.getThresholdBytes());

        co = ConsumeOptions.builder().batchBytes(1000, 100).thresholdPercent(50).build();
        assertEquals(100, co.getBatchSize());
        assertEquals(50, co.getThresholdMessages());
        assertEquals(1000, co.getBatchBytes());
        assertEquals(500, co.getThresholdBytes());

        assertThrows(IllegalArgumentException.class,
            () -> ConsumeOptions.builder().batchSize(-99).build());
        assertThrows(IllegalArgumentException.class,
            () -> ConsumeOptions.builder().batchBytes(-99, 1).build());
        assertThrows(IllegalArgumentException.class,
            () -> ConsumeOptions.builder().thresholdPercent(0).build());
        assertThrows(IllegalArgumentException.class,
            () -> ConsumeOptions.builder().thresholdPercent(-99).build());
        assertThrows(IllegalArgumentException.class,
            () -> ConsumeOptions.builder().thresholdPercent(101).build());
        assertThrows(IllegalArgumentException.class,
            () -> ConsumeOptions.builder().expiresIn(0).build());
        assertThrows(IllegalArgumentException.class,
            () -> ConsumeOptions.builder().expiresIn(MIN_EXPIRES_MILLS - 1).build());
        assertThrows(IllegalArgumentException.class,
            () -> ConsumeOptions.builder().expiresIn(MAX_EXPIRES_MILLIS + 1).build());
    }
}
