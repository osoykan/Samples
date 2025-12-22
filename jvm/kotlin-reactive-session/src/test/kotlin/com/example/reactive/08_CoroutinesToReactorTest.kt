package com.example.reactive

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.flux
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

/**
 * # Module 8: Coroutines to Reactor Bridge
 *
 * kotlinx-coroutines-reactor provides builders and extensions
 * to convert coroutines (suspend/Flow) to Reactor types (Mono/Flux).
 *
 * Key conversions:
 * - suspend -> Mono (mono { })
 * - Flow -> Flux (asFlux(), flux { })
 */
class CoroutinesToReactorTest :
    DescribeSpec({

        describe("mono { } builder - suspend to Mono") {

            it("mono { } wraps suspend function result in Mono") {
                suspend fun fetchData(): String {
                    delay(10)
                    return "data"
                }

                val mono: Mono<String> = mono { fetchData() }

                StepVerifier.create(mono)
                    .expectNext("data")
                    .verifyComplete()
            }

            it("mono { } with simple value") {
                val mono = mono {
                    delay(10)
                    42
                }

                StepVerifier.create(mono)
                    .expectNext(42)
                    .verifyComplete()
            }

            it("mono { } with null returns empty Mono") {
                val mono = mono {
                    delay(10)
                    null
                }

                StepVerifier.create(mono)
                    .verifyComplete() // No value, just completion
            }

            it("Exception in mono { } becomes Mono error") {
                val mono = mono<String> {
                    delay(10)
                    throw RuntimeException("Coroutine error")
                }

                StepVerifier.create(mono)
                    .expectErrorMessage("Coroutine error")
                    .verify()
            }

            it("mono { } with custom coroutine context") {
                val mono = mono(kotlinx.coroutines.Dispatchers.Default) {
                    "executed on Default dispatcher"
                }

                StepVerifier.create(mono)
                    .expectNext("executed on Default dispatcher")
                    .verifyComplete()
            }

            it("Calling other suspend functions inside mono { }") {
                suspend fun step1(): Int {
                    delay(10)
                    return 10
                }

                suspend fun step2(value: Int): Int {
                    delay(10)
                    return value * 2
                }

                val mono = mono {
                    val a = step1()
                    val b = step2(a)
                    a + b // 10 + 20 = 30
                }

                StepVerifier.create(mono)
                    .expectNext(30)
                    .verifyComplete()
            }
        }

        describe("flux { } builder - creating Flux from coroutine") {

            it("flux { } allows sending multiple values") {
                val flux: Flux<Int> = flux {
                    send(1)
                    delay(10)
                    send(2)
                    delay(10)
                    send(3)
                }

                StepVerifier.create(flux)
                    .expectNext(1, 2, 3)
                    .verifyComplete()
            }

            it("flux { } with loop") {
                val flux = flux {
                    for (i in 1..5) {
                        send(i)
                        delay(5)
                    }
                }

                StepVerifier.create(flux)
                    .expectNext(1, 2, 3, 4, 5)
                    .verifyComplete()
            }

            it("Exception in flux { } becomes error signal") {
                val flux = flux {
                    send(1)
                    send(2)
                    throw RuntimeException("Error at 3")
                }

                StepVerifier.create(flux)
                    .expectNext(1, 2)
                    .expectErrorMessage("Error at 3")
                    .verify()
            }

            it("flux { } with suspend function calls") {
                suspend fun fetchItem(id: Int): String {
                    delay(5)
                    return "Item-$id"
                }

                val flux = flux {
                    for (id in 1..3) {
                        val item = fetchItem(id)
                        send(item)
                    }
                }

                StepVerifier.create(flux)
                    .expectNext("Item-1", "Item-2", "Item-3")
                    .verifyComplete()
            }
        }

        describe("Flow to Flux conversion") {

            it("asFlux() converts Flow to Flux") {
                val flow: Flow<Int> = flowOf(1, 2, 3)
                val flux: Flux<Int> = flow.asFlux()

                StepVerifier.create(flux)
                    .expectNext(1, 2, 3)
                    .verifyComplete()
            }

            it("Empty Flow becomes empty Flux") {
                val flow = flow<String> { }
                val flux = flow.asFlux()

                StepVerifier.create(flux)
                    .verifyComplete()
            }

            it("Flow with delays converts correctly") {
                val flow = flow {
                    emit(1)
                    delay(10)
                    emit(2)
                    delay(10)
                    emit(3)
                }

                StepVerifier.create(flow.asFlux())
                    .expectNext(1, 2, 3)
                    .verifyComplete()
            }

            it("Error in Flow propagates to Flux") {
                val flow = flow {
                    emit(1)
                    emit(2)
                    throw RuntimeException("Flow error")
                }

                StepVerifier.create(flow.asFlux())
                    .expectNext(1, 2)
                    .expectErrorMessage("Flow error")
                    .verify()
            }

            it("Flux operators work on converted Flow") {
                val flow = flowOf(1, 2, 3, 4, 5)

                val flux = flow.asFlux()
                    .filter { it % 2 == 0 }
                    .map { it * 10 }

                StepVerifier.create(flux)
                    .expectNext(20, 40)
                    .verifyComplete()
            }
        }

        describe("Practical usage patterns") {

            it("Exposing coroutine-based service as Reactor API") {
                // Coroutine-based implementation
                class CoroutineUserService {
                    suspend fun findById(id: String): String {
                        delay(10)
                        return "User-$id"
                    }

                    fun findAll(): Flow<String> = flow {
                        for (i in 1..3) {
                            delay(5)
                            emit("User-$i")
                        }
                    }
                }

                // Reactor API wrapper
                class ReactorUserService(private val coroutineService: CoroutineUserService) {
                    fun findById(id: String): Mono<String> = mono {
                        coroutineService.findById(id)
                    }

                    fun findAll(): Flux<String> = coroutineService.findAll().asFlux()
                }

                val service = ReactorUserService(CoroutineUserService())

                StepVerifier.create(service.findById("123"))
                    .expectNext("User-123")
                    .verifyComplete()

                StepVerifier.create(service.findAll())
                    .expectNext("User-1", "User-2", "User-3")
                    .verifyComplete()
            }

            it("Combining coroutines inside mono { }") {
                suspend fun fetchUser(id: Int): String {
                    delay(10)
                    return "User-$id"
                }

                suspend fun fetchPermissions(userId: String): List<String> {
                    delay(10)
                    return listOf("read", "write")
                }

                data class UserWithPermissions(val name: String, val permissions: List<String>)

                val mono = mono {
                    val user = fetchUser(1)
                    val permissions = fetchPermissions(user)
                    UserWithPermissions(user, permissions)
                }

                StepVerifier.create(mono)
                    .assertNext { result ->
                        result.name shouldBe "User-1"
                        result.permissions shouldBe listOf("read", "write")
                    }
                    .verifyComplete()
            }

            it("Error handling in mono { }") {
                suspend fun riskyOperation(): String {
                    delay(10)
                    throw IllegalArgumentException("Invalid input")
                }

                val mono = mono {
                    try {
                        riskyOperation()
                    } catch (e: IllegalArgumentException) {
                        "fallback"
                    }
                }

                StepVerifier.create(mono)
                    .expectNext("fallback")
                    .verifyComplete()
            }

            it("Parallel operations in mono { } using async") {
                suspend fun fetchA(): String {
                    delay(50)
                    return "A"
                }

                suspend fun fetchB(): String {
                    delay(50)
                    return "B"
                }

                val mono = mono {
                    coroutineScope {
                        val a = async { fetchA() }
                        val b = async { fetchB() }
                        "${a.await()}${b.await()}"
                    }
                }

                StepVerifier.create(mono)
                    .expectNext("AB")
                    .verifyComplete()
            }
        }

        describe("Context propagation") {

            it("mono { } preserves coroutine context") {
                val mono = mono(kotlinx.coroutines.Dispatchers.Default) {
                    val threadName = Thread.currentThread().name
                    "Ran on: $threadName"
                }

                StepVerifier.create(mono)
                    .assertNext { it.contains("DefaultDispatcher") shouldBe true }
                    .verifyComplete()
            }

            it("Flow context flows through asFlux()") {
                val myFlow = flow {
                    emit(Thread.currentThread().name)
                }.flowOn(Dispatchers.Default)

                StepVerifier.create(myFlow.asFlux())
                    .assertNext { it.contains("DefaultDispatcher") shouldBe true }
                    .verifyComplete()
            }
        }

        describe("Backpressure handling") {

            it("Flux from Flow respects subscriber demand") {
                var emitted = 0

                val flow = flow {
                    repeat(100) {
                        emitted++
                        emit(it)
                    }
                }

                // Take only 5 elements
                StepVerifier.create(flow.asFlux().take(5))
                    .expectNextCount(5)
                    .verifyComplete()
            }
        }

        describe("Best practices") {

            it("Use mono { } for single async operations") {
                // Helper functions for this test
                suspend fun fetchData(): String {
                    delay(10)
                    return "data"
                }

                fun process(data: String) = data.uppercase()

                // Good: Single value computation
                val goodMono = mono {
                    val data = fetchData()
                    process(data)
                }

                StepVerifier.create(goodMono)
                    .expectNext("DATA")
                    .verifyComplete()
            }

            it("Use flux { } or asFlux() for multiple values") {
                // Good: Multiple values
                val goodFlux = flux {
                    for (i in 1..3) {
                        send(i)
                    }
                }

                StepVerifier.create(goodFlux)
                    .expectNext(1, 2, 3)
                    .verifyComplete()
            }

            it("Don't block inside mono { } - use suspend functions") {
                // Bad: Thread.sleep() inside mono - blocks thread
                // val badMono = mono { Thread.sleep(100); "data" }

                // Good: delay() suspends without blocking
                val goodMono = mono {
                    delay(10)
                    "data"
                }

                StepVerifier.create(goodMono)
                    .expectNext("data")
                    .verifyComplete()
            }
        }
    })
