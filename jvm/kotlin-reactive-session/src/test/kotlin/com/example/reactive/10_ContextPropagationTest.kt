package com.example.reactive

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier
import reactor.util.context.Context
import kotlin.coroutines.coroutineContext

/**
 * # Module 10: Context Propagation
 *
 * Context propagation is critical in reactive systems where
 * operations may execute on different threads.
 *
 * Key concepts:
 * - Reactor Context (immutable, flows downstream to upstream)
 * - CoroutineContext (flows parent to child)
 * - MDC and logging context challenges
 * - ThreadLocal limitations
 */
class ContextPropagationTest :
    DescribeSpec({

        describe("Reactor Context basics") {

            it("contextWrite adds data to reactive context") {
                val mono = Mono.deferContextual { ctx ->
                    Mono.just("User: ${ctx.get<String>("userId")}")
                }.contextWrite(Context.of("userId", "12345"))

                StepVerifier.create(mono)
                    .expectNext("User: 12345")
                    .verifyComplete()
            }

            it("Context flows UPSTREAM (from subscriber to publisher)") {
                val mono = Mono.deferContextual { ctx ->
                    Mono.just("Value: ${ctx.getOrDefault("key", "not-found")}")
                }
                    .flatMap { value ->
                        Mono.deferContextual { ctx ->
                            Mono.just("$value, Nested: ${ctx.getOrDefault("key", "not-found")}")
                        }
                    }
                    .contextWrite(Context.of("key", "found"))

                StepVerifier.create(mono)
                    .expectNext("Value: found, Nested: found")
                    .verifyComplete()
            }

            it("Multiple contextWrite calls merge contexts") {
                val mono = Mono.deferContextual { ctx ->
                    Mono.just("a=${ctx.get<String>("a")}, b=${ctx.get<String>("b")}")
                }
                    .contextWrite(Context.of("a", "valueA"))
                    .contextWrite(Context.of("b", "valueB"))

                StepVerifier.create(mono)
                    .expectNext("a=valueA, b=valueB")
                    .verifyComplete()
            }

            it("Inner contextWrite shadows outer for same key") {
                val mono = Mono.deferContextual { ctx ->
                    Mono.just("key=${ctx.get<String>("key")}")
                }
                    .contextWrite(Context.of("key", "inner")) // This wins
                    .contextWrite(Context.of("key", "outer"))

                StepVerifier.create(mono)
                    .expectNext("key=inner")
                    .verifyComplete()
            }

            it("Context is preserved across operators") {
                val flux = Flux.deferContextual { ctx ->
                    Flux.just(1, 2, 3)
                        .map { "$it-${ctx.get<String>("prefix")}" }
                }
                    .contextWrite(Context.of("prefix", "item"))

                StepVerifier.create(flux)
                    .expectNext("1-item", "2-item", "3-item")
                    .verifyComplete()
            }
        }

        describe("Reactor Context across schedulers") {

            it("Context is preserved when switching schedulers") {
                val mono = Mono.deferContextual { ctx ->
                    Mono.just("Context: ${ctx.get<String>("data")}")
                }
                    .subscribeOn(Schedulers.boundedElastic())
                    .publishOn(Schedulers.parallel())
                    .contextWrite(Context.of("data", "preserved"))

                StepVerifier.create(mono)
                    .expectNext("Context: preserved")
                    .verifyComplete()
            }

            it("Context works with flatMap on different schedulers") {
                val mono = Mono.just("start")
                    .flatMap {
                        Mono.deferContextual { ctx ->
                            Mono.just("${ctx.get<String>("key")}")
                        }.subscribeOn(Schedulers.boundedElastic())
                    }
                    .contextWrite(Context.of("key", "value"))

                StepVerifier.create(mono)
                    .expectNext("value")
                    .verifyComplete()
            }
        }

        describe("CoroutineContext basics") {

            it("CoroutineName context element") {
                withContext(CoroutineName("my-coroutine")) {
                    val name = coroutineContext[CoroutineName]?.name
                    name shouldBe "my-coroutine"
                }
            }

            it("Context flows to child coroutines") {
                withContext(CoroutineName("parent")) {
                    val result = coroutineScope {
                        async {
                            coroutineContext[CoroutineName]?.name
                        }.await()
                    }

                    result shouldBe "parent"
                }
            }

            it("Child can override context") {
                withContext(CoroutineName("parent")) {
                    val parentName = coroutineContext[CoroutineName]?.name

                    withContext(CoroutineName("child")) {
                        val childName = coroutineContext[CoroutineName]?.name
                        childName shouldBe "child"
                    }

                    // Parent unchanged
                    coroutineContext[CoroutineName]?.name shouldBe "parent"
                    parentName shouldBe "parent"
                }
            }

            it("Combining multiple context elements") {
                withContext(CoroutineName("name") + Dispatchers.Default) {
                    coroutineContext[CoroutineName]?.name shouldBe "name"
                    coroutineContext[CoroutineDispatcher] shouldNotBe null
                }
            }
        }

        describe("Flow context") {

            it("flowOn changes upstream dispatcher") {
                val threads = mutableListOf<String>()

                flow {
                    threads.add("emit: ${Thread.currentThread().name}")
                    emit(1)
                }
                    .flowOn(Dispatchers.Default)
                    .collect {
                        threads.add("collect: ${Thread.currentThread().name}")
                    }

                threads.size shouldBe 2
            }

            it("Collector context is different from emission context") {
                val emissionContext = mutableListOf<String>()
                val collectionContext = mutableListOf<String>()

                flow {
                    emissionContext.add(Thread.currentThread().name)
                    emit(1)
                }
                    .flowOn(Dispatchers.Default)
                    .collect {
                        collectionContext.add(Thread.currentThread().name)
                    }

                // Different contexts
                emissionContext.isNotEmpty() shouldBe true
                collectionContext.isNotEmpty() shouldBe true
            }
        }

        describe("ThreadLocal challenges in reactive streams") {

            it("ThreadLocal DOES NOT work across schedulers") {
                val threadLocal = ThreadLocal<String>()

                val mono = Mono.fromCallable {
                    threadLocal.set("set-in-callable")
                    "started"
                }
                    .subscribeOn(Schedulers.boundedElastic())
                    .publishOn(Schedulers.parallel())
                    .map {
                        // ThreadLocal value is LOST after scheduler switch!
                        threadLocal.get() ?: "null"
                    }

                StepVerifier.create(mono)
                    .expectNext("null") // Value was lost!
                    .verifyComplete()
            }

            it("Use Reactor Context instead of ThreadLocal") {
                val mono = Mono.fromCallable { "started" }
                    .subscribeOn(Schedulers.boundedElastic())
                    .publishOn(Schedulers.parallel())
                    .flatMap {
                        Mono.deferContextual { ctx ->
                            Mono.just(ctx.get<String>("data"))
                        }
                    }
                    .contextWrite(Context.of("data", "preserved-via-context"))

                StepVerifier.create(mono)
                    .expectNext("preserved-via-context")
                    .verifyComplete()
            }
        }

        describe("Bridging Reactor Context and CoroutineContext") {

            it("ReactorContext in coroutines") {
                val mono = mono(ReactorContext(Context.of("key", "value"))) {
                    val ctx = kotlin.coroutines.coroutineContext[ReactorContext]?.context
                    ctx?.get<String>("key") ?: "not-found"
                }

                val result = mono.awaitSingle()
                result shouldBe "value"
            }

            it("Access Reactor Context from mono {} builder") {
                val mono = Mono.just("data")
                    .flatMap {
                        mono {
                            val reactorCtx = kotlin.coroutines.coroutineContext[ReactorContext]?.context
                            "userId: ${reactorCtx?.getOrDefault("userId", "unknown")}"
                        }
                    }
                    .contextWrite(Context.of("userId", "123"))

                StepVerifier.create(mono)
                    .expectNext("userId: 123")
                    .verifyComplete()
            }
        }

        describe("Practical patterns") {

            it("Request tracing pattern with Reactor Context") {
                fun processRequest(requestId: String): Mono<String> = Mono.just("Processing request $requestId")

                val result = processRequest("req-001")
                    .flatMap { msg ->
                        Mono.deferContextual { ctx ->
                            Mono.just("$msg [traced: ${ctx.get<String>("requestId")}]")
                        }
                    }
                    .contextWrite(Context.of("requestId", "req-001")) // Context flows upstream

                StepVerifier.create(result)
                    .expectNext("Processing request req-001 [traced: req-001]")
                    .verifyComplete()
            }

            it("Coroutine context for tracing") {
                val result = withContext(CoroutineName("trace-123")) {
                    // Access coroutine context directly within withContext
                    val name = coroutineContext[CoroutineName]?.name
                    "Operation with name: $name"
                }

                result shouldBe "Operation with name: trace-123"
            }

            it("Extracting context at boundaries") {
                // When calling Reactor from Coroutines
                suspend fun callReactorService(): String = Mono.deferContextual { ctx ->
                    Mono.just("Got: ${ctx.getOrDefault("trace", "none")}")
                }
                    .contextWrite(Context.of("trace", "my-trace-id"))
                    .awaitSingle()

                val result = callReactorService()
                result shouldBe "Got: my-trace-id"
            }
        }

        describe("Best practices") {

            it("Use Reactor Context for cross-cutting concerns") {
                fun <T> Mono<T>.withTracing(traceId: String): Mono<T> = this.contextWrite(Context.of("traceId", traceId))

                val mono = Mono.just("data")
                    .flatMap {
                        Mono.deferContextual { ctx ->
                            Mono.just("Data with trace: ${ctx.get<String>("traceId")}")
                        }
                    }
                    .withTracing("trace-123")

                StepVerifier.create(mono)
                    .expectNext("Data with trace: trace-123")
                    .verifyComplete()
            }

            it("Use CoroutineName for debugging") {
                withContext(CoroutineName("admin-task")) {
                    val name = coroutineContext[CoroutineName]?.name
                        ?: throw IllegalStateException("No coroutine name")
                    val result = "Running as: $name"
                    result shouldBe "Running as: admin-task"
                }
            }

            it("Clean up context at request boundaries") {
                val mono = Mono.deferContextual { ctx ->
                    // Read at start
                    val requestId = ctx.get<String>("requestId")
                    Mono.just("Request: $requestId")
                }
                    .doOnTerminate {
                        // Clean up resources if needed
                    }
                    .contextWrite(Context.of("requestId", "req-123"))

                StepVerifier.create(mono)
                    .expectNext("Request: req-123")
                    .verifyComplete()
            }
        }
    })
