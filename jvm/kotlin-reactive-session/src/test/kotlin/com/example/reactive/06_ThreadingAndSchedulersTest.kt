package com.example.reactive

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.concurrent.Executors

/**
 * # Module 6: Threading and Schedulers
 *
 * Reactive programming is THREADING AGNOSTIC - you can change
 * where code executes without modifying the business logic.
 *
 * Key concepts:
 * - subscribeOn: controls where subscription happens (source thread)
 * - publishOn: switches thread for downstream operators
 * - flowOn: Kotlin Flow upstream context change
 * - Schedulers (Reactor) vs Dispatchers (Coroutines)
 */
class ThreadingAndSchedulersTest :
    DescribeSpec({

        describe("Reactor Schedulers") {

            it("Schedulers.immediate() - runs on current thread") {
                var threadName = ""

                Mono.just("data")
                    .subscribeOn(Schedulers.immediate())
                    .doOnNext { threadName = Thread.currentThread().name }
                    .block()

                // Runs on the calling thread
                threadName shouldNotBe ""
            }

            it("Schedulers.single() - single reusable thread") {
                val threads = mutableSetOf<String>()

                Flux.range(1, 10)
                    .subscribeOn(Schedulers.single())
                    .doOnNext { threads.add(Thread.currentThread().name) }
                    .blockLast()

                // All work on single thread
                threads.size shouldBe 1
                threads.first() shouldContain "single"
            }

            it("Schedulers.parallel() - for CPU-bound work") {
                val threads = mutableSetOf<String>()

                Flux.range(1, 100)
                    .parallel() // Enable parallel processing
                    .runOn(Schedulers.parallel())
                    .doOnNext { threads.add(Thread.currentThread().name) }
                    .sequential()
                    .blockLast()

                // Uses multiple threads (up to CPU cores)
                threads.size shouldNotBe 1
                threads.forEach { it shouldContain "parallel" }
            }

            it("Schedulers.boundedElastic() - for blocking I/O") {
                var threadName = ""

                Mono.fromCallable {
                    // Simulate blocking I/O
                    Thread.sleep(10)
                    "data"
                }
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnNext { threadName = Thread.currentThread().name }
                    .block()

                threadName shouldContain "boundedElastic"
            }

            it("Schedulers.fromExecutor() - custom thread pool") {
                val customExecutor = Executors.newFixedThreadPool(2) { r ->
                    Thread(r, "custom-thread")
                }
                val customScheduler = Schedulers.fromExecutor(customExecutor)

                var threadName = ""

                Mono.just("data")
                    .subscribeOn(customScheduler)
                    .doOnNext { threadName = Thread.currentThread().name }
                    .block()

                threadName shouldContain "custom-thread"

                customExecutor.shutdown()
            }
        }

        describe("subscribeOn - controls SOURCE thread") {

            it("subscribeOn determines where subscription happens") {
                val threadLog = mutableListOf<String>()

                Flux.create<Int> { sink ->
                    threadLog.add("emit: ${Thread.currentThread().name}")
                    sink.next(1)
                    sink.next(2)
                    sink.complete()
                }
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnNext { threadLog.add("doOnNext: ${Thread.currentThread().name}") }
                    .blockLast()

                // Emission happens on boundedElastic
                threadLog.first() shouldContain "boundedElastic"
            }

            it("ONLY the FIRST subscribeOn takes effect") {
                val threadLog = mutableListOf<String>()

                Flux.create<Int> { sink ->
                    threadLog.add("emit: ${Thread.currentThread().name}")
                    sink.next(1)
                    sink.complete()
                }
                    .subscribeOn(Schedulers.single()) // This wins!
                    .subscribeOn(Schedulers.parallel()) // Ignored
                    .subscribeOn(Schedulers.boundedElastic()) // Ignored
                    .doOnNext { threadLog.add("process: ${Thread.currentThread().name}") }
                    .blockLast()

                // First subscribeOn (closest to source) wins
                threadLog.first() shouldContain "single"
            }

            it("subscribeOn affects the entire upstream chain") {
                val threadLog = mutableListOf<String>()

                Flux.just(1, 2, 3)
                    .doOnNext { threadLog.add("step1: ${Thread.currentThread().name}") }
                    .map { it * 2 }
                    .doOnNext { threadLog.add("step2: ${Thread.currentThread().name}") }
                    .subscribeOn(Schedulers.boundedElastic())
                    .blockLast()

                // All steps run on boundedElastic
                threadLog.all { it.contains("boundedElastic") } shouldBe true
            }
        }

        describe("publishOn - switches DOWNSTREAM thread") {

            it("publishOn switches thread for subsequent operators") {
                val threadLog = mutableListOf<String>()

                Flux.just(1, 2, 3)
                    .doOnNext { threadLog.add("before: ${Thread.currentThread().name}") }
                    .publishOn(Schedulers.parallel())
                    .doOnNext { threadLog.add("after: ${Thread.currentThread().name}") }
                    .blockLast()

                // After publishOn, thread changes
                val beforeThreads = threadLog.filter { it.startsWith("before") }
                val afterThreads = threadLog.filter { it.startsWith("after") }

                afterThreads.all { it.contains("parallel") } shouldBe true
            }

            it("multiple publishOn calls - each switches thread") {
                val threadLog = mutableListOf<String>()

                Flux.just(1)
                    .doOnNext { threadLog.add("start: ${Thread.currentThread().name}") }
                    .publishOn(Schedulers.single())
                    .doOnNext { threadLog.add("single: ${Thread.currentThread().name}") }
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext { threadLog.add("elastic: ${Thread.currentThread().name}") }
                    .blockLast()

                threadLog[1] shouldContain "single"
                threadLog[2] shouldContain "boundedElastic"
            }

            it("publishOn position matters - only affects downstream") {
                val beforePublishOn = mutableListOf<String>()
                val afterPublishOn = mutableListOf<String>()

                Flux.range(1, 3)
                    .doOnNext { beforePublishOn.add(Thread.currentThread().name) }
                    .publishOn(Schedulers.parallel())
                    .doOnNext { afterPublishOn.add(Thread.currentThread().name) }
                    .blockLast()

                // Before and after may be on different threads
                afterPublishOn.all { it.contains("parallel") } shouldBe true
            }
        }

        describe("Combining subscribeOn and publishOn") {

            it("subscribeOn for source, publishOn for processing") {
                val threadLog = mutableListOf<String>()

                Mono.fromCallable {
                    // This is a blocking I/O call
                    threadLog.add("io: ${Thread.currentThread().name}")
                    Thread.sleep(10)
                    "data"
                }
                    .subscribeOn(Schedulers.boundedElastic()) // For blocking I/O
                    .publishOn(Schedulers.parallel()) // For CPU processing
                    .map { data ->
                        threadLog.add("process: ${Thread.currentThread().name}")
                        data.uppercase()
                    }
                    .block()

                threadLog[0] shouldContain "boundedElastic"
                threadLog[1] shouldContain "parallel"
            }

            it("Real-world pattern: fetch on IO, process on CPU") {
                val threadLog = mutableListOf<String>()

                fun fetchFromDatabase(): String {
                    threadLog.add("db: ${Thread.currentThread().name}")
                    Thread.sleep(10) // Simulate blocking DB call
                    return "raw-data"
                }

                fun processData(data: String): String {
                    threadLog.add("cpu: ${Thread.currentThread().name}")
                    return data.uppercase() // CPU-bound transformation
                }

                val result = Mono.fromCallable { fetchFromDatabase() }
                    .subscribeOn(Schedulers.boundedElastic())
                    .publishOn(Schedulers.parallel())
                    .map { processData(it) }
                    .block()

                result shouldBe "RAW-DATA"
                threadLog[0] shouldContain "boundedElastic"
                threadLog[1] shouldContain "parallel"
            }
        }

        describe("Kotlin Coroutines Dispatchers") {

            it("Dispatchers.Default - for CPU-bound work") {
                val threadName = withContext(Dispatchers.Default) {
                    Thread.currentThread().name
                }

                threadName shouldContain "DefaultDispatcher"
            }

            it("Dispatchers.IO - for blocking I/O") {
                val threadName = withContext(Dispatchers.IO) {
                    Thread.sleep(10) // Blocking call OK here
                    Thread.currentThread().name
                }

                threadName shouldContain "DefaultDispatcher"
            }

            it("Dispatchers.Unconfined - no thread confinement") {
                val threads = mutableListOf<String>()

                withContext(Dispatchers.Unconfined) {
                    threads.add(Thread.currentThread().name)
                    delay(10)
                    threads.add(Thread.currentThread().name)
                }

                threads.size shouldBe 2
            }

            it("Custom dispatcher from executor") {
                val customExecutor = Executors.newSingleThreadExecutor { r ->
                    Thread(r, "my-custom-thread")
                }
                val customDispatcher = customExecutor.asCoroutineDispatcher()

                val threadName = withContext(customDispatcher) {
                    Thread.currentThread().name
                }

                threadName shouldContain "my-custom-thread"

                customDispatcher.close()
                customExecutor.shutdown()
            }
        }

        describe("Kotlin Flow - flowOn") {

            it("flowOn changes UPSTREAM context (opposite of publishOn!)") {
                val threadLog = mutableListOf<String>()

                flow {
                    threadLog.add("emit: ${Thread.currentThread().name}")
                    emit(1)
                }
                    .flowOn(Dispatchers.Default) // Affects emission (upstream)
                    .map {
                        threadLog.add("map: ${Thread.currentThread().name}")
                        it * 2
                    }
                    .collect()

                // Emission on Default, collection on test thread
                threadLog[0] shouldContain "DefaultDispatcher"
            }

            it("Multiple flowOn - each affects its upstream") {
                val threadLog = mutableListOf<String>()

                flow {
                    threadLog.add("emit: ${Thread.currentThread().name}")
                    emit(1)
                }
                    .map {
                        threadLog.add("map1: ${Thread.currentThread().name}")
                        it
                    }
                    .flowOn(Dispatchers.Default)
                    .map {
                        threadLog.add("map2: ${Thread.currentThread().name}")
                        it
                    }
                    .flowOn(Dispatchers.IO)
                    .collect()

                // emit and map1 on Default (innermost flowOn for that section)
                // map2 on IO
            }

            it("Collect always runs on collector's context") {
                val collectThread = mutableListOf<String>()

                flow { emit(1) }
                    .flowOn(Dispatchers.Default)
                    .collect {
                        collectThread.add(Thread.currentThread().name)
                    }

                // Collection runs on the caller's context
                collectThread.size shouldBe 1
            }
        }

        describe("withContext in suspend functions") {

            it("withContext switches context for a block") {
                val threads = mutableListOf<String>()

                threads.add("start: ${Thread.currentThread().name}")

                withContext(Dispatchers.Default) {
                    threads.add("default: ${Thread.currentThread().name}")
                }

                withContext(Dispatchers.IO) {
                    threads.add("io: ${Thread.currentThread().name}")
                }

                threads.add("end: ${Thread.currentThread().name}")

                threads.size shouldBe 4
            }

            it("Nested withContext") {
                val result = withContext(Dispatchers.IO) {
                    // Outer block on IO
                    val data = "fetched-data"

                    withContext(Dispatchers.Default) {
                        // Inner block on Default for CPU work
                        data.uppercase()
                    }
                }

                result shouldBe "FETCHED-DATA"
            }
        }

        describe("Comparison: Reactor vs Coroutines threading") {

            it("Reactor: subscribeOn + publishOn pattern") {
                val log = mutableListOf<String>()

                Mono.fromCallable {
                    log.add("source: ${Thread.currentThread().name}")
                    "data"
                }
                    .subscribeOn(Schedulers.boundedElastic())
                    .publishOn(Schedulers.parallel())
                    .map {
                        log.add("process: ${Thread.currentThread().name}")
                        it.uppercase()
                    }
                    .block()

                log[0] shouldContain "boundedElastic"
                log[1] shouldContain "parallel"
            }

            it("Coroutines: withContext pattern") {
                val log = mutableListOf<String>()

                val result = withContext(Dispatchers.IO) {
                    log.add("source: ${Thread.currentThread().name}")
                    val data = "data"

                    withContext(Dispatchers.Default) {
                        log.add("process: ${Thread.currentThread().name}")
                        data.uppercase()
                    }
                }

                result shouldBe "DATA"
            }

            it("Flow: flowOn pattern") {
                val log = mutableListOf<String>()

                flow {
                    log.add("emit: ${Thread.currentThread().name}")
                    emit("data")
                }
                    .flowOn(Dispatchers.IO)
                    .map {
                        log.add("map: ${Thread.currentThread().name}")
                        it.uppercase()
                    }
                    .flowOn(Dispatchers.Default)
                    .collect {
                        log.add("collect: ${Thread.currentThread().name}")
                    }

                log.size shouldBe 3
            }
        }

        describe("Why threading agnosticism matters") {

            it("Same business logic, different threading") {
                // The business logic is the same
                fun processData(data: String): String = data.uppercase()

                // But we can run it on different threads without changing it!

                // Option 1: Run on parallel scheduler
                val result1 = Mono.just("data")
                    .publishOn(Schedulers.parallel())
                    .map { processData(it) }
                    .block()

                // Option 2: Run on boundedElastic
                val result2 = Mono.just("data")
                    .publishOn(Schedulers.boundedElastic())
                    .map { processData(it) }
                    .block()

                // Same result, different threading!
                result1 shouldBe "DATA"
                result2 shouldBe "DATA"
            }

            it("Decoupling business logic from infrastructure") {
                // In tests: run synchronously
                // In production: run on appropriate scheduler

                fun businessLogic(input: String): Mono<String> = Mono.just(input.uppercase())

                // Test - can run on immediate/current thread
                val testResult = businessLogic("test")
                    .subscribeOn(Schedulers.immediate())
                    .block()

                // Production - would run on proper scheduler
                val prodResult = businessLogic("prod")
                    .subscribeOn(Schedulers.boundedElastic())
                    .block()

                testResult shouldBe "TEST"
                prodResult shouldBe "PROD"
            }
        }

        describe("Common patterns and best practices") {

            it("Wrap blocking calls with subscribeOn(boundedElastic)") {
                fun blockingDatabaseCall(): String {
                    Thread.sleep(10)
                    return "db-result"
                }

                // CORRECT: Wrap blocking call
                val result = Mono.fromCallable { blockingDatabaseCall() }
                    .subscribeOn(Schedulers.boundedElastic())
                    .block()

                result shouldBe "db-result"
            }

            it("Use parallel for CPU-intensive transformations") {
                val result = Flux.range(1, 1000)
                    .parallel()
                    .runOn(Schedulers.parallel())
                    .map { it * it } // CPU-bound
                    .sequential()
                    .collectList()
                    .block()

                result?.size shouldBe 1000
            }

            it("Don't block the event loop!") {
                // DON'T DO THIS in production:
                // Flux.just(1)
                //     .map { Thread.sleep(1000); it } // Blocks event loop!

                // DO THIS:
                val result = Mono.fromCallable {
                    Thread.sleep(10) // Blocking call
                    "result"
                }
                    .subscribeOn(Schedulers.boundedElastic()) // On proper scheduler
                    .block()

                result shouldBe "result"
            }
        }
    })
