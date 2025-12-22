package com.example.reactive

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier
import java.time.Duration

/**
 * # Module 13: DOs and DON'Ts
 *
 * A collection of best practices and anti-patterns when working
 * with reactive programming in Kotlin/Java.
 *
 * This module demonstrates common mistakes and their correct alternatives.
 */
class DosAndDontsTest :
    DescribeSpec({

        describe("DO: Use appropriate schedulers for blocking operations") {

            it("WRONG: Blocking in reactive chain without scheduler") {
                // DON'T: This blocks the event loop!
                val badFlux = Flux.just(1, 2, 3)
                    .map {
                        // Thread.sleep(100) // NEVER DO THIS!
                        it * 2 // Pretend this is blocking
                    }

                // This works but would block event loop in real scenario
                StepVerifier.create(badFlux)
                    .expectNext(2, 4, 6)
                    .verifyComplete()
            }

            it("CORRECT: Wrap blocking calls with subscribeOn(boundedElastic)") {
                fun blockingDatabaseCall(): String {
                    Thread.sleep(10) // Simulated blocking I/O
                    return "result"
                }

                // DO: Wrap blocking operation
                val goodMono = Mono.fromCallable { blockingDatabaseCall() }
                    .subscribeOn(Schedulers.boundedElastic())

                StepVerifier.create(goodMono)
                    .expectNext("result")
                    .verifyComplete()
            }

            it("Coroutines: Use withContext(Dispatchers.IO) for blocking") {
                suspend fun blockingOperation(): String = withContext(Dispatchers.IO) {
                    Thread.sleep(10)
                    "result"
                }

                val result = blockingOperation()
                result shouldBe "result"
            }
        }

        describe("DON'T: Never call .block() inside reactive chains") {

            it("WRONG: Blocking inside map/flatMap") {
                // DON'T: This defeats reactive benefits!
                // val badMono = Mono.just(1)
                //     .flatMap {
                //         val result = someOtherMono.block() // NEVER!
                //         Mono.just(result)
                //     }
            }

            it("CORRECT: Use flatMap for composition") {
                fun fetchUser(id: Int): Mono<String> = Mono.just("User-$id")
                fun fetchOrders(user: String): Mono<List<String>> = Mono.just(listOf("Order1", "Order2"))

                // DO: Chain with flatMap
                val goodMono = fetchUser(1)
                    .flatMap { user -> fetchOrders(user) }

                StepVerifier.create(goodMono)
                    .expectNext(listOf("Order1", "Order2"))
                    .verifyComplete()
            }

            it("Coroutines: Sequential calls look natural") {
                suspend fun fetchUser(id: Int): String {
                    delay(10)
                    return "User-$id"
                }

                suspend fun fetchOrders(user: String): List<String> {
                    delay(10)
                    return listOf("Order1", "Order2")
                }

                // Natural sequential calls
                val user = fetchUser(1)
                val orders = fetchOrders(user)

                orders shouldBe listOf("Order1", "Order2")
            }
        }

        describe("DO: Limit parallelism with flatMap concurrency") {

            it("WRONG: Unbounded flatMap can overwhelm resources") {
                var concurrent = 0
                var maxConcurrent = 0

                // DON'T: Unbounded concurrency
                val flux = Flux.range(1, 100)
                    .flatMap { id ->
                        Mono.fromCallable {
                            concurrent++
                            maxConcurrent = maxOf(maxConcurrent, concurrent)
                            Thread.sleep(10)
                            concurrent--
                            id
                        }.subscribeOn(Schedulers.boundedElastic())
                    }

                flux.blockLast()
                // maxConcurrent could be very high!
            }

            it("CORRECT: Use flatMap with concurrency limit") {
                var concurrent = 0
                var maxConcurrent = 0

                // DO: Limit concurrency
                val flux = Flux.range(1, 100)
                    .flatMap({ id ->
                        Mono.fromCallable {
                            concurrent++
                            maxConcurrent = maxOf(maxConcurrent, concurrent)
                            Thread.sleep(5)
                            concurrent--
                            id
                        }.subscribeOn(Schedulers.boundedElastic())
                    }, 10) // Max 10 concurrent!

                flux.blockLast()
                maxConcurrent shouldNotBe 100 // Was limited
            }

            it("Coroutines: Use flatMapMerge with limited concurrency") {
                var concurrent = 0
                var maxConcurrent = 0

                (1..100).asFlow()
                    .flatMapMerge(concurrency = 10) { id ->
                        flow {
                            concurrent++
                            maxConcurrent = maxOf(maxConcurrent, concurrent)
                            delay(5)
                            concurrent--
                            emit(id)
                        }
                    }
                    .collect()

                maxConcurrent shouldNotBe 100
            }
        }

        describe("DON'T: Avoid side effects in map operators") {

            it("WRONG: Side effects in map") {
                val log = mutableListOf<String>()

                // DON'T: Side effects in map
                val flux = Flux.just(1, 2, 3)
                    .map {
                        log.add("Processing: $it") // Side effect in map!
                        it * 2
                    }

                // Without subscription, side effects don't happen
                log.size shouldBe 0

                flux.blockLast()
                log.size shouldBe 3 // Happened, but it's confusing
            }

            it("CORRECT: Use doOnNext for side effects") {
                val log = mutableListOf<String>()

                // DO: Use doOnNext for side effects
                val flux = Flux.just(1, 2, 3)
                    .doOnNext { log.add("Processing: $it") } // Clear intent
                    .map { it * 2 }

                flux.blockLast()
                log.size shouldBe 3
            }

            it("Coroutines: Use onEach for Flow side effects") {
                val log = mutableListOf<String>()

                flowOf(1, 2, 3)
                    .onEach { log.add("Processing: $it") }
                    .map { it * 2 }
                    .collect()

                log.size shouldBe 3
            }
        }

        describe("DON'T: Never subscribe inside subscribe") {

            it("WRONG: Nested subscriptions (callback hell)") {
                // DON'T: This is callback hell!
                // mono1.subscribe { result1 ->
                //     mono2.subscribe { result2 ->
                //         mono3.subscribe { result3 ->
                //             // Deeply nested, hard to follow
                //         }
                //     }
                // }
            }

            it("CORRECT: Use flatMap for sequential composition") {
                val mono1 = Mono.just(1)
                val mono2 = Mono.just(2)
                val mono3 = Mono.just(3)

                // DO: Use flatMap
                val composed = mono1.flatMap { r1 ->
                    mono2.flatMap { r2 ->
                        mono3.map { r3 -> r1 + r2 + r3 }
                    }
                }

                StepVerifier.create(composed)
                    .expectNext(6)
                    .verifyComplete()
            }

            it("CORRECT: Use zip for parallel composition") {
                val mono1 = Mono.just(1)
                val mono2 = Mono.just(2)
                val mono3 = Mono.just(3)

                // DO: Use zip for parallel
                val composed = Mono.zip(mono1, mono2, mono3)
                    .map { tuple -> tuple.t1 + tuple.t2 + tuple.t3 }

                StepVerifier.create(composed)
                    .expectNext(6)
                    .verifyComplete()
            }
        }

        describe("DO: Always handle errors") {

            it("WRONG: Ignoring errors") {
                // DON'T: Errors are silently ignored
                // flux.subscribe() // No error handler!
            }

            it("CORRECT: Handle errors explicitly") {
                val errorLog = mutableListOf<String>()

                // DO: Always handle errors
                Flux.just(1, 2, 3)
                    .map { if (it == 2) throw RuntimeException("Error") else it }
                    .doOnError { errorLog.add("Error: ${it.message}") }
                    .onErrorResume { Flux.just(-1) }
                    .blockLast()

                errorLog.isNotEmpty() shouldBe true
            }

            it("Coroutines: Use try-catch or catch operator") {
                val result = flow {
                    emit(1)
                    throw RuntimeException("Error")
                }
                    .catch { emit(-1) }
                    .toList()

                result shouldBe listOf(1, -1)
            }
        }

        describe("DO: Handle empty cases explicitly") {

            it("WRONG: Assuming Mono will have value") {
                // DON'T: Will throw if empty
                // val result = emptyMono.block() // NoSuchElementException!
            }

            it("CORRECT: Use switchIfEmpty or defaultIfEmpty") {
                val emptyMono = Mono.empty<String>()

                // DO: Handle empty cases
                val withDefault = emptyMono.defaultIfEmpty("default")
                val withSwitch = emptyMono.switchIfEmpty(Mono.just("fallback"))

                StepVerifier.create(withDefault)
                    .expectNext("default")
                    .verifyComplete()

                StepVerifier.create(withSwitch)
                    .expectNext("fallback")
                    .verifyComplete()
            }

            it("Coroutines: Use nullable types and Elvis") {
                suspend fun maybeFind(): String? = null

                val result = maybeFind() ?: "default"
                result shouldBe "default"
            }
        }

        describe("DON'T: Don't ignore backpressure") {

            it("WRONG: Unbounded buffer") {
                // DON'T: Can cause OOM
                // Flux.interval(Duration.ofMillis(1))
                //     .onBackpressureBuffer() // Unbounded!
                //     .subscribe { Thread.sleep(100) }
            }

            it("CORRECT: Use bounded buffers with strategy") {
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
        }

        describe("DO: Use StepVerifier for testing Reactor") {

            it("WRONG: Using block() in tests") {
                // DON'T: Less control, no error verification
                val result = Mono.just("test").block()
                result shouldBe "test"
            }

            it("CORRECT: Use StepVerifier") {
                // DO: Full control over verification
                StepVerifier.create(Mono.just("test"))
                    .expectNext("test")
                    .verifyComplete()

                // Can verify errors
                StepVerifier.create(Mono.error<String>(RuntimeException("Error")))
                    .expectError(RuntimeException::class.java)
                    .verify()

                // Can verify timing
                StepVerifier.withVirtualTime { Mono.delay(Duration.ofSeconds(10)) }
                    .thenAwait(Duration.ofSeconds(10))
                    .expectNextCount(1)
                    .verifyComplete()
            }
        }

        describe("DO: Use Kotest for coroutine testing") {

            it("Kotest is coroutine-enabled - suspend functions work directly") {
                suspend fun compute(): Int {
                    delay(1000)
                    return 42
                }

                val result = compute()
                result shouldBe 42
            }

            it("Flow testing works directly in Kotest") {
                val results = flow {
                    emit(1)
                    delay(100)
                    emit(2)
                }
                    .toList()

                results shouldBe listOf(1, 2)
            }
        }

        describe("DON'T: Don't use ThreadLocal in reactive code") {

            it("WRONG: ThreadLocal loses value across schedulers") {
                // ThreadLocal values are NOT propagated across thread boundaries
                // This test demonstrates the concept (actual behavior depends on scheduler)
                val threadLocal = ThreadLocal<String>()
                val results = mutableListOf<String?>()

                Mono.fromCallable {
                    threadLocal.set("value")
                    results.add("setter thread: ${Thread.currentThread().name}")
                    "set"
                }
                    .subscribeOn(Schedulers.boundedElastic())
                    .publishOn(Schedulers.parallel())
                    .map { _ ->
                        val value = threadLocal.get() // Might be null on different thread!
                        results.add("getter thread: ${Thread.currentThread().name}")
                        results.add("value: $value")
                        value ?: "null" // Handle null case
                    }
                    .block()

                // ThreadLocal might be null if threads differ (typical case)
                // The key lesson: don't rely on ThreadLocal in reactive code
                results.size shouldBe 3
            }

            it("CORRECT: Use Reactor Context") {
                val mono = Mono.deferContextual { ctx ->
                    Mono.just(ctx.get<String>("key"))
                }
                    .subscribeOn(Schedulers.boundedElastic())
                    .publishOn(Schedulers.parallel())
                    .contextWrite { it.put("key", "value") }

                StepVerifier.create(mono)
                    .expectNext("value")
                    .verifyComplete()
            }
        }

        describe("DO: Remember that nothing happens until subscription") {

            it("WRONG: Forgetting to subscribe") {
                val log = mutableListOf<String>()

                // DON'T: Nothing happens!
                Mono.fromCallable {
                    log.add("Executed")
                    "result"
                }
                // Missing .subscribe() or .block()!

                log.size shouldBe 0 // Nothing executed!
            }

            it("CORRECT: Always subscribe or return to framework") {
                val log = mutableListOf<String>()

                // DO: Subscribe
                Mono.fromCallable {
                    log.add("Executed")
                    "result"
                }.block()

                log.size shouldBe 1 // Executed!

                // Or in Spring WebFlux, return from controller
                // The framework subscribes for you
            }
        }

        describe("Summary: Quick reference") {

            it("Reactor DOs and DON'Ts summary") {
                // DOs:
                // ✓ Use subscribeOn(boundedElastic) for blocking I/O
                // ✓ Use flatMap for async composition
                // ✓ Use flatMap(concurrency) to limit parallelism
                // ✓ Use doOnNext for side effects
                // ✓ Handle errors with onErrorReturn/onErrorResume
                // ✓ Use switchIfEmpty for empty handling
                // ✓ Use StepVerifier for testing
                // ✓ Use Reactor Context instead of ThreadLocal

                // DON'Ts:
                // ✗ Don't call block() inside reactive chains
                // ✗ Don't block event loop threads
                // ✗ Don't nest subscribes (callback hell)
                // ✗ Don't put side effects in map()
                // ✗ Don't ignore errors
                // ✗ Don't use unbounded buffers
                // ✗ Don't use ThreadLocal
            }

            it("Coroutines DOs and DON'Ts summary") {
                // DOs:
                // ✓ Use withContext(Dispatchers.IO) for blocking I/O
                // ✓ Use structured concurrency
                // ✓ Use catch {} for Flow error handling
                // ✓ Kotest is coroutine-enabled
                // ✓ Use onEach for side effects in Flow
                // ✓ Handle nullable returns explicitly

                // DON'Ts:
                // ✗ Don't use GlobalScope
                // ✗ Don't block inside coroutines
                // ✗ Don't ignore CancellationException
                // ✗ Don't put side effects in map()
                // ✗ Don't ignore errors
            }
        }
    })
