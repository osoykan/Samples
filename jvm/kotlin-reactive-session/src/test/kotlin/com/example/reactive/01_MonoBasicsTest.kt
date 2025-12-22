package com.example.reactive

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * # Module 1: Mono Basics
 *
 * Mono represents a reactive sequence of 0 or 1 element.
 * Think of it as a reactive equivalent of Optional<T> or CompletableFuture<T>.
 *
 * Key concepts covered:
 * - Creating Mono instances
 * - Transformations (map, flatMap)
 * - Fallback operators
 * - Side effects
 * - Testing with StepVerifier
 */
class MonoBasicsTest :
    DescribeSpec({

        describe("Creating Mono instances") {

            it("Mono.just() - wraps an existing value") {
                // Mono.just() creates a Mono that emits a single value
                // The value is captured immediately (eager evaluation)
                val mono = Mono.just("Hello, Reactor!")

                StepVerifier.create(mono)
                    .expectNext("Hello, Reactor!")
                    .verifyComplete()
            }

            it("Mono.empty() - represents absence of value") {
                // Mono.empty() creates a Mono that completes without emitting any value
                // Similar to Optional.empty() or null in nullable context
                val mono = Mono.empty<String>()

                StepVerifier.create(mono)
                    .verifyComplete() // Completes immediately, no onNext signal
            }

            it("Mono.error() - represents a failed computation") {
                // Mono.error() creates a Mono that immediately signals an error
                val mono = Mono.error<String>(RuntimeException("Something went wrong"))

                StepVerifier.create(mono)
                    .expectError(RuntimeException::class.java)
                    .verify()
            }

            it("Mono.defer() - lazy evaluation, creates new Mono on each subscription") {
                // IMPORTANT: Mono.just() captures the value immediately
                // Mono.defer() creates a new Mono on each subscription

                val counter = AtomicInteger(0)

                // With Mono.just() - value is computed once
                val eagerMono = Mono.just(counter.incrementAndGet())
                eagerMono.block()
                eagerMono.block()
                val eagerCount = counter.get() // Still 1!

                // With Mono.defer() - factory is called on each subscription
                val lazyMono = Mono.defer { Mono.just(counter.incrementAndGet()) }
                lazyMono.block()
                lazyMono.block()
                val lazyCount = counter.get() // Now 3 (1 + 2 more subscriptions)

                eagerCount shouldBe 1
                lazyCount shouldBe 3
            }

            it("Mono.fromCallable() - wraps a potentially blocking operation") {
                // fromCallable wraps a Callable/lambda that may throw
                // The lambda is executed on subscription
                val mono = Mono.fromCallable {
                    // Simulating some computation
                    Thread.sleep(10)
                    "Computed value"
                }

                StepVerifier.create(mono)
                    .expectNext("Computed value")
                    .verifyComplete()
            }

            it("Mono.fromSupplier() - similar to fromCallable but for Supplier") {
                val mono = Mono.fromSupplier { "Supplied value" }

                StepVerifier.create(mono)
                    .expectNext("Supplied value")
                    .verifyComplete()
            }
        }

        describe("Transformations") {

            it("map() - synchronous transformation") {
                // map() applies a synchronous function to the value
                // Use when your transformation is simple and doesn't involve async operations
                val mono = Mono.just(5)
                    .map { it * 2 }
                    .map { "Result: $it" }

                StepVerifier.create(mono)
                    .expectNext("Result: 10")
                    .verifyComplete()
            }

            it("flatMap() - asynchronous transformation returning another Mono") {
                // flatMap() is used when your transformation returns another Mono
                // Essential for chaining async operations
                fun fetchUser(id: Int): Mono<String> = Mono.just("User-$id")
                fun fetchUserDetails(user: String): Mono<String> = Mono.just("Details of $user")

                val mono = Mono.just(1)
                    .flatMap { userId -> fetchUser(userId) }
                    .flatMap { user -> fetchUserDetails(user) }

                StepVerifier.create(mono)
                    .expectNext("Details of User-1")
                    .verifyComplete()
            }

            it("filter() - conditionally emit or complete empty") {
                // filter() emits the value only if predicate returns true
                // Otherwise, it completes empty (no value)
                val passingFilter = Mono.just(10)
                    .filter { it > 5 }

                val failingFilter = Mono.just(3)
                    .filter { it > 5 }

                StepVerifier.create(passingFilter)
                    .expectNext(10)
                    .verifyComplete()

                StepVerifier.create(failingFilter)
                    .verifyComplete() // Empty! No value emitted
            }

            it("cast() - type casting with runtime check") {
                val mono: Mono<Any> = Mono.just("Hello")
                val castedMono: Mono<String> = mono.cast(String::class.java)

                StepVerifier.create(castedMono)
                    .expectNext("Hello")
                    .verifyComplete()
            }

            it("transform() - applying a reusable transformation function") {
                // transform() lets you extract common transformation logic
                fun <T> addLogging(): (Mono<T>) -> Mono<T> = { mono ->
                    mono.doOnSubscribe { println("Subscribed!") }
                        .doOnNext { println("Value: $it") }
                        .doOnSuccess { println("Completed!") }
                }

                val mono = Mono.just("test")
                    .transform(addLogging())

                StepVerifier.create(mono)
                    .expectNext("test")
                    .verifyComplete()
            }
        }

        describe("Fallback operators") {

            it("defaultIfEmpty() - provide default value for empty Mono") {
                val mono = Mono.empty<String>()
                    .defaultIfEmpty("Default Value")

                StepVerifier.create(mono)
                    .expectNext("Default Value")
                    .verifyComplete()
            }

            it("switchIfEmpty() - switch to another Mono if empty") {
                // More powerful than defaultIfEmpty - can switch to a computed Mono
                val fallbackMono = Mono.fromCallable {
                    "Fallback computed value"
                }

                val mono = Mono.empty<String>()
                    .switchIfEmpty(fallbackMono)

                StepVerifier.create(mono)
                    .expectNext("Fallback computed value")
                    .verifyComplete()
            }

            it("switchIfEmpty() with defer for lazy fallback evaluation") {
                // Important: without defer, the fallback is computed even if not needed
                val counter = AtomicInteger(0)

                val nonEmptyMono = Mono.just("value")
                    .switchIfEmpty(
                        Mono.defer {
                            counter.incrementAndGet()
                            Mono.just("fallback")
                        },
                    )

                StepVerifier.create(nonEmptyMono)
                    .expectNext("value")
                    .verifyComplete()

                counter.get() shouldBe 0 // Fallback was never computed
            }
        }

        describe("Side effects - doOn* operators") {

            it("doOnNext() - side effect when value is emitted") {
                val sideEffect = mutableListOf<String>()

                val mono = Mono.just("value")
                    .doOnNext { sideEffect.add("Received: $it") }

                // Side effect hasn't happened yet - nothing happens until subscribe!
                sideEffect.size shouldBe 0

                mono.block()

                sideEffect shouldBe listOf("Received: value")
            }

            it("doOnSuccess() - side effect on successful completion (with or without value)") {
                val successLog = mutableListOf<String>()

                val monoWithValue = Mono.just("data")
                    .doOnSuccess { successLog.add("Success with: $it") }

                val emptyMono = Mono.empty<String>()
                    .doOnSuccess { successLog.add("Success with: $it") }

                monoWithValue.block()
                emptyMono.block()

                successLog shouldBe listOf("Success with: data", "Success with: null")
            }

            it("doOnError() - side effect on error") {
                val errorLog = mutableListOf<String>()

                val mono = Mono.error<String>(RuntimeException("Oops!"))
                    .doOnError { errorLog.add("Error: ${it.message}") }

                // Need to handle the error or verify it
                StepVerifier.create(mono)
                    .expectError()
                    .verify()

                errorLog shouldBe listOf("Error: Oops!")
            }

            it("doOnSubscribe() - side effect when subscription happens") {
                val subscriptionLog = mutableListOf<String>()

                val mono = Mono.just("value")
                    .doOnSubscribe { subscriptionLog.add("Someone subscribed!") }

                subscriptionLog.size shouldBe 0 // Not subscribed yet

                mono.block()
                mono.block() // Subscribe twice

                subscriptionLog.size shouldBe 2 // Called on each subscription
            }

            it("doOnTerminate() - side effect on any termination (success or error)") {
                val terminationLog = mutableListOf<String>()

                val successMono = Mono.just("value")
                    .doOnTerminate { terminationLog.add("Terminated (success)") }

                val errorMono = Mono.error<String>(RuntimeException())
                    .doOnTerminate { terminationLog.add("Terminated (error)") }

                successMono.block()
                runCatching { errorMono.block() }

                terminationLog shouldBe listOf("Terminated (success)", "Terminated (error)")
            }

            it("doFinally() - always executed, provides termination type") {
                val finallyLog = mutableListOf<String>()

                val mono = Mono.just("value")
                    .doFinally { signalType -> finallyLog.add("Finally: $signalType") }

                mono.block()

                finallyLog shouldBe listOf("Finally: onComplete")
            }
        }

        describe("Timing operators") {

            it("delayElement() - delays the emission") {
                val mono = Mono.just("delayed")
                    .delayElement(Duration.ofMillis(100))

                StepVerifier.create(mono)
                    .expectNext("delayed")
                    .verifyComplete()
            }

            it("timeout() - fails if value not emitted within duration") {
                val slowMono = Mono.just("slow")
                    .delayElement(Duration.ofSeconds(10))
                    .timeout(Duration.ofMillis(100))

                StepVerifier.create(slowMono)
                    .expectError(java.util.concurrent.TimeoutException::class.java)
                    .verify()
            }

            it("timeout() with fallback") {
                val slowMono = Mono.just("slow")
                    .delayElement(Duration.ofSeconds(10))
                    .timeout(Duration.ofMillis(100), Mono.just("fallback"))

                StepVerifier.create(slowMono)
                    .expectNext("fallback")
                    .verifyComplete()
            }
        }

        describe("Mono is lazy - nothing happens until subscription") {

            it("demonstrates that Mono is cold/lazy") {
                val executionLog = mutableListOf<String>()

                // This creates a Mono but doesn't execute anything
                val mono = Mono.fromCallable {
                    executionLog.add("Computing...")
                    "result"
                }

                // Still nothing happened
                executionLog.size shouldBe 0

                // Now we subscribe - this triggers execution
                val result = mono.block()

                result shouldBe "result"
                executionLog shouldBe listOf("Computing...")

                // Subscribe again - executes again (cold stream)
                mono.block()
                executionLog shouldBe listOf("Computing...", "Computing...")
            }

            it("cache() - converts cold Mono to hot (cached)") {
                val executionLog = mutableListOf<String>()

                val cachedMono = Mono.fromCallable {
                    executionLog.add("Computing...")
                    "result"
                }.cache()

                cachedMono.block()
                cachedMono.block()
                cachedMono.block()

                // Only computed once!
                executionLog shouldBe listOf("Computing...")
            }
        }

        describe("Zipping and combining Monos") {

            it("zipWith() - combines two Monos") {
                val mono1 = Mono.just("Hello")
                val mono2 = Mono.just("World")

                val combined = mono1.zipWith(mono2) { a, b -> "$a $b" }

                StepVerifier.create(combined)
                    .expectNext("Hello World")
                    .verifyComplete()
            }

            it("Mono.zip() - combines multiple Monos") {
                val mono1 = Mono.just("A")
                val mono2 = Mono.just("B")
                val mono3 = Mono.just("C")

                val combined = Mono.zip(mono1, mono2, mono3)
                    .map { tuple -> "${tuple.t1}-${tuple.t2}-${tuple.t3}" }

                StepVerifier.create(combined)
                    .expectNext("A-B-C")
                    .verifyComplete()
            }

            it("zipWhen() - sequential dependent Monos") {
                fun fetchUser(id: Int) = Mono.just("User-$id")
                fun fetchPermissions(user: String) = Mono.just(listOf("read", "write"))

                // zipWhen lets you access the result of first Mono
                // while combining with second
                val result = fetchUser(1)
                    .zipWhen { user -> fetchPermissions(user) }
                    .map { tuple -> "${tuple.t1} has ${tuple.t2}" }

                StepVerifier.create(result)
                    .expectNext("User-1 has [read, write]")
                    .verifyComplete()
            }
        }

        describe("Block operations - USE WITH CAUTION") {

            it("block() - blocks until value is available") {
                // WARNING: Only use in tests or at application boundaries
                // NEVER use inside reactive chains!
                val result = Mono.just("value").block()
                result shouldBe "value"
            }

            it("block() with timeout") {
                val result = Mono.just("value")
                    .delayElement(Duration.ofMillis(50))
                    .block(Duration.ofSeconds(1))

                result shouldBe "value"
            }

            it("blockOptional() - for potentially empty Mono") {
                val hasValue = Mono.just("value").blockOptional()
                val empty = Mono.empty<String>().blockOptional()

                hasValue.isPresent shouldBe true
                hasValue.get() shouldBe "value"
                empty.isPresent shouldBe false
            }
        }

        describe("Testing best practices with StepVerifier") {

            it("expectNext() - verify specific value") {
                StepVerifier.create(Mono.just("expected"))
                    .expectNext("expected")
                    .verifyComplete()
            }

            it("assertNext() - verify with custom assertion") {
                StepVerifier.create(Mono.just("test"))
                    .assertNext { value ->
                        value.length shouldBe 4
                        value shouldNotBe "other"
                    }
                    .verifyComplete()
            }

            it("expectError() - verify error type") {
                StepVerifier.create(Mono.error<String>(IllegalArgumentException("bad")))
                    .expectError(IllegalArgumentException::class.java)
                    .verify()
            }

            it("expectErrorMessage() - verify error message") {
                StepVerifier.create(Mono.error<String>(RuntimeException("specific message")))
                    .expectErrorMessage("specific message")
                    .verify()
            }

            it("verifyTimeout() - for delayed emissions") {
                val delayed = Mono.just("value")
                    .delayElement(Duration.ofMillis(100))

                StepVerifier.create(delayed)
                    .expectNext("value")
                    .expectComplete()
                    .verify(Duration.ofSeconds(1))
            }
        }
    })
