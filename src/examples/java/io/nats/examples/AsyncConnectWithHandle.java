// Copyright 2024 The NATS Authors
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

package io.nats.examples;

import io.nats.client.*;

import java.util.concurrent.CountDownLatch;

public class AsyncConnectWithHandle {

    /**
     * 'connectAsynchronouslyWithHandle' does two things:
     * - asynchronously connects to NATS, like what `connectAsynchronously` already did
     * - but, it also returns a `Connection` handle that you can call `nc.subscribe` on
     */
    public static void main(String[] args) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Options options = Options.builder()
                .server(Options.DEFAULT_URL)
                .build();
        boolean reconnectOnConnect = true;

        // Asynchronously connects, but also returns a `Connection` handle.
        try (Connection nc = Nats.connectAsynchronouslyWithHandle(options, reconnectOnConnect)) {

            // We can already call subscribe, even if there is no connection yet.
            // Need to be careful with JetStream and request/reply, which of course will time out if no connection was made yet.
            Dispatcher dispatcher = nc.createDispatcher();
            dispatcher.subscribe("hello", msg -> {
                System.out.println("received hello");
            });

            // Await indefinitely.
            latch.await();
        }
    }
}
