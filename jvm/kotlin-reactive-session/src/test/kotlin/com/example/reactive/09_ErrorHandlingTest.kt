package com.example.reactive

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

/**
 * # Module 9: Error Handling
 *
 * Error handling is crucial in reactive systems.
 * Both Reactor and Coroutines provide rich error handling mechanisms.
 *
 * Key concepts:
 * - Reactor: onErrorReturn, onErrorResume, onErrorMap, retry
 * - Coroutines: try-catch, catch operator, runCatching
 *
 * Note: Kotest DescribeSpec is already coroutine-enabled - no runTest needed!
 */
class ErrorHandlingTest :
    DescribeSpec({

        describe("Reactor error handling - Mono") {

            it("onErrorReturn - provide fallback value") {
                val mono = Mono.error<String>(RuntimeException("Failed"))
                    .onErrorReturn("fallback")

                StepVerifier.create(mono)
                    .expectNext("fallback")
                    .verifyComplete()
            }

            it("onErrorResume - switch to fallback publisher") {
                val mono = Mono.error<String>(RuntimeException("Primary failed"))
                    .onErrorResume { Mono.just("from fallback mono") }

                StepVerifier.create(mono)
                    .expectNext("from fallback mono")
                    .verifyComplete()
            }

            it("onErrorMap - transform error type") {
                class CustomException(message: String) : RuntimeException(message)

                val mono = Mono.error<String>(RuntimeException("Original"))
                    .onErrorMap { CustomException("Wrapped: ${it.message}") }

                StepVerifier.create(mono)
                    .expectError(CustomException::class.java)
                    .verify()
            }
        }

        describe("Reactor error handling - Flux") {

            it("Error terminates the stream") {
                val flux = Flux.just(1, 2, 3)
                    .map {
                        if (it == 2) throw RuntimeException("Error at 2")
                        it
                    }

                StepVerifier.create(flux)
                    .expectNext(1)
                    .expectError(RuntimeException::class.java)
                    .verify()
            }

            it("onErrorReturn completes stream with fallback") {
                val flux = Flux.just(1, 2, 3)
                    .map {
                        if (it == 2) throw RuntimeException()
                        it * 10
                    }
                    .onErrorReturn(-1)

                StepVerifier.create(flux)
                    .expectNext(10, -1)
                    .verifyComplete()
            }
        }

        describe("Reactor retry patterns") {

            it("retry(n) - simple retry N times") {
                var attempts = 0

                val mono = Mono.defer {
                    attempts++
                    if (attempts < 3) {
                        Mono.error(RuntimeException("Attempt $attempts"))
                    } else {
                        Mono.just("Success on attempt $attempts")
                    }
                }.retry(2)

                StepVerifier.create(mono)
                    .expectNext("Success on attempt 3")
                    .verifyComplete()
            }
        }

        describe("Kotlin Coroutines error handling") {

            it("try-catch works naturally in suspend functions") {
                suspend fun riskyOperation(): String {
                    delay(10)
                    throw RuntimeException("Failed!")
                }

                val result = try {
                    riskyOperation()
                } catch (e: RuntimeException) {
                    "Caught: ${e.message}"
                }

                result shouldBe "Caught: Failed!"
            }

            it("runCatching for functional style") {
                suspend fun mightFail(shouldFail: Boolean): String {
                    delay(10)
                    if (shouldFail) throw RuntimeException("Error")
                    return "Success"
                }

                val success = runCatching { mightFail(false) }
                val failure = runCatching { mightFail(true) }

                success.isSuccess shouldBe true
                failure.isFailure shouldBe true
            }
        }

        describe("Kotlin Flow error handling") {

            it("catch operator catches upstream errors") {
                val result = flow {
                    emit(1)
                    emit(2)
                    throw RuntimeException("Flow error")
                }
                    .catch { emit(-1) }
                    .toList()

                result shouldBe listOf(1, 2, -1)
            }

            it("catch only catches upstream errors") {
                shouldThrow<RuntimeException> {
                    flowOf(1, 2, 3)
                        .catch { emit(-1) }
                        .collect {
                            if (it == 2) throw RuntimeException("In collect")
                        }
                }
            }
        }

        describe("Flow retry patterns") {

            it("retry operator - simple retry") {
                var attempts = 0

                val result = flow {
                    attempts++
                    if (attempts < 3) throw RuntimeException()
                    emit("Success on attempt $attempts")
                }
                    .retry(2)
                    .first()

                result shouldBe "Success on attempt 3"
            }
        }

        describe("Best practices") {

            it("Always handle errors at the end of the chain") {
                val mono = Mono.just("data")
                    .flatMap { Mono.error<String>(RuntimeException()) }
                    .onErrorResume { Mono.just("recovered") }

                StepVerifier.create(mono)
                    .expectNext("recovered")
                    .verifyComplete()
            }

            it("Don't swallow errors silently") {
                val errorLog = mutableListOf<String>()

                flow<Int> { throw RuntimeException("Error") }
                    .catch { e ->
                        errorLog.add("Error: ${e.message}")
                    }
                    .collect()

                errorLog.isNotEmpty() shouldBe true
            }
        }
    })
