/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.build.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link LazyValue}.
 */
@SuppressWarnings("resource")
class LazyValueTest {

    @Test
    void testInitialized() {
        LazyValue<String> lazyValue = new LazyValue<>(() -> "value");
        assertThat(lazyValue.get(), is("value"));
        assertThat(lazyValue.get(), is("value"));
    }

    @Test
    void testBadSupplier() {
        AtomicInteger counter = new AtomicInteger();
        LazyValue<String> lazyValue = new LazyValue<>(() -> {
            if (counter.getAndIncrement() == 0) {
                throw new RuntimeException("error!");
            }
            return "value";
        });
        RuntimeException ex = assertThrows(RuntimeException.class, lazyValue::get);
        assertThat(ex.getMessage(), is("error!"));
        assertThat(lazyValue.get(), is("value"));
    }

    @Test
    void testInitRaceWithBadSupplier() throws InterruptedException, ExecutionException {
        AtomicInteger counter = new AtomicInteger();
        LazyValue<String> lazyValue = new LazyValue<>(() -> {
            if (counter.getAndIncrement() == 0) {
                sleep();
                throw new RuntimeException("error!");
            }
            return Thread.currentThread().getName();
        });

        Deque<Future<String>> futures = new ArrayDeque<>();
        ExecutorService executorService = executorService();

        CountDownLatch latch1 = new CountDownLatch(1);
        futures.add(executorService.submit(() -> {
            latch1.countDown();
            return lazyValue.get();
        }));

        latch1.await();
        futures.add(executorService.submit(lazyValue::get));

        Future<String> first = futures.pop();
        ExecutionException ex = assertThrows(ExecutionException.class, first::get);
        assertThat(ex.getCause(), is(instanceOf(RuntimeException.class)));
        assertThat(ex.getCause().getMessage(), is("error!"));

        while (!futures.isEmpty()) {
            Future<String> future = futures.pop();
            String value = future.get();
            assertThat(value, is(nullValue()));
        }

        CountDownLatch latch2 = new CountDownLatch(1);
        futures.add(executorService.submit(() -> {
            latch2.countDown();
            return lazyValue.get();
        }));
        latch2.await();

        futures.add(executorService.submit(lazyValue::get));

        while (!futures.isEmpty()) {
            Future<String> future = futures.pop();
            String value = future.get();
            assertThat(value, is("test-3"));
        }

        String value = lazyValue.get();
        assertThat(value, is("test-3"));
    }

    @Test
    void testInitRace() throws InterruptedException, ExecutionException {
        LazyValue<String> lazyValue = new LazyValue<>(() -> {
            sleep();
            return Thread.currentThread().getName();
        });

        List<Future<String>> futures = new ArrayList<>();
        ExecutorService executorService = executorService();

        CountDownLatch latch = new CountDownLatch(1);
        futures.add(executorService.submit(() -> {
            latch.countDown();
            return lazyValue.get();
        }));
        latch.await();

        futures.add(executorService.submit(lazyValue::get));
        futures.add(executorService.submit(lazyValue::get));
        futures.add(executorService.submit(lazyValue::get));

        for (Future<String> future : futures) {
            String value = future.get();
            assertThat(value, is("test-1"));
        }

        String value = lazyValue.get();
        assertThat(value, is("test-1"));
    }

    private static ExecutorService executorService() {
        AtomicInteger counter = new AtomicInteger(1);
        return Executors.newFixedThreadPool(4, r -> new Thread(null, r, "test-" + counter.getAndIncrement()));
    }

    private static void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
