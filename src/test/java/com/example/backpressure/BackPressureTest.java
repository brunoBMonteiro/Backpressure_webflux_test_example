package com.example.backpressure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;


@ExtendWith(SpringExtension.class)
class BackpressureTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackpressureTest.class);

    @Test
    void whenLimitRateSet_thenSplitIntoChunks() throws InterruptedException {
        Flux<Integer> limit = Flux.range(1, 25);

        limit.limitRate(10);
        limit.subscribe(
                value -> LOGGER.debug(String.valueOf(value)),
                err -> err.printStackTrace(),
                () -> LOGGER.debug("Finished!!"),
                subscription -> subscription.request(10)
        );

        StepVerifier.create(limit)
                .expectSubscription()
                .thenRequest(15)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .expectNext(11, 12, 13, 14, 15)
                .thenRequest(10)
                .expectNext(16, 17, 18, 19, 20, 21, 22, 23, 24, 25)
                .verifyComplete();
    }

    @Test
    void whenCancel_thenSubscriptionFinished() {
        Flux<Integer> cancel = Flux.range(1, 10).log();

        cancel.subscribe(new BaseSubscriber<Integer>() {
            @Override
            protected void hookOnNext(Integer value) {
                request(3);
                System.out.println(value);
                cancel();
            }
        });

        StepVerifier.create(cancel)
                .expectNext(1, 2, 3, 4, 5)
                .thenCancel()
                .verify();
    }

}