package io.nats.client.proto;

import io.nats.client.*;
import io.nats.client.impl.VertxDataPort;
import io.nats.client.impl.VertxDispatchExecutorImpl;
import io.vertx.core.Vertx;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static io.nats.client.Options.DEFAULT_URL;

public class ProtoTLS {

    public static void main(String [] args) {
        try {

            final Options.Builder builder = new Options.Builder();
            builder.server(DEFAULT_URL);

            final Vertx vertx = Vertx.vertx();
            VertxDataPort.setVertx(vertx);
            builder.executor(()->new VertxDispatchExecutorImpl(vertx));
            builder.callbackExecutor(()->new VertxDispatchExecutorImpl(vertx));
            builder.connectionExecutor(()->new VertxDispatchExecutorImpl(vertx));
            builder.dataPortType(VertxDataPort.class.getCanonicalName());
            builder.connectionTimeout(Duration.ofSeconds(30));
            builder.tlsAlgorithm("SunX509");
            builder.tlsKeystorePassword("password".toCharArray());
            builder.tlsTruststorePassword("password".toCharArray());
            builder.tlsKeystorePath("environments/tls/keystore.jks");
            builder.tlsTruststorePath("environments/tls/truststore.jks");
            //builder.sslContext(MTLS.createSSLContext());

            //builder.sslContext(SSLContext.getDefault());
            builder.secure();
            final Connection connect1 = Nats.connect(builder.build());
            final Connection connect3 = Nats.connect(builder.build());
            final Connection connect2 = Nats.connect(builder.build());
            final Subscription subscription = connect1.subscribe("foo");
            final Dispatcher dispatcher = connect3.createDispatcher(message -> {
                System.out.println("D MESSAGE from " + message.getSubject());
                System.out.println("D Message " + new String(message.getData(), StandardCharsets.UTF_8));
            });
            dispatcher.subscribe("foo");

            Thread.sleep(1000);
            for (int i = 0; i < 10; i++) {
                connect2.publish("foo", ("bar " + i).getBytes(StandardCharsets.UTF_8));
            }
            Thread.sleep(1000);

            Message message = subscription.nextMessage(Duration.ofSeconds(30));
            while (message!=null) {
                System.out.println("MESSAGE from " + message.getSubject());
                System.out.println("Message " + new String(message.getData(), StandardCharsets.UTF_8));
                message = subscription.nextMessage(Duration.ofMillis(100));
            }
            connect2.close();
            connect1.close();
            connect3.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
