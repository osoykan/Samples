package com.example.reactive

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier
import java.util.concurrent.Executors

/**
 * # Module 14: Virtual Threads (Project Loom)
 *
 * Java 21+ introduces Virtual Threads (Project Loom) - lightweight threads
 * managed by the JVM that allow blocking code to scale like non-blocking code.
 *
 * Key concepts:
 * - Virtual vs Platform threads
 * - Reactor + Virtual Threads integration
 * - Kotlin Coroutines + Virtual Threads
 * - When to use Virtual Threads vs Reactive
 *
 * ## Enabling Virtual Threads in Reactor
 *
 * Set the system property:
 * ```
 * -Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true
 * ```
 *
 * When enabled, `Schedulers.boundedElastic()` returns a scheduler
 * backed by virtual threads instead of platform threads.
 */
class VirtualThreadsTest :
    DescribeSpec({

        describe("Virtual Threads basics (Java 21+)") {

            it("Creating virtual threads directly") {
                var isVirtualThread = false

                // Create a virtual thread directly
                val vThread = Thread.ofVirtual()
                    .name("my-virtual-thread")
                    .start {
                        isVirtualThread = Thread.currentThread().isVirtual
                        Thread.sleep(10) // Blocking is OK in virtual threads!
                    }

                vThread.join()

                isVirtualThread shouldBe true
            }

            it("Virtual thread executor") {
                val virtualThreadChecks = java.util.concurrent.ConcurrentLinkedQueue<Boolean>()

                // newVirtualThreadPerTaskExecutor creates virtual threads on demand
                Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                    val futures = (1..100).map { i ->
                        executor.submit {
                            virtualThreadChecks.add(Thread.currentThread().isVirtual)
                            Thread.sleep(10) // Each blocking call gets its own virtual thread
                            "Result $i"
                        }
                    }
                    futures.forEach { it.get() }
                }

                // All tasks ran on virtual threads
                virtualThreadChecks.size shouldBe 100
                virtualThreadChecks.all { it } shouldBe true
            }

            it("Checking if current thread is virtual") {
                var isVirtual = false

                Thread.ofVirtual().start {
                    isVirtual = Thread.currentThread().isVirtual
                }.join()

                isVirtual shouldBe true

                // Platform thread is NOT virtual
                Thread.currentThread().isVirtual shouldBe false
            }
        }

        describe("Reactor with Virtual Threads") {

            it("Custom virtual thread scheduler") {
                var ranOnVirtualThread = false

                // Create a scheduler backed by virtual threads
                val virtualThreadScheduler = Schedulers.fromExecutor(
                    Executors.newVirtualThreadPerTaskExecutor(),
                )

                val mono = Mono.fromCallable {
                    ranOnVirtualThread = Thread.currentThread().isVirtual
                    Thread.sleep(50) // Blocking is now scalable!
                    "result"
                }.subscribeOn(virtualThreadScheduler)

                StepVerifier.create(mono)
                    .expectNext("result")
                    .verifyComplete()

                ranOnVirtualThread shouldBe true
            }

            it("Parallel processing with virtual threads") {
                val virtualChecks = mutableListOf<Boolean>()

                val virtualScheduler = Schedulers.fromExecutor(
                    Executors.newVirtualThreadPerTaskExecutor(),
                )

                val flux = Flux.range(1, 50)
                    .flatMap({ id ->
                        Mono.fromCallable {
                            synchronized(virtualChecks) {
                                virtualChecks.add(Thread.currentThread().isVirtual)
                            }
                            Thread.sleep(10) // Simulate blocking I/O
                            "Item-$id"
                        }.subscribeOn(virtualScheduler)
                    }, 50) // High concurrency - each gets a virtual thread

                StepVerifier.create(flux)
                    .expectNextCount(50)
                    .verifyComplete()

                // Each task ran on a virtual thread
                virtualChecks.all { it } shouldBe true
            }

            it("boundedElastic with virtual threads (when enabled via system property)") {
                // NOTE: This test demonstrates the concept.
                // In production, set: -Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true
                // Then Schedulers.boundedElastic() automatically uses virtual threads

                val threadInfo = mutableListOf<String>()

                // Standard boundedElastic (uses platform threads by default)
                val mono = Mono.fromCallable {
                    threadInfo.add(Thread.currentThread().toString())
                    Thread.sleep(10)
                    "result"
                }.subscribeOn(Schedulers.boundedElastic())

                StepVerifier.create(mono)
                    .expectNext("result")
                    .verifyComplete()

                // With system property enabled, this would show "virtual" instead
                threadInfo.isNotEmpty() shouldBe true
            }
        }

        describe("Kotlin Coroutines with Virtual Threads") {

            it("Virtual thread dispatcher") {
                val virtualDispatcher = Executors.newVirtualThreadPerTaskExecutor()
                    .asCoroutineDispatcher()

                var isVirtual = false

                withContext(virtualDispatcher) {
                    isVirtual = Thread.currentThread().isVirtual
                    Thread.sleep(10) // Blocking is OK
                }

                isVirtual shouldBe true

                virtualDispatcher.close()
            }

            it("Dispatchers.IO with Loom (conceptual)") {
                // NOTE: Kotlin's Dispatchers.IO doesn't use virtual threads by default (yet)
                // But you can create a virtual thread dispatcher for I/O work

                val virtualIo = Executors.newVirtualThreadPerTaskExecutor()
                    .asCoroutineDispatcher()

                val virtualChecks = mutableListOf<Boolean>()

                // Multiple blocking operations scale with virtual threads
                withContext(virtualIo) {
                    repeat(10) {
                        virtualChecks.add(Thread.currentThread().isVirtual)
                        Thread.sleep(5)
                    }
                }

                virtualChecks.all { it } shouldBe true

                virtualIo.close()
            }
        }

        describe("Virtual Threads vs Reactive - When to use which?") {

        /*
         * VIRTUAL THREADS are great for:
         * --------------------------------
         * - Existing blocking code that you want to scale
         * - Simple synchronous programming model
         * - When you have many concurrent blocking I/O operations
         * - Legacy code migration without rewriting
         *
         * REACTIVE (Mono/Flux/Flow) is great for:
         * ----------------------------------------
         * - Streaming data (Server-Sent Events, WebSockets)
         * - Complex async pipelines with operators
         * - Backpressure handling
         * - When you need fine-grained control over concurrency
         * - Non-blocking all the way down (no blocking calls)
         *
         * COMBINING BOTH:
         * ---------------
         * - Use virtual threads for blocking I/O in reactive pipelines
         * - subscribeOn(virtualScheduler) for blocking operations
         * - Keep reactive for streaming/pipeline logic
         */

            it("Virtual threads for simple blocking I/O") {
                val virtualScheduler = Schedulers.fromExecutor(
                    Executors.newVirtualThreadPerTaskExecutor(),
                )

                // Simple blocking service call - virtual threads make this scalable
                fun fetchFromDatabase(id: Int): String {
                    Thread.sleep(10) // Blocking DB call
                    return "Record-$id"
                }

                val mono = Mono.fromCallable { fetchFromDatabase(1) }
                    .subscribeOn(virtualScheduler)

                StepVerifier.create(mono)
                    .expectNext("Record-1")
                    .verifyComplete()
            }

            it("Reactive for streaming with backpressure") {
                // Reactive shines for streaming scenarios
                val flux = Flux.range(1, 1000)
                    .onBackpressureBuffer(100)
                    .map { "Event-$it" }
                    .take(10)

                StepVerifier.create(flux)
                    .expectNextCount(10)
                    .verifyComplete()
            }

            it("Combining: Virtual threads for blocking in reactive pipeline") {
                val virtualScheduler = Schedulers.fromExecutor(
                    Executors.newVirtualThreadPerTaskExecutor(),
                )

                // Blocking operation wrapped for virtual threads
                fun blockingFetch(id: Int): String {
                    Thread.sleep(5)
                    return "Data-$id"
                }

                // Reactive pipeline uses virtual threads for blocking parts
                val flux = Flux.range(1, 20)
                    .flatMap({ id ->
                        Mono.fromCallable { blockingFetch(id) }
                            .subscribeOn(virtualScheduler)
                    }, 20)

                StepVerifier.create(flux)
                    .expectNextCount(20)
                    .verifyComplete()
            }
        }

        describe("Best Practices with Virtual Threads") {

            it("DO: Use virtual threads for blocking I/O") {
                val virtualScheduler = Schedulers.fromExecutor(
                    Executors.newVirtualThreadPerTaskExecutor(),
                )

                // GOOD: Blocking operations on virtual threads
                val mono = Mono.fromCallable {
                    // JDBC call, file I/O, blocking HTTP client, etc.
                    Thread.sleep(10)
                    "result"
                }.subscribeOn(virtualScheduler)

                StepVerifier.create(mono)
                    .expectNext("result")
                    .verifyComplete()
            }

            it("DON'T: Use virtual threads for CPU-bound work") {
                // Virtual threads don't help CPU-bound work
                // They're designed for waiting on I/O, not computation

                // For CPU-bound work, use:
                // - Schedulers.parallel() (Reactor)
                // - Dispatchers.Default (Coroutines)

                val result = Mono.fromCallable {
                    // CPU-bound computation
                    (1..1000).sum()
                }.subscribeOn(Schedulers.parallel()) // Use parallel for CPU work

                StepVerifier.create(result)
                    .expectNext(500500)
                    .verifyComplete()
            }

            it("DON'T: Hold locks for long periods in virtual threads") {
                // Virtual threads can be pinned to platform threads when:
                // - Inside synchronized blocks
                // - During native method calls
                //
                // Use java.util.concurrent.locks.ReentrantLock instead of synchronized
                // when you need locks with virtual threads

                val lock = java.util.concurrent.locks.ReentrantLock()
                var counter = 0

                val virtualScheduler = Schedulers.fromExecutor(
                    Executors.newVirtualThreadPerTaskExecutor(),
                )

                val flux = Flux.range(1, 10)
                    .flatMap({
                        Mono.fromCallable {
                            lock.lock()
                            try {
                                counter++
                            } finally {
                                lock.unlock()
                            }
                            counter
                        }.subscribeOn(virtualScheduler)
                    }, 10)

                StepVerifier.create(flux)
                    .expectNextCount(10)
                    .verifyComplete()

                counter shouldBe 10
            }
        }

        describe("How to enable Virtual Threads in production") {

            it("Configuration options") {
            /*
             * Option 1: System Property (Reactor)
             * ------------------------------------
             * Add to JVM args:
             * -Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true
             *
             * This makes Schedulers.boundedElastic() use virtual threads.
             *
             * Option 2: Programmatic Scheduler
             * ---------------------------------
             * Create your own scheduler:
             * val virtualScheduler = Schedulers.fromExecutor(
             *     Executors.newVirtualThreadPerTaskExecutor()
             * )
             *
             * Option 3: Spring Boot 3.2+
             * --------------------------
             * Add to application.properties:
             * spring.threads.virtual.enabled=true
             *
             * This enables virtual threads for:
             * - Tomcat/Jetty/Undertow request handling
             * - @Async methods
             * - Task scheduling
             */

                // Verify we're on Java 21+ (virtual threads require it)
                val javaVersion = Runtime.version().feature()
                (javaVersion >= 21) shouldBe true
            }
        }
    })
