package com.example.reactive

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.*

/**
 * # Module 3: Suspend Functions Basics
 *
 * Suspend functions are the foundation of Kotlin Coroutines.
 * They represent computations that can be paused and resumed without blocking threads.
 *
 * Key concepts covered:
 * - What suspend functions are
 * - Difference between suspend and blocking
 * - Coroutine builders (launch, async, runBlocking)
 * - Structured concurrency
 * - Dispatchers and context
 */
class SuspendBasicsTest :
    DescribeSpec({

        describe("What is a suspend function?") {

            it("suspend keyword marks functions that can be paused") {
                // A suspend function can suspend execution without blocking a thread
                // It can only be called from another suspend function or a coroutine

                suspend fun fetchData(): String {
                    delay(100) // Suspends, doesn't block!
                    return "data"
                }

                val result = fetchData()
                result shouldBe "data"
            }

            it("suspend functions look sequential but are non-blocking") {
                suspend fun step1(): String {
                    delay(100)
                    return "Step 1 done"
                }

                suspend fun step2(): String {
                    delay(100)
                    return "Step 2 done"
                }

                suspend fun workflow(): String {
                    val result1 = step1() // Waits for step1
                    val result2 = step2() // Then waits for step2
                    return "$result1, $result2"
                }

                val result = workflow()
                result shouldBe "Step 1 done, Step 2 done"
            }

            it("delay() vs Thread.sleep() - the key difference") {
                // Note: We demonstrate the concept without timing assertions
                // because runTest uses virtual time

                // delay() suspends the coroutine without blocking the thread
                // Thread.sleep() BLOCKS the thread (avoid in coroutines!)

                // In real code:
                // - delay(100) frees the thread for 100ms
                // - Thread.sleep(100) blocks the thread for 100ms

                // Both accomplish "wait 100ms" but delay() is non-blocking
                delay(10) // Non-blocking wait
            }
        }

        describe("Coroutine builders") {

            it("coroutineScope - creates a suspending scope") {
                // coroutineScope creates a scope that suspends until all children complete
                // Unlike runBlocking, it doesn't block - it suspends!
                val result = coroutineScope {
                    delay(10)
                    "completed"
                }

                result shouldBe "completed"
            }

            it("launch - fire and forget coroutine") {
                val results = mutableListOf<String>()

                // launch returns a Job, not a result
                val job = coroutineScope {
                    launch {
                        delay(50)
                        results.add("from launch")
                    }
                }

                job.join() // Wait for completion

                results shouldBe listOf("from launch")
            }

            it("async - coroutine that returns a result") {
                // async returns a Deferred<T> which is a future-like object
                val result = coroutineScope {
                    val deferred: Deferred<String> = async {
                        delay(50)
                        "async result"
                    }
                    // await() suspends until result is ready
                    deferred.await()
                }

                result shouldBe "async result"
            }

            it("async enables parallel execution") {
                // Demonstrates parallel execution concept
                suspend fun fetchUser(): String {
                    delay(10)
                    return "User"
                }

                suspend fun fetchOrders(): String {
                    delay(10)
                    return "Orders"
                }

                // Sequential - one after another
                val sequential = coroutineScope {
                    val user = fetchUser()
                    val orders = fetchOrders()
                    "$user, $orders"
                }

                // Parallel with async - both at same time
                val parallel = coroutineScope {
                    val userDeferred = async { fetchUser() }
                    val ordersDeferred = async { fetchOrders() }
                    "${userDeferred.await()}, ${ordersDeferred.await()}"
                }

                sequential shouldBe "User, Orders"
                parallel shouldBe "User, Orders"
            }

            it("coroutineScope - creates a scope for child coroutines") {
                suspend fun loadData(): List<String> = coroutineScope {
                    val data1 = async {
                        delay(50)
                        "data1"
                    }
                    val data2 = async {
                        delay(50)
                        "data2"
                    }
                    listOf(data1.await(), data2.await())
                }

                val result = loadData()
                result shouldBe listOf("data1", "data2")
            }

            it("supervisorScope - child failures don't cancel siblings") {
                val results = mutableListOf<String>()

                supervisorScope {
                    launch {
                        delay(50)
                        results.add("success")
                    }

                    launch {
                        delay(10)
                        throw RuntimeException("Child failed")
                    }.invokeOnCompletion { ex ->
                        if (ex != null) results.add("failed")
                    }
                }

                // Both completed (one successfully, one with failure)
                results.size shouldBe 2
            }
        }

        describe("Structured concurrency") {

            it("parent waits for all children to complete") {
                val log = mutableListOf<String>()

                coroutineScope {
                    launch {
                        delay(100)
                        log.add("child 1 done")
                    }
                    launch {
                        delay(50)
                        log.add("child 2 done")
                    }
                    log.add("parent scope")
                }

                // Parent scope doesn't complete until children are done
                log shouldBe listOf("parent scope", "child 2 done", "child 1 done")
            }

            it("cancelling parent cancels all children") {
                val log = mutableListOf<String>()

                coroutineScope {
                    val job = launch {
                        launch {
                            try {
                                delay(1000)
                                log.add("child 1 done")
                            } catch (e: CancellationException) {
                                log.add("child 1 cancelled")
                                throw e
                            }
                        }
                        launch {
                            try {
                                delay(1000)
                                log.add("child 2 done")
                            } catch (e: CancellationException) {
                                log.add("child 2 cancelled")
                                throw e
                            }
                        }
                    }

                    delay(50)
                    job.cancel() // Cancel parent
                    job.join()
                }

                log shouldBe listOf("child 1 cancelled", "child 2 cancelled")
            }

            it("exception in child cancels parent and siblings (default behavior)") {
                val log = mutableListOf<String>()

                try {
                    coroutineScope {
                        launch {
                            delay(100)
                            log.add("slow child done")
                        }
                        launch {
                            delay(10)
                            throw RuntimeException("Fast child failed")
                        }
                    }
                } catch (e: RuntimeException) {
                    log.add("caught: ${e.message}")
                }

                // Slow child was cancelled due to sibling failure
                log shouldBe listOf("caught: Fast child failed")
            }
        }

        describe("Dispatchers - where coroutines run") {

            it("Dispatchers.Default - for CPU-intensive work") {
                val threadName = withContext(Dispatchers.Default) {
                    // CPU-bound work runs on shared pool
                    Thread.currentThread().name
                }

                threadName shouldNotBe null
                // Typically "DefaultDispatcher-worker-X"
            }

            it("Dispatchers.IO - for blocking I/O operations") {
                val threadName = withContext(Dispatchers.IO) {
                    // I/O-bound work runs on elastic pool
                    Thread.currentThread().name
                }

                threadName shouldNotBe null
                // Typically "DefaultDispatcher-worker-X" (shared with Default but more threads)
            }

            it("Dispatchers.Unconfined - starts in caller thread, resumes anywhere") {
                val threads = mutableListOf<String>()

                withContext(Dispatchers.Unconfined) {
                    threads.add(Thread.currentThread().name)
                    delay(10) // After suspension, may resume on different thread
                    threads.add(Thread.currentThread().name)
                }

                threads.size shouldBe 2
                // Threads may or may not be the same
            }

            it("withContext() - switches dispatcher temporarily") {
                suspend fun cpuIntensiveWork(): Int = withContext(Dispatchers.Default) {
                    // Heavy computation here
                    (1..1000).sum()
                }

                suspend fun blockingIoWork(): String = withContext(Dispatchers.IO) {
                    // Blocking I/O here
                    "data"
                }

                val sum = cpuIntensiveWork()
                val data = blockingIoWork()

                sum shouldBe 500500
                data shouldBe "data"
            }
        }

        describe("Exception handling in coroutines") {

            it("try-catch works in suspend functions") {
                suspend fun riskyOperation(): String {
                    delay(10)
                    throw RuntimeException("Something went wrong")
                }

                val result = try {
                    riskyOperation()
                } catch (e: RuntimeException) {
                    "Fallback: ${e.message}"
                }

                result shouldBe "Fallback: Something went wrong"
            }

            it("runCatching for functional error handling") {
                suspend fun riskyOperation(shouldFail: Boolean): String {
                    delay(10)
                    if (shouldFail) throw RuntimeException("Failed!")
                    return "Success"
                }

                val successResult = runCatching { riskyOperation(false) }
                val failureResult = runCatching { riskyOperation(true) }

                successResult.isSuccess shouldBe true
                successResult.getOrNull() shouldBe "Success"

                failureResult.isFailure shouldBe true
                failureResult.exceptionOrNull()?.message shouldBe "Failed!"

                // Use getOrDefault, getOrElse for fallbacks
                failureResult.getOrDefault("Default") shouldBe "Default"
                failureResult.getOrElse { "Fallback: ${it.message}" } shouldBe "Fallback: Failed!"
            }

            it("CoroutineExceptionHandler for uncaught exceptions") {
                val exceptions = mutableListOf<String>()

                val handler = CoroutineExceptionHandler { _, exception ->
                    exceptions.add("Caught: ${exception.message}")
                }

                // Create a custom scope with the handler
                // The handler only works at the scope level, not in child coroutines
                val scope = CoroutineScope(Dispatchers.Default + handler)
                val job = scope.launch {
                    throw RuntimeException("Uncaught!")
                }
                job.join()

                exceptions shouldBe listOf("Caught: Uncaught!")
            }
        }

        describe("Cancellation") {

            it("coroutines are cooperative with cancellation") {
                val log = mutableListOf<String>()

                coroutineScope {
                    val job = launch {
                        repeat(10) { i ->
                            log.add("Step $i")
                            delay(50) // Suspension point - checks for cancellation
                        }
                    }

                    delay(120)
                    job.cancel()
                    job.join()
                }

                // Only ~2 steps completed before cancellation
                (log.size < 10) shouldBe true
            }

            it("isActive check for CPU-intensive work") {
                val log = mutableListOf<String>()

                coroutineScope {
                    val job = launch(Dispatchers.Default) {
                        var i = 0
                        while (isActive && i < 100) { // Must check isActive manually!
                            i++
                            log.add("Iteration $i")
                            // CPU work without suspension point
                        }
                    }

                    job.join()
                }

                log.isNotEmpty() shouldBe true
            }

            it("ensureActive() throws if cancelled") {
                coroutineScope {
                    val job = launch {
                        repeat(10) { i ->
                            ensureActive() // Throws CancellationException if cancelled
                            // CPU work here
                            delay(50)
                        }
                    }

                    delay(120)
                    job.cancelAndJoin()
                }
            }

            it("withTimeout for time-limited operations") {
                val result = runCatching {
                    withTimeout(100) {
                        delay(500) // Takes too long
                        "completed"
                    }
                }

                result.isFailure shouldBe true
                result.exceptionOrNull() shouldNotBe null
            }

            it("withTimeoutOrNull returns null on timeout") {
                val result = withTimeoutOrNull(100) {
                    delay(500)
                    "completed"
                }

                result shouldBe null

                val quickResult = withTimeoutOrNull(100) {
                    delay(10)
                    "quick"
                }

                quickResult shouldBe "quick"
            }
        }

        describe("yield() - cooperative multitasking") {

            it("yield() gives other coroutines a chance to run") {
                val log = mutableListOf<String>()

                coroutineScope {
                    val job1 = launch {
                        repeat(3) {
                            log.add("A$it")
                            yield() // Let other coroutines run
                        }
                    }

                    val job2 = launch {
                        repeat(3) {
                            log.add("B$it")
                            yield()
                        }
                    }

                    job1.join()
                    job2.join()
                }

                // A and B interleave due to yield()
                log shouldBe listOf("A0", "B0", "A1", "B1", "A2", "B2")
            }
        }

        describe("Job and its states") {

            it("Job lifecycle: New -> Active -> Completing -> Completed") {
                coroutineScope {
                    val job = launch(start = CoroutineStart.LAZY) {
                        delay(100)
                    }

                    job.isActive shouldBe false // LAZY start, not active yet
                    job.start()
                    job.isActive shouldBe true // Now active

                    job.join()
                    job.isCompleted shouldBe true // Completed
                    job.isCancelled shouldBe false // Not cancelled
                }
            }

            it("Job can have children") {
                val latch = CompletableDeferred<Unit>()

                coroutineScope {
                    val parentJob = launch {
                        launch {
                            latch.await() // Wait until we check children
                        }
                        launch {
                            latch.await()
                        }
                    }

                    // Wait a bit for children to start
                    delay(10)
                    parentJob.children.count() shouldBe 2

                    latch.complete(Unit) // Release children
                    parentJob.join()
                }
            }
        }

        describe("Best practices summary") {

            it("Always use structured concurrency") {
                // DON'T: GlobalScope.launch { } - unstructured
                // DO: Use coroutineScope, launch in structured parent

                suspend fun loadAllData(): List<String> = coroutineScope {
                    // Children are properly managed
                    val a = async { "data1" }
                    val b = async { "data2" }
                    listOf(a.await(), b.await())
                }

                loadAllData() shouldBe listOf("data1", "data2")
            }

            it("Use appropriate dispatchers") {
                // CPU-bound: Dispatchers.Default
                // I/O-bound: Dispatchers.IO
                // UI: Dispatchers.Main (Android/Desktop)

                // Example of mixed workload
                val result = withContext(Dispatchers.IO) {
                    // Blocking I/O
                    val data = "raw data"

                    withContext(Dispatchers.Default) {
                        // CPU-intensive processing
                        data.uppercase()
                    }
                }

                result shouldBe "RAW DATA"
            }

            it("Handle cancellation properly") {
                suspend fun properResource() = coroutineScope {
                    try {
                        // Acquire resource
                        delay(1000)
                        // Use resource
                    } finally {
                        // Always cleanup, even on cancellation
                        withContext(NonCancellable) {
                            // Cleanup that must complete
                            delay(10)
                        }
                    }
                }

                coroutineScope {
                    val job = launch { properResource() }
                    delay(50)
                    job.cancel()
                    job.join()
                }
            }
        }
    })
