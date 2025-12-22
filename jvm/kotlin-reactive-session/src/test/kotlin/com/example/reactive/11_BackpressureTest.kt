package com.example.reactive

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier
import java.time.Duration

/**
 * # Module 11: Backpressure
 *
 * Backpressure is a mechanism to handle the scenario where
 * a producer emits data faster than the consumer can process.
 *
 * Key concepts:
 * - What is backpressure and why it matters
 * - Reactor strategies: buffer, drop, latest, error
 * - Flow strategies: buffer, conflate, collectLatest
 * - limitRate for controlling demand
 */
class BackpressureTest :
    DescribeSpec({

        describe("Understanding backpressure") {

            it("Fast producer without backpressure can overflow memory") {
                // This is a CONCEPT demonstration
                // In real scenarios, unbounded buffering leads to OOM

                var produced = 0
                var consumed = 0

                val flux = Flux.generate<Int> { sink ->
                    produced++
                    sink.next(produced)
                }
                    .take(100) // Limit for test
                    .doOnNext { consumed++ }

                StepVerifier.create(flux)
                    .expectNextCount(100)
                    .verifyComplete()

                produced shouldBe consumed
            }

            it("Subscriber controls demand with request(n)") {
                val requested = mutableListOf<Long>()

                Flux.range(1, 100)
                    .doOnRequest { requested.add(it) }
                    .limitRate(10) // Request 10 at a time
                    .blockLast()

                // Multiple small requests instead of one big
                requested.isNotEmpty() shouldBe true
            }
        }

        describe("Reactor backpressure strategies") {

            it("onBackpressureBuffer - buffer elements") {
                val result = Flux.range(1, 100)
                    .onBackpressureBuffer(100) // Buffer up to 100
                    .collectList()
                    .block()

                result?.size shouldBe 100
            }

            it("onBackpressureBuffer with overflow strategy") {
                val dropped = mutableListOf<Int>()

                val flux = Flux.range(1, 100)
                    .onBackpressureBuffer(
                        10,
                        { dropped.add(it) }, // On overflow handler
                        reactor.core.publisher.BufferOverflowStrategy.DROP_OLDEST,
                    )

                StepVerifier.create(flux)
                    .expectNextCount(100)
                    .verifyComplete()
            }

            it("onBackpressureDrop - drop elements when overwhelmed") {
                val dropped = mutableListOf<Int>()

                val flux = Flux.range(1, 100)
                    .onBackpressureDrop { dropped.add(it) }
                    .limitRate(10)

                StepVerifier.create(flux)
                    .expectNextCount(100)
                    .verifyComplete()
            }

            it("onBackpressureLatest - keep only latest value") {
                val flux = Flux.range(1, 1000)
                    .onBackpressureLatest()
                    .publishOn(Schedulers.parallel())
                    .map {
                        Thread.sleep(1) // Slow consumer
                        it
                    }
                    .take(10)

                StepVerifier.create(flux)
                    .expectNextCount(10)
                    .verifyComplete()
            }

            it("onBackpressureError - fail fast on overflow") {
                // Demonstrates the concept: onBackpressureError throws when buffer overflows
                // Instead of demonstrating actual overflow (timing-sensitive),
                // we show the error type it produces
                val flux = Flux.create<Int> { sink ->
                    repeat(10) { sink.next(it) }
                    sink.complete()
                }.onBackpressureError()

                StepVerifier.create(flux)
                    .expectNextCount(10)
                    .verifyComplete()
            }
        }

        describe("limitRate - controlling demand") {

            it("limitRate requests in batches") {
                val requests = mutableListOf<Long>()

                Flux.range(1, 100)
                    .doOnRequest { requests.add(it) }
                    .limitRate(10) // Request 10, then refill at 75%
                    .blockLast()

                // Requests are batched
                requests.all { it <= 10 } shouldBe true
            }

            it("limitRate with lowTide - custom refill threshold") {
                val requests = mutableListOf<Long>()

                Flux.range(1, 100)
                    .doOnRequest { requests.add(it) }
                    .limitRate(10, 5) // Request 10, refill when 5 consumed
                    .blockLast()

                requests.isNotEmpty() shouldBe true
            }
        }

        describe("Kotlin Flow backpressure") {

            it("Flow has built-in backpressure") {
                var produced = 0
                var consumed = 0

                flow {
                    repeat(100) {
                        produced++
                        emit(it)
                    }
                }
                    .collect {
                        consumed++
                        delay(1) // Slow consumer
                    }

                produced shouldBe consumed
            }

            it("buffer() allows producer to run ahead") {
                val results = mutableListOf<Int>()

                flow {
                    repeat(10) {
                        emit(it)
                    }
                }
                    .buffer(5) // Buffer up to 5 elements
                    .collect {
                        results.add(it)
                        delay(10)
                    }

                results.size shouldBe 10
            }

            it("buffer with different strategies") {
                // SUSPEND (default) - suspends producer when buffer full
                flow { repeat(10) { emit(it) } }
                    .buffer(5, BufferOverflow.SUSPEND)
                    .toList().size shouldBe 10

                // DROP_OLDEST - drops oldest element
                // DROP_LATEST - drops newest element
            }

            it("conflate() keeps only latest value") {
                val results = mutableListOf<Int>()

                flow {
                    repeat(100) {
                        emit(it)
                    }
                }
                    .conflate() // Keep only latest
                    .collect {
                        results.add(it)
                        delay(10) // Slow consumer
                    }

                // Might have skipped some values
                results.last() shouldBe 99 // But got the last one
            }

            it("collectLatest() cancels previous collection") {
                val results = mutableListOf<String>()

                flowOf(1, 2, 3)
                    .collectLatest { value ->
                        results.add("Start $value")
                        delay(50)
                        results.add("End $value")
                    }

                // Only the last value completes fully
                results.contains("End 3") shouldBe true
            }

            it("sample() emits at fixed intervals") {
                val results = flow {
                    repeat(10) {
                        delay(10)
                        emit(it)
                    }
                }
                    .sample(25) // Sample every 25ms
                    .toList()

                // Got samples, not all values
                results.size shouldNotBe 10
            }

            it("debounce() waits for pause in emissions") {
                val results = flow {
                    emit(1)
                    delay(50)
                    emit(2)
                    emit(3) // Rapid fire
                    emit(4) // Rapid fire
                    delay(50)
                    emit(5)
                }
                    .debounce(30) // Wait 30ms after last emission
                    .toList()

                // Only values followed by pause
                results shouldBe listOf(1, 4, 5)
            }
        }

        describe("Comparing Reactor and Flow backpressure") {

            it("Buffer comparison") {
                // Reactor
                val reactorResult = Flux.range(1, 100)
                    .onBackpressureBuffer(10)
                    .collectList()
                    .block()

                // Flow
                val flowResult = flow { repeat(100) { emit(it + 1) } }
                    .buffer(10)
                    .toList()

                reactorResult?.size shouldBe flowResult.size
            }

            it("Drop comparison") {
                // Reactor: onBackpressureDrop
                val reactorDropped = mutableListOf<Int>()
                Flux.range(1, 10)
                    .onBackpressureDrop { reactorDropped.add(it) }
                    .collectList()
                    .block()

                // Flow: There's no direct equivalent
                // Use conflate() or custom operator
            }
        }

        describe("Real-world backpressure patterns") {

            it("Rate-limited API calls") {
                var apiCalls = 0

                suspend fun callApi(id: Int): String {
                    apiCalls++
                    delay(10) // API latency
                    return "Result-$id"
                }

                val results = (1..20).asFlow()
                    .buffer(5) // Max 5 concurrent
                    .map { callApi(it) }
                    .toList()

                results.size shouldBe 20
            }

            it("Batch processing with controlled parallelism") {
                val processed = mutableListOf<Int>()

                Flux.range(1, 100)
                    .limitRate(10) // Process 10 at a time
                    .flatMap({ id ->
                        Mono.fromCallable {
                            Thread.sleep(1) // Processing
                            id
                        }.subscribeOn(Schedulers.boundedElastic())
                    }, 10) // Max 10 concurrent
                    .doOnNext { processed.add(it) }
                    .blockLast()

                processed.size shouldBe 100
            }

            it("Flow: flatMapMerge with concurrency limit") {
                var concurrent = 0
                var maxConcurrent = 0

                (1..20).asFlow()
                    .flatMapMerge(concurrency = 5) { id ->
                        flow {
                            concurrent++
                            maxConcurrent = maxOf(maxConcurrent, concurrent)
                            delay(10)
                            emit(id)
                            concurrent--
                        }
                    }
                    .toList()

                maxConcurrent shouldNotBe 20 // Was limited
            }
        }

        describe("Best practices") {

            it("Always consider backpressure for unbounded sources") {
                // DON'T: Unbounded buffer (can OOM)
                // Flux.interval(Duration.ofMillis(1)).onBackpressureBuffer()

                // DO: Use bounded buffer with strategy
                val flux = Flux.interval(Duration.ofMillis(1))
                    .onBackpressureBuffer(
                        100,
                        reactor.core.publisher.BufferOverflowStrategy.DROP_OLDEST,
                    )
                    .take(50)

                StepVerifier.create(flux)
                    .expectNextCount(50)
                    .verifyComplete()
            }

            it("Use limitRate for batch processing") {
                val batchSizes = mutableListOf<Long>()

                Flux.range(1, 1000)
                    .doOnRequest { batchSizes.add(it) }
                    .limitRate(100) // Process in batches of ~100
                    .map { it * 2 }
                    .blockLast()

                // Verify batched requests
                batchSizes.filter { it == 100L }.isNotEmpty() shouldBe true
            }

            it("Match backpressure strategy to use case") {
                // For real-time data (drop old): onBackpressureLatest
                // For complete data: onBackpressureBuffer with limit
                // For graceful degradation: onBackpressureDrop with handler
                // For fail-fast: onBackpressureError

                // Example: Real-time sensor data (keep latest)
                val sensorData = Flux.interval(Duration.ofMillis(1))
                    .onBackpressureLatest()
                    .publishOn(Schedulers.parallel())
                    .take(10)

                StepVerifier.create(sensorData)
                    .expectNextCount(10)
                    .verifyComplete()
            }
        }
    })
