package io.quarkus.ts.messaging.strimzi.kafka.reactive;

import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.ts.messaging.kafka.StockPrice;
import io.quarkus.ts.messaging.kafka.status;
import io.smallrye.common.constraint.NotNull;

@ApplicationScoped
public class KStockPriceProducer {

    private static final Logger LOG = Logger.getLogger(KStockPriceProducer.class);
    private static final int BATCH_SIZE = 100;

    @Inject
    @Channel("source-stock-price")
    @OnOverflow(value = OnOverflow.Strategy.DROP)
    Emitter<StockPrice> emitter;

    @ConfigProperty(name = "cron.expr.skip", defaultValue = "false")
    String skipCronJob;

    private Random random = new Random();

    @Scheduled(cron = "{cron.expr}")
    public void generate() {
        if (!Boolean.parseBoolean(skipCronJob)) {
            IntStream.range(0, BATCH_SIZE).forEach(next -> {
                StockPrice event = StockPrice.newBuilder()
                        .setId("IBM")
                        .setPrice(random.nextDouble())
                        .setStatus(status.PENDING)
                        .build();
                emitter.send(event).whenComplete(handlerEmitterResponse(KStockPriceProducer.class.getName()));
            });
        }
    }

    @NotNull
    private BiConsumer<Void, Throwable> handlerEmitterResponse(final String owner) {
        return (success, failure) -> {
            if (failure != null) {
                LOG.error(String.format("D'oh! %s", failure.getMessage()));
            }
        };
    }

}
