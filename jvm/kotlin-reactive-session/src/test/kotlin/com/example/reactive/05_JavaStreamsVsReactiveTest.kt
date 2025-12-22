package com.example.reactive

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * # Module 5: Java Streams vs Reactive Streams
 *
 * This module demonstrates WHY Java Streams cannot replace Flux/Flow.
 * Understanding these differences is crucial for making the right choice.
 *
 * Key differences:
 * - Execution model (pull vs push)
 * - Reusability
 * - Async support
 * - Backpressure
 * - Threading model
 */
class JavaStreamsVsReactiveTest :
    DescribeSpec({

        describe("Java Streams are SINGLE-USE") {

            it("Stream throws IllegalStateException on reuse") {
                val stream = listOf(1, 2, 3).stream()

                // First terminal operation works
                val count = stream.count()
                count shouldBe 3

                // Second terminal operation fails!
                shouldThrow<IllegalStateException> {
                    stream.collect(Collectors.toList())
                }
            }

            it("Flux is REUSABLE - each subscription creates new execution") {
                var executionCount = 0

                val flux = Flux.fromIterable(listOf(1, 2, 3))
                    .doOnSubscribe { executionCount++ }

                // Multiple subscriptions work fine
                flux.collectList().block()
                flux.collectList().block()
                flux.collectList().block()

                executionCount shouldBe 3 // Executed 3 times
            }

            it("Flow is REUSABLE - each collection creates new execution") {
                var executionCount = 0

                val flow = flow {
                    executionCount++
                    emit(1)
                    emit(2)
                    emit(3)
                }

                runTest {
                    flow.toList()
                    flow.toList()
                    flow.toList()
                }

                executionCount shouldBe 3 // Executed 3 times
            }
        }

        describe("Java Streams are SYNCHRONOUS and BLOCKING") {

            it("Stream terminal operations BLOCK until complete") {
                val startTime = System.currentTimeMillis()

                // This blocks the calling thread
                val result = (1..5).asSequence()
                    .map {
                        Thread.sleep(50) // Simulating work
                        it * 2
                    }
                    .toList()

                val elapsed = System.currentTimeMillis() - startTime

                result shouldBe listOf(2, 4, 6, 8, 10)
                // Blocking: took at least 250ms
                elapsed shouldNotBe 0
            }

            it("Flux doesn't block - uses reactive subscription") {
                val results = mutableListOf<Int>()

                // This returns immediately - just sets up the pipeline
                val flux = Flux.range(1, 5)
                    .delayElements(java.time.Duration.ofMillis(10))
                    .doOnNext { results.add(it) }

                results.size shouldBe 0 // Nothing yet!

                // Now we subscribe and wait
                flux.blockLast()

                results.size shouldBe 5 // Now we have results
            }

            it("Flow doesn't block caller - suspends instead") {
                runTest {
                    val results = mutableListOf<Int>()

                    val flow = flow {
                        repeat(5) {
                            delay(10) // Suspends, doesn't block!
                            emit(it)
                        }
                    }

                    // Collect suspends the coroutine, not the thread
                    flow.collect { results.add(it) }

                    results.size shouldBe 5
                }
            }
        }

        describe("Java Streams CANNOT handle async sources naturally") {

            it("Stream awkwardly handles CompletableFutures") {
                // With Java Streams, you have to manually join futures
                val futures = listOf(
                    CompletableFuture.supplyAsync { "result1" },
                    CompletableFuture.supplyAsync { "result2" },
                )

                // Option 1: Block on each future (defeats async purpose)
                val results = futures.stream()
                    .map { it.join() } // BLOCKS on each!
                    .collect(Collectors.toList())

                results shouldBe listOf("result1", "result2")

                // Option 2: Use CompletableFuture.allOf (outside Stream)
                // Not composable with Stream operators
            }

            it("Flux naturally composes async operations") {
                fun asyncFetch(id: Int): Mono<String> = Mono.fromCallable { "result$id" }
                    .subscribeOn(Schedulers.boundedElastic())

                val flux = Flux.just(1, 2, 3)
                    .flatMap { asyncFetch(it) } // Non-blocking async!

                StepVerifier.create(flux)
                    .expectNextCount(3)
                    .verifyComplete()
            }

            it("Flow naturally integrates with suspend functions") {
                suspend fun asyncFetch(id: Int): String {
                    delay(10) // Simulating async call
                    return "result$id"
                }

                runTest {
                    val results = flowOf(1, 2, 3)
                        .map { asyncFetch(it) } // Natural integration!
                        .toList()

                    results shouldBe listOf("result1", "result2", "result3")
                }
            }
        }

        describe("Java Streams have NO backpressure") {

            it("Stream with fast producer can overwhelm memory") {
                // WARNING: This could cause OOM with truly infinite stream!
                // Using limited example for safety

                val limited = Stream.iterate(0) { it + 1 }
                    .limit(1000) // Must artificially limit
                    .collect(Collectors.toList())

                limited.size shouldBe 1000

                // With infinite stream and no limit:
                // Stream.iterate(0) { it + 1 }.collect(Collectors.toList())
                // This would run forever and eventually OOM!
            }

            it("Flux handles infinite sources with backpressure") {
                // Flux can handle infinite sources because of backpressure
                val flux = Flux.range(0, Int.MAX_VALUE)
                    .take(1000) // Consumer controls how much to take

                StepVerifier.create(flux)
                    .expectNextCount(1000)
                    .verifyComplete()
            }

            it("Flow handles infinite sources naturally") {
                runTest {
                    val infiniteFlow = flow {
                        var i = 0
                        while (true) {
                            emit(i++)
                            // Consumer backpressure is automatic
                        }
                    }

                    val results = infiniteFlow
                        .take(1000) // Consumer controls
                        .toList()

                    results.size shouldBe 1000
                }
            }
        }

        describe("Java Streams are THREAD-BOUND") {

            it("Stream runs on caller thread (or parallel stream pool)") {
                val threads = mutableSetOf<String>()
                val mainThread = Thread.currentThread().name

                listOf(1, 2, 3).stream()
                    .map {
                        threads.add(Thread.currentThread().name)
                        it
                    }
                    .collect(Collectors.toList())

                // Sequential stream runs on caller thread
                threads.size shouldBe 1
                threads.first() shouldBe mainThread
            }

            it("Parallel Stream uses ForkJoinPool - limited control") {
                val threads = mutableSetOf<String>()

                listOf(1, 2, 3, 4, 5, 6, 7, 8).parallelStream()
                    .map {
                        threads.add(Thread.currentThread().name)
                        Thread.sleep(10)
                        it
                    }
                    .collect(Collectors.toList())

                // Uses ForkJoinPool.commonPool() by default
                // You have limited control over threading
                threads.isNotEmpty() shouldBe true
            }

            it("Flux is threading agnostic - you control where it runs") {
                val emissionThread = mutableListOf<String>()
                val processingThread = mutableListOf<String>()

                Flux.just(1, 2, 3)
                    .doOnNext { emissionThread.add(Thread.currentThread().name) }
                    .subscribeOn(Schedulers.boundedElastic()) // Emission thread
                    .publishOn(Schedulers.parallel()) // Processing thread
                    .doOnNext { processingThread.add(Thread.currentThread().name) }
                    .blockLast()

                // Emission and processing can be on different thread pools
                emissionThread.isNotEmpty() shouldBe true
                processingThread.isNotEmpty() shouldBe true
            }

            it("Flow threading controlled by flowOn and dispatchers") {
                runTest {
                    val threads = mutableListOf<String>()

                    flow {
                        threads.add("emit: ${Thread.currentThread().name}")
                        emit(1)
                    }
                        .flowOn(kotlinx.coroutines.Dispatchers.Default)
                        .map {
                            threads.add("map: ${Thread.currentThread().name}")
                            it
                        }
                        .collect()

                    threads.size shouldBe 2
                }
            }
        }

        describe("Error handling comparison") {

            it("Stream exception breaks the entire pipeline") {
                shouldThrow<RuntimeException> {
                    listOf(1, 2, 3, 4, 5).stream()
                        .map {
                            if (it == 3) throw RuntimeException("Error at 3")
                            it * 2
                        }
                        .collect(Collectors.toList())
                }
                // No built-in recovery - exception propagates
            }

            it("Flux has rich error handling operators") {
                val flux = Flux.just(1, 2, 3, 4, 5)
                    .map {
                        if (it == 3) throw RuntimeException("Error at 3")
                        it * 2
                    }
                    .onErrorResume { Flux.just(-1) } // Recovery!

                StepVerifier.create(flux)
                    .expectNext(2, 4, -1) // Error replaced with fallback
                    .verifyComplete()
            }

            it("Flow has catch operator for error recovery") {
                runTest {
                    val result = flow {
                        emit(1)
                        emit(2)
                        throw RuntimeException("Error!")
                    }
                        .map { it * 2 }
                        .catch { emit(-1) } // Recovery!
                        .toList()

                    result shouldBe listOf(2, 4, -1)
                }
            }
        }

        describe("Lazy evaluation comparison") {

            it("Stream is lazy but blocks on terminal operation") {
                var executed = false

                val stream = listOf(1, 2, 3).stream()
                    .peek { executed = true } // Use peek for side effect
                    .map { it * 2 }

                executed shouldBe false // Lazy!

                // Use toList() as terminal operation - count() may be optimized to skip peek
                stream.toList() // Terminal operation BLOCKS

                executed shouldBe true
            }

            it("Flux is lazy AND non-blocking") {
                var executed = false

                val flux = Flux.just(1, 2, 3)
                    .map {
                        executed = true
                        it * 2
                    }

                executed shouldBe false // Lazy!

                // Use block() to ensure execution for test purposes
                flux.blockLast()

                executed shouldBe true
            }

            it("Flow is lazy - nothing happens until collect") {
                var executed = false

                val flow = flow {
                    executed = true
                    emit(1)
                }

                runTest {
                    executed shouldBe false // Lazy!

                    flow.collect()

                    executed shouldBe true
                }
            }
        }

        describe("Summary: When to use what") {

            it("Use Java Streams for simple synchronous transformations") {
                // Good use case for Stream:
                // - In-memory data transformation
                // - No async operations needed
                // - Single-use processing

                val result = listOf("apple", "banana", "cherry")
                    .stream()
                    .filter { it.length > 5 }
                    .map { it.uppercase() }
                    .collect(Collectors.toList())

                result shouldBe listOf("BANANA", "CHERRY")
            }

            it("Use Flux/Flow for async, reactive, or reusable pipelines") {
                // Good use cases for Flux/Flow:
                // - Async data sources (HTTP, DB, messaging)
                // - Event streams
                // - Backpressure-needed scenarios
                // - Reusable pipelines

                runTest {
                    // Simulating async data fetching
                    suspend fun fetchFromApi(page: Int): List<String> {
                        delay(10) // Simulating network
                        return listOf("item${page}a", "item${page}b")
                    }

                    // Reactive pipeline for pagination
                    val allItems = (1..3).asFlow()
                        .flatMapConcat { page ->
                            fetchFromApi(page).asFlow()
                        }
                        .toList()

                    allItems.size shouldBe 6
                }
            }
        }
    })
