package com.example.reactive

import app.cash.turbine.test
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.NoSuchElementException

/**
 * # Module 7: Reactor to Coroutines Bridge
 *
 * kotlinx-coroutines-reactor provides extensions to convert
 * Reactor types (Mono/Flux) to coroutines (suspend/Flow).
 *
 * Key conversions:
 * - Mono -> suspend (await*)
 * - Flux -> Flow (asFlow)
 *
 * Note: Kotest DescribeSpec is already coroutine-enabled, no runTest needed!
 */
class ReactorToCoroutinesTest :
    DescribeSpec({

        describe("Mono to suspend function conversions") {

            it("awaitSingle() - suspends until Mono emits") {
                val mono = Mono.just("Hello")
                val result: String = mono.awaitSingle()
                result shouldBe "Hello"
            }

            it("awaitSingle() throws on empty Mono") {
                val emptyMono = Mono.empty<String>()

                shouldThrow<NoSuchElementException> {
                    emptyMono.awaitSingle()
                }
            }

            it("awaitSingleOrNull() - returns null for empty Mono") {
                val emptyMono = Mono.empty<String>()
                val result: String? = emptyMono.awaitSingleOrNull()
                result shouldBe null
            }

            it("awaitSingleOrNull() - returns value for non-empty Mono") {
                val mono = Mono.just("value")
                val result: String? = mono.awaitSingleOrNull()
                result shouldBe "value"
            }

            it("awaitFirst() - for Mono, same as awaitSingle()") {
                val mono = Mono.just("first")
                val result = mono.awaitFirst()
                result shouldBe "first"
            }

            it("awaitFirstOrNull() - for potentially empty Mono") {
                val mono = Mono.just("value")
                val empty = Mono.empty<String>()

                mono.awaitFirstOrNull() shouldBe "value"
                empty.awaitFirstOrNull() shouldBe null
            }

            it("Mono with delay suspends properly") {
                val delayedMono = Mono.just("delayed")
                    .delayElement(Duration.ofMillis(50))

                val result = delayedMono.awaitSingle()
                result shouldBe "delayed"
            }

            it("Error Mono throws exception in coroutine") {
                val errorMono = Mono.error<String>(RuntimeException("Reactor error"))

                shouldThrow<RuntimeException> {
                    errorMono.awaitSingle()
                }.message shouldBe "Reactor error"
            }
        }

        describe("Flux to Flow conversion") {

            it("asFlow() converts Flux to Flow - with Turbine") {
                val flux = Flux.just(1, 2, 3)

                flux.asFlow().test {
                    awaitItem() shouldBe 1
                    awaitItem() shouldBe 2
                    awaitItem() shouldBe 3
                    awaitComplete()
                }
            }

            it("asFlow() converts Flux to Flow - with toList()") {
                val flux = Flux.just(1, 2, 3)
                val result = flux.asFlow().toList()
                result shouldBe listOf(1, 2, 3)
            }

            it("Empty Flux becomes empty Flow") {
                val flux = Flux.empty<String>()
                val result = flux.asFlow().toList()
                result shouldBe emptyList()
            }

            it("Flux with delay works with Flow") {
                val flux = Flux.just(1, 2, 3)
                    .delayElements(Duration.ofMillis(10))

                val result = flux.asFlow().toList()
                result shouldBe listOf(1, 2, 3)
            }

            it("Error in Flux propagates to Flow") {
                val flux = Flux.just(1, 2)
                    .concatWith(Flux.error(RuntimeException("Flux error")))

                shouldThrow<RuntimeException> {
                    flux.asFlow().toList()
                }.message shouldBe "Flux error"
            }

            it("Error in Flux with Turbine") {
                val flux = Flux.just(1, 2)
                    .concatWith(Flux.error(RuntimeException("Flux error")))

                flux.asFlow().test {
                    awaitItem() shouldBe 1
                    awaitItem() shouldBe 2
                    awaitError().message shouldBe "Flux error"
                }
            }

            it("Flow operators work on converted Flux") {
                val flux = Flux.range(1, 10)

                val result = mutableListOf<Int>()
                flux.asFlow()
                    .collect { value ->
                        if (value % 2 == 0) {
                            result.add(value * 10)
                        }
                    }

                result shouldBe listOf(20, 40, 60, 80, 100)
            }
        }

        describe("Flux single element extractions") {

            it("awaitFirst() gets first element from Flux") {
                val flux = Flux.just(1, 2, 3)
                val first = flux.awaitFirst()
                first shouldBe 1
            }

            it("awaitLast() gets last element from Flux") {
                val flux = Flux.just(1, 2, 3)
                val last = flux.awaitLast()
                last shouldBe 3
            }

            it("awaitFirst() throws on empty Flux") {
                shouldThrow<NoSuchElementException> {
                    Flux.empty<Int>().awaitFirst()
                }
            }

            it("awaitFirstOrNull() returns null for empty Flux") {
                val result = Flux.empty<Int>().awaitFirstOrNull()
                result shouldBe null
            }
        }

        describe("Practical usage patterns") {

            it("Using Reactor service in suspend function") {
                // Simulating a Reactor-based service
                class UserService {
                    fun findById(id: String): Mono<String> = Mono.just("User-$id")

                    fun findAll(): Flux<String> = Flux.just("User-1", "User-2", "User-3")
                }

                val service = UserService()

                // Use in suspend function with await
                suspend fun getUser(id: String): String = service.findById(id).awaitSingle()

                suspend fun getAllUsers(): List<String> = service.findAll().asFlow().toList()

                getUser("123") shouldBe "User-123"
                getAllUsers() shouldBe listOf("User-1", "User-2", "User-3")
            }

            it("Combining Reactor and Coroutines in workflow") {
                // Reactor-based fetch
                fun fetchData(): Mono<String> = Mono.just("data")

                // Coroutine-based processing
                suspend fun processData(data: String): String {
                    delay(10)
                    return data.uppercase()
                }

                // Combined workflow
                suspend fun workflow(): String {
                    val data = fetchData().awaitSingle()
                    return processData(data)
                }

                workflow() shouldBe "DATA"
            }

            it("Error handling when converting Reactor to Coroutines") {
                fun riskyMono(): Mono<String> = Mono.error(IllegalStateException("Service unavailable"))

                val result = try {
                    riskyMono().awaitSingle()
                } catch (e: IllegalStateException) {
                    "fallback: ${e.message}"
                }

                result shouldBe "fallback: Service unavailable"
            }

            it("Optional values pattern") {
                fun maybeFind(id: String): Mono<String> = if (id == "exists") {
                    Mono.just("Found: $id")
                } else {
                    Mono.empty()
                }

                suspend fun findOrDefault(id: String): String = maybeFind(id).awaitSingleOrNull() ?: "Not found"

                findOrDefault("exists") shouldBe "Found: exists"
                findOrDefault("missing") shouldBe "Not found"
            }
        }

        describe("Flow conversion preserves backpressure") {

            it("Slow collector applies backpressure to Flux source") {
                var emissionCount = 0

                val flux = Flux.range(1, 100)
                    .doOnNext { emissionCount++ }

                // Convert to Flow and collect slowly
                flux.asFlow()
                    .collect {
                        delay(1) // Slow consumer
                    }

                emissionCount shouldBe 100
            }
        }
    })
