package com.example.reactive

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * # Module 4: Flow Basics
 *
 * Flow is Kotlin's cold asynchronous stream type.
 * It's the coroutine equivalent of Flux - representing 0 to N async values.
 *
 * Key concepts:
 * - Creating Flows
 * - Flow operators
 * - Terminal operators
 * - Cold vs Hot flows
 * - SharedFlow and StateFlow
 *
 * Note: Kotest DescribeSpec is already coroutine-enabled, so we don't need runTest!
 * We use Turbine (https://github.com/cashapp/turbine) for Flow testing.
 */
class FlowBasicsTest :
    DescribeSpec({

        describe("Creating Flows") {

            it("flow { } builder - the primary way to create flows") {
                val myFlow = flow {
                    emit(1)
                    emit(2)
                    emit(3)
                }

                // Using Turbine for elegant Flow testing
                myFlow.test {
                    awaitItem() shouldBe 1
                    awaitItem() shouldBe 2
                    awaitItem() shouldBe 3
                    awaitComplete()
                }
            }

            it("flowOf() - create flow from values") {
                flowOf(1, 2, 3).test {
                    awaitItem() shouldBe 1
                    awaitItem() shouldBe 2
                    awaitItem() shouldBe 3
                    awaitComplete()
                }
            }

            it("asFlow() - convert collections to flow") {
                listOf("a", "b", "c").asFlow().test {
                    awaitItem() shouldBe "a"
                    awaitItem() shouldBe "b"
                    awaitItem() shouldBe "c"
                    awaitComplete()
                }

                (1..3).asFlow().test {
                    awaitItem() shouldBe 1
                    awaitItem() shouldBe 2
                    awaitItem() shouldBe 3
                    awaitComplete()
                }
            }

            it("emptyFlow() - flow with no elements") {
                emptyFlow<Int>().test {
                    awaitComplete()
                }
            }

            it("flow builder with delay - async emission") {
                val flow = flow {
                    repeat(3) {
                        delay(10)
                        emit(it)
                    }
                }

                flow.test {
                    awaitItem() shouldBe 0
                    awaitItem() shouldBe 1
                    awaitItem() shouldBe 2
                    awaitComplete()
                }
            }

            it("channelFlow - for concurrent emissions") {
                val flow = channelFlow {
                    launch { send(1) }
                    launch { send(2) }
                }

                flow.test {
                    val items = setOf(awaitItem(), awaitItem())
                    items shouldBe setOf(1, 2)
                    awaitComplete()
                }
            }
        }

        describe("Intermediate operators") {

            it("map - transform each element") {
                flowOf(1, 2, 3)
                    .map { it * 2 }
                    .test {
                        awaitItem() shouldBe 2
                        awaitItem() shouldBe 4
                        awaitItem() shouldBe 6
                        awaitComplete()
                    }
            }

            it("filter - keep matching elements") {
                flowOf(1, 2, 3, 4, 5)
                    .filter { it % 2 == 0 }
                    .test {
                        awaitItem() shouldBe 2
                        awaitItem() shouldBe 4
                        awaitComplete()
                    }
            }

            it("transform - emit zero or more values per input") {
                flowOf(1, 2)
                    .transform { value ->
                        emit("a$value")
                        emit("b$value")
                    }
                    .test {
                        awaitItem() shouldBe "a1"
                        awaitItem() shouldBe "b1"
                        awaitItem() shouldBe "a2"
                        awaitItem() shouldBe "b2"
                        awaitComplete()
                    }
            }

            it("take - take first N elements") {
                flowOf(1, 2, 3, 4, 5)
                    .take(3)
                    .test {
                        awaitItem() shouldBe 1
                        awaitItem() shouldBe 2
                        awaitItem() shouldBe 3
                        awaitComplete()
                    }
            }

            it("drop - skip first N elements") {
                flowOf(1, 2, 3, 4, 5)
                    .drop(2)
                    .test {
                        awaitItem() shouldBe 3
                        awaitItem() shouldBe 4
                        awaitItem() shouldBe 5
                        awaitComplete()
                    }
            }

            it("takeWhile and dropWhile") {
                flowOf(1, 2, 3, 4, 5)
                    .takeWhile { it < 4 }
                    .test {
                        awaitItem() shouldBe 1
                        awaitItem() shouldBe 2
                        awaitItem() shouldBe 3
                        awaitComplete()
                    }

                flowOf(1, 2, 3, 4, 5)
                    .dropWhile { it < 3 }
                    .test {
                        awaitItem() shouldBe 3
                        awaitItem() shouldBe 4
                        awaitItem() shouldBe 5
                        awaitComplete()
                    }
            }

            it("distinctUntilChanged - remove consecutive duplicates") {
                flowOf(1, 1, 2, 2, 3, 2, 2)
                    .distinctUntilChanged()
                    .test {
                        awaitItem() shouldBe 1
                        awaitItem() shouldBe 2
                        awaitItem() shouldBe 3
                        awaitItem() shouldBe 2
                        awaitComplete()
                    }
            }

            it("onEach - side effects without transforming") {
                val log = mutableListOf<String>()

                flowOf(1, 2, 3)
                    .onEach { log.add("Processing: $it") }
                    .test {
                        awaitItem() shouldBe 1
                        awaitItem() shouldBe 2
                        awaitItem() shouldBe 3
                        awaitComplete()
                    }

                log shouldBe listOf("Processing: 1", "Processing: 2", "Processing: 3")
            }

            it("onStart and onCompletion - lifecycle callbacks") {
                val log = mutableListOf<String>()

                flowOf(1, 2)
                    .onStart { log.add("Started") }
                    .onEach { log.add("Value: $it") }
                    .onCompletion { log.add("Completed") }
                    .test {
                        awaitItem() shouldBe 1
                        awaitItem() shouldBe 2
                        awaitComplete()
                    }

                log shouldBe listOf("Started", "Value: 1", "Value: 2", "Completed")
            }
        }

        describe("Terminal operators") {

            it("collect - consume all values") {
                val collected = mutableListOf<Int>()
                flowOf(1, 2, 3).collect { collected.add(it) }
                collected shouldBe listOf(1, 2, 3)
            }

            it("toList and toSet") {
                flowOf(1, 2, 3).toList() shouldBe listOf(1, 2, 3)
                flowOf(1, 2, 2, 3).toSet() shouldBe setOf(1, 2, 3)
            }

            it("first and firstOrNull") {
                flowOf(1, 2, 3).first() shouldBe 1
                flowOf(1, 2, 3).first { it > 1 } shouldBe 2
                emptyFlow<Int>().firstOrNull() shouldBe null
            }

            it("single and singleOrNull") {
                flowOf(42).single() shouldBe 42
                flowOf(1, 2).singleOrNull() shouldBe null // More than one
                emptyFlow<Int>().singleOrNull() shouldBe null
            }

            it("reduce - combine elements") {
                val sum = flowOf(1, 2, 3, 4, 5).reduce { acc, value -> acc + value }
                sum shouldBe 15
            }

            it("fold - reduce with initial value") {
                val sum = flowOf(1, 2, 3).fold(10) { acc, value -> acc + value }
                sum shouldBe 16 // 10 + 1 + 2 + 3
            }

            it("count") {
                flowOf(1, 2, 3, 4, 5).count() shouldBe 5
                flowOf(1, 2, 3, 4, 5).count { it > 3 } shouldBe 2
            }
        }

        describe("Flow is COLD") {

            it("flow doesn't run until collected") {
                val log = mutableListOf<String>()

                val myFlow = flow {
                    log.add("Flow started")
                    emit(1)
                    emit(2)
                }

                log.size shouldBe 0 // Nothing happened yet!

                myFlow.test {
                    awaitItem() shouldBe 1
                    awaitItem() shouldBe 2
                    awaitComplete()
                }

                log shouldBe listOf("Flow started")
            }

            it("each collection restarts the flow") {
                var counter = 0

                val myFlow = flow {
                    counter++
                    emit("value")
                }

                myFlow.test {
                    awaitItem() shouldBe "value"
                    awaitComplete()
                }

                myFlow.test {
                    awaitItem() shouldBe "value"
                    awaitComplete()
                }

                myFlow.test {
                    awaitItem() shouldBe "value"
                    awaitComplete()
                }

                counter shouldBe 3 // Flow ran 3 times
            }
        }

        describe("Combining flows") {

            it("zip - pairs elements from two flows") {
                val nums = flowOf(1, 2, 3)
                val strs = flowOf("a", "b", "c")

                nums.zip(strs) { n, s -> "$s$n" }.test {
                    awaitItem() shouldBe "a1"
                    awaitItem() shouldBe "b2"
                    awaitItem() shouldBe "c3"
                    awaitComplete()
                }
            }

            it("combine - combines latest values") {
                val nums = flowOf(1, 2)
                val strs = flowOf("a", "b")

                combine(nums, strs) { n, s -> "$s$n" }.test {
                    // combine emits whenever either flow emits
                    cancelAndIgnoreRemainingEvents()
                }
            }

            it("flatMapConcat - sequential inner flows") {
                flowOf(1, 2)
                    .flatMapConcat { n ->
                        flowOf("${n}a", "${n}b")
                    }
                    .test {
                        awaitItem() shouldBe "1a"
                        awaitItem() shouldBe "1b"
                        awaitItem() shouldBe "2a"
                        awaitItem() shouldBe "2b"
                        awaitComplete()
                    }
            }

            it("flatMapMerge - concurrent inner flows") {
                flowOf(1, 2)
                    .flatMapMerge { n ->
                        flowOf("${n}a", "${n}b")
                    }
                    .test {
                        val items = listOf(awaitItem(), awaitItem(), awaitItem(), awaitItem())
                        items.toSet() shouldBe setOf("1a", "1b", "2a", "2b")
                        awaitComplete()
                    }
            }
        }

        describe("Error handling") {

            it("catch operator - catches upstream errors") {
                flow {
                    emit(1)
                    throw RuntimeException("Error!")
                }
                    .catch { emit(-1) } // Fallback
                    .test {
                        awaitItem() shouldBe 1
                        awaitItem() shouldBe -1
                        awaitComplete()
                    }
            }

            it("catch can emit multiple fallback values") {
                flow<Int> { throw RuntimeException() }
                    .catch { emitAll(flowOf(1, 2, 3)) }
                    .test {
                        awaitItem() shouldBe 1
                        awaitItem() shouldBe 2
                        awaitItem() shouldBe 3
                        awaitComplete()
                    }
            }

            it("Flow errors can be caught with Turbine") {
                flow<Int> {
                    emit(1)
                    throw RuntimeException("Oops!")
                }.test {
                    awaitItem() shouldBe 1
                    awaitError().message shouldBe "Oops!"
                }
            }

            it("onCompletion knows about errors") {
                var completionCause: Throwable? = null

                flow<Int> { throw RuntimeException("Error") }
                    .onCompletion { cause -> completionCause = cause }
                    .catch { /* handle */ }
                    .collect()

                completionCause?.message shouldBe "Error"
            }
        }

        describe("Context and flowOn") {

            it("flowOn changes upstream context") {
                val threads = mutableListOf<String>()

                flow {
                    threads.add(Thread.currentThread().name)
                    emit(1)
                }
                    .flowOn(Dispatchers.Default)
                    .onEach { threads.add(Thread.currentThread().name) }
                    .collect()

                // Emission and collection may be on different threads
                threads.size shouldBe 2
            }
        }

        describe("SharedFlow and StateFlow (HOT flows)") {

            it("MutableSharedFlow - hot broadcast stream") {
                val sharedFlow = MutableSharedFlow<Int>(replay = 1)

                // Emit values
                sharedFlow.emit(1)
                sharedFlow.emit(2)

                // Collector gets replayed value
                sharedFlow.first() shouldBe 2
            }

            it("MutableStateFlow - observable state holder") {
                val stateFlow = MutableStateFlow(0)

                // StateFlow always has a value
                stateFlow.value shouldBe 0

                stateFlow.value = 1
                stateFlow.value shouldBe 1

                stateFlow.value = 2
                stateFlow.value shouldBe 2
            }

            it("Testing StateFlow with Turbine - values must be different") {
                val stateFlow = MutableStateFlow("initial")

                stateFlow.test {
                    // StateFlow emits initial value immediately
                    awaitItem() shouldBe "initial"

                    // Update with different value
                    stateFlow.value = "updated"
                    awaitItem() shouldBe "updated"

                    // Update with another different value
                    stateFlow.value = "final"
                    awaitItem() shouldBe "final"

                    // Cancel to stop collecting
                    cancelAndIgnoreRemainingEvents()
                }
            }

            it("shareIn - convert cold to shared hot flow") {
                val scope = CoroutineScope(Dispatchers.Default)
                var emissionCount = 0

                val coldFlow = flow {
                    emissionCount++
                    emit(1)
                }

                val sharedFlow = coldFlow.shareIn(
                    scope = scope,
                    started = SharingStarted.Eagerly,
                    replay = 1,
                )

                delay(50)
                sharedFlow.first() shouldBe 1
                sharedFlow.first() shouldBe 1

                // Only emitted once (shared)
                emissionCount shouldBe 1

                scope.cancel()
            }

            it("stateIn - convert cold to StateFlow") {
                val scope = CoroutineScope(Dispatchers.Default)

                val coldFlow = flowOf(1, 2, 3)

                val stateFlow = coldFlow.stateIn(
                    scope = scope,
                    started = SharingStarted.Eagerly,
                    initialValue = 0,
                )

                // StateFlow keeps latest value
                delay(50)
                stateFlow.value shouldBe 3

                scope.cancel()
            }
        }

        describe("Buffering and conflation") {

            it("buffer - allows producer to run ahead") {
                flowOf(1, 2, 3)
                    .buffer()
                    .map { it * 2 }
                    .test {
                        awaitItem() shouldBe 2
                        awaitItem() shouldBe 4
                        awaitItem() shouldBe 6
                        awaitComplete()
                    }
            }

            it("conflate - keep only latest value when slow") {
                flow {
                    emit(1)
                    emit(2)
                    emit(3)
                }
                    .conflate()
                    .test {
                        // May receive all or some values
                        cancelAndIgnoreRemainingEvents()
                    }
            }

            it("collectLatest - cancels slow collection") {
                val result = mutableListOf<String>()

                flowOf(1, 2, 3)
                    .collectLatest { value ->
                        result.add("Start $value")
                        delay(50)
                        result.add("End $value")
                    }

                // Only the last value completes collection
                result.last() shouldBe "End 3"
            }
        }

        describe("Turbine testing features") {

            it("Turbine provides expectNoEvents for timing checks") {
                flow {
                    delay(50)
                    emit(1)
                }.test {
                    expectNoEvents() // No events yet (checks immediately)
                    awaitItem() shouldBe 1
                    awaitComplete()
                }
            }

            it("Turbine skipItems for skipping expected items") {
                flowOf(1, 2, 3, 4, 5).test {
                    skipItems(3)
                    awaitItem() shouldBe 4
                    awaitItem() shouldBe 5
                    awaitComplete()
                }
            }

            it("Testing multiple flows with turbineScope") {
                turbineScope {
                    val flow1 = flowOf(1, 2).testIn(this)
                    val flow2 = flowOf("a", "b").testIn(this)

                    flow1.awaitItem() shouldBe 1
                    flow2.awaitItem() shouldBe "a"
                    flow1.awaitItem() shouldBe 2
                    flow2.awaitItem() shouldBe "b"

                    flow1.awaitComplete()
                    flow2.awaitComplete()
                }
            }

            it("Named turbines for better error messages") {
                turbineScope {
                    val userFlow = flowOf("Alice").testIn(this, name = "userFlow")
                    val orderFlow = flowOf("Order1").testIn(this, name = "orderFlow")

                    userFlow.awaitItem() shouldBe "Alice"
                    orderFlow.awaitItem() shouldBe "Order1"

                    userFlow.awaitComplete()
                    orderFlow.awaitComplete()
                }
            }
        }
    })
