# Kotlin Reactive Programming Session

A comprehensive learning project demonstrating the intersection of **Project Reactor** (Mono/Flux) and **Kotlin Coroutines** (suspend/Flow) in Spring Boot applications.

## Table of Contents

- [Introduction](#introduction)
- [Quick Reference](#quick-reference)
- [Java Streams vs Reactive Streams](#java-streams-vs-reactive-streams)
- [Threading Model](#threading-model)
- [Error Handling](#error-handling)
- [Context Propagation](#context-propagation)
- [Backpressure](#backpressure)
- [DOs and DONTs](#dos-and-donts)
- [Running the Tests](#running-the-tests)

---

## Introduction

### What is Reactive Programming?

Reactive programming is a declarative programming paradigm concerned with **data streams** and the **propagation of change**. It enables:

- **Non-blocking I/O**: Efficient resource utilization
- **Backpressure**: Handling fast producers and slow consumers
- **Composability**: Building complex async workflows declaratively
- **Resilience**: Built-in error handling and recovery

### Project Reactor vs Kotlin Coroutines

| Aspect | Project Reactor | Kotlin Coroutines |
|--------|-----------------|-------------------|
| **Origin** | Java/Spring ecosystem | Kotlin language feature |
| **API Style** | Fluent chain operators | Sequential (imperative-looking) |
| **Learning Curve** | Steeper | Gentler for imperative devs |
| **Debugging** | Stack traces can be complex | Better stack traces |
| **Interop** | Native in Spring WebFlux | Excellent via kotlinx-coroutines-reactor |

### When to Use Which?

**Use Project Reactor when:**
- Working primarily with Java libraries
- Need fine-grained control over operators
- Team is experienced with reactive streams

**Use Kotlin Coroutines when:**
- Writing Kotlin-first applications
- Prefer sequential-looking async code
- Need simpler error handling with try/catch

**Best Practice:** Use both! Leverage Reactor for Spring WebFlux integration and convert to coroutines for business logic.

---

## Quick Reference

### Mono/Flux vs Suspend/Flow

| Reactor | Kotlin Coroutines | Description |
|---------|-------------------|-------------|
| `Mono<T>` | `suspend fun(): T` | Single async value (0..1) |
| `Flux<T>` | `Flow<T>` | Stream of async values (0..N) |
| `Mono.just(v)` | Just return `v` | Wrap a value |
| `Mono.empty()` | Return `null` or use `emptyFlow()` | Empty/absent value |
| `Mono.error(e)` | `throw e` | Signal an error |
| `Mono.defer { }` | Use suspend function directly | Lazy evaluation |
| `flatMap` | Another `suspend` call | Async composition |
| `map` | Regular function call | Sync transformation |

### Bridge Functions (kotlinx-coroutines-reactor)

| From | To | Function | Notes |
|------|-----|----------|-------|
| `Mono<T>` | `T` | `awaitSingle()` | Throws if empty |
| `Mono<T>` | `T?` | `awaitSingleOrNull()` | Returns null if empty |
| `Mono<T>` | `T` | `awaitFirst()` | Same as awaitSingle for Mono |
| `Flux<T>` | `T` | `awaitFirst()` | First element |
| `Flux<T>` | `T` | `awaitLast()` | Last element |
| `Flux<T>` | `List<T>` | `awaitFirstOrNull()` | First or null |
| `Flux<T>` | `Flow<T>` | `asFlow()` | Convert to Flow |
| `suspend () -> T` | `Mono<T>` | `mono { }` | Wrap suspend in Mono |
| `Flow<T>` | `Flux<T>` | `asFlux()` | Convert to Flux |
| `Flow<T>` | `Publisher<T>` | `asPublisher()` | For Java interop |

### Common Operators Comparison

| Operation | Reactor | Kotlin Flow |
|-----------|---------|-------------|
| Transform | `map { }` | `map { }` |
| Async transform | `flatMap { }` | `suspend` call in `map` |
| Filter | `filter { }` | `filter { }` |
| Take first N | `take(n)` | `take(n)` |
| Skip first N | `skip(n)` | `drop(n)` |
| Handle errors | `onErrorResume { }` | `catch { }` |
| Side effects | `doOnNext { }` | `onEach { }` |
| Collect to list | `collectList()` | `toList()` |
| Reduce | `reduce { }` | `reduce { }` |

---

## Java Streams vs Reactive Streams

### Why Java Streams Cannot Replace Flux/Flow

| Aspect | Java Stream | Flux / Flow |
|--------|-------------|-------------|
| **Execution Model** | Pull-based, synchronous | Push-based, asynchronous |
| **Reusability** | Single-use (terminal op closes) | Reusable (cold streams) |
| **Backpressure** | Not supported | Built-in support |
| **Async Operations** | Cannot handle natively | Designed for async |
| **Error Handling** | Exceptions break pipeline | First-class error operators |
| **Lazy Evaluation** | Lazy but blocking | Lazy and non-blocking |
| **Threading** | Caller thread (or parallel pool) | Threading agnostic |
| **Infinite Sources** | Problematic (OOM risk) | Handled gracefully |

### Key Differences Explained

```kotlin
// Java Stream - BLOCKS until complete
val result = listOf(1, 2, 3).stream()
    .map { it * 2 }
    .collect(Collectors.toList()) // Blocks here!

// Flux - Non-blocking, subscribes when ready
Flux.just(1, 2, 3)
    .map { it * 2 }
    .subscribe { println(it) } // Non-blocking subscription

// Flow - Non-blocking, collects in coroutine
flow {
    emit(1); emit(2); emit(3)
}.map { it * 2 }
 .collect { println(it) } // Suspends, doesn't block
```

### Stream is Single-Use

```kotlin
val stream = listOf(1, 2, 3).stream()
stream.count() // Works
stream.count() // IllegalStateException: stream has already been operated upon
```

### Stream Cannot Handle Async Sources

```kotlin
// This is awkward with Streams
val futures = listOf(
    CompletableFuture.supplyAsync { fetchUser(1) },
    CompletableFuture.supplyAsync { fetchUser(2) }
)
// You have to block or use CompletableFuture.allOf()

// With Flux - natural async composition
Flux.merge(
    Mono.fromCallable { fetchUser(1) }.subscribeOn(Schedulers.boundedElastic()),
    Mono.fromCallable { fetchUser(2) }.subscribeOn(Schedulers.boundedElastic())
).collectList()
```

---

## Threading Model

### Reactive Programming is Threading Agnostic

One of the most powerful features of reactive programming is the ability to **decouple business logic from threading concerns**. You can change where code executes without modifying the code itself.

```
┌─────────────────────────────────────────────────────────────────┐
│                    Reactive Pipeline                            │
├─────────────────────────────────────────────────────────────────┤
│  Source ──► Op1 ──► Op2 ──► publishOn() ──► Op3 ──► Op4 ──► Sink│
│    │                           │                                │
│    └── subscribeOn() ──────────┘                                │
│        (controls source)     (switches downstream)              │
└─────────────────────────────────────────────────────────────────┘
```

### subscribeOn vs publishOn

| Operator | What it Controls | Position Matters? |
|----------|------------------|-------------------|
| `subscribeOn` | Where subscription signal travels up (source thread) | No - only first one matters |
| `publishOn` | Where subsequent operators execute (downstream) | Yes - can use multiple times |

```kotlin
Flux.range(1, 10)
    .map { /* runs on subscribeOn thread */ }
    .subscribeOn(Schedulers.boundedElastic())  // Affects source emission
    .publishOn(Schedulers.parallel())          // Switch for downstream
    .map { /* runs on parallel scheduler */ }
    .publishOn(Schedulers.single())            // Switch again
    .map { /* runs on single scheduler */ }
    .subscribe()
```

### Kotlin Flow: flowOn

In Kotlin Flow, `flowOn` changes the context for **upstream** operations (opposite of `publishOn`):

```kotlin
flow {
    emit(1)  // Runs on IO dispatcher
    emit(2)
}
.flowOn(Dispatchers.IO)  // Affects upstream (emissions)
.map { it * 2 }          // Runs on collector's context
.collect { }             // Runs on caller's context
```

### Reactor Schedulers

| Scheduler | Use Case | Thread Pool |
|-----------|----------|-------------|
| `Schedulers.immediate()` | Current thread | None |
| `Schedulers.single()` | Sequential, low-latency | 1 thread |
| `Schedulers.parallel()` | CPU-bound work | N = CPU cores |
| `Schedulers.boundedElastic()` | Blocking I/O | Grows up to limit |
| `Schedulers.fromExecutor()` | Custom executor | User-defined |

### Kotlin Dispatchers

| Dispatcher | Use Case | Notes |
|------------|----------|-------|
| `Dispatchers.Default` | CPU-bound | Similar to parallel() |
| `Dispatchers.IO` | I/O-bound, blocking | Similar to boundedElastic() |
| `Dispatchers.Main` | UI thread | Android/Desktop |
| `Dispatchers.Unconfined` | No confinement | Use with caution |

---

## Error Handling

### Reactor Error Handling

```kotlin
Mono.just("data")
    .flatMap { riskyOperation(it) }
    .onErrorReturn("default")           // Fallback value
    .onErrorResume { Mono.just("alt") } // Fallback publisher
    .onErrorMap { CustomException(it) } // Transform error
    .doOnError { log.error("Failed", it) } // Side effect
    .retry(3)                           // Simple retry
    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))) // Exponential backoff
```

### Kotlin Coroutines Error Handling

```kotlin
// suspend function - use try/catch
suspend fun fetchData(): String {
    return try {
        riskyOperation()
    } catch (e: Exception) {
        "default"
    }
}

// Flow - use catch operator
flow { emit(riskyOperation()) }
    .catch { e -> emit("default") }  // Catches upstream errors
    .onEach { }                      // Won't catch errors here
    .collect { }

// Using runCatching
val result = runCatching { riskyOperation() }
    .getOrDefault("default")
```

### Error Handling Comparison

| Reactor | Kotlin | Use Case |
|---------|--------|----------|
| `onErrorReturn(v)` | `catch { emit(v) }` | Provide fallback value |
| `onErrorResume(fn)` | `catch { emitAll(fn()) }` | Provide fallback stream |
| `onErrorMap(fn)` | `catch { throw fn(it) }` | Transform error type |
| `retry(n)` | `retry(n)` | Simple retry N times |
| `retryWhen(spec)` | Custom with `retryWhen` | Complex retry logic |
| `doOnError { }` | `onEach { }.catch { }` | Log without recovering |

---

## Context Propagation

### The Challenge

In reactive streams, operations can switch threads frequently. Traditional `ThreadLocal` doesn't work because:

1. Thread context is lost when switching schedulers
2. Reactive operators may execute on different threads
3. MDC (Mapped Diagnostic Context) for logging breaks

### Reactor Context

```kotlin
Mono.just("data")
    .flatMap { 
        Mono.deferContextual { ctx ->
            val userId = ctx.get<String>("userId")
            Mono.just("$it for $userId")
        }
    }
    .contextWrite(Context.of("userId", "123"))  // Add to context
    .subscribe()
```

### Kotlin Coroutine Context

```kotlin
// Using coroutine context elements
suspend fun processWithContext() {
    val userId = coroutineContext[UserIdContext]?.userId
    // process...
}

// Custom context element
data class UserIdContext(val userId: String) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> = Key
    companion object Key : CoroutineContext.Key<UserIdContext>
}

// Usage
withContext(UserIdContext("123")) {
    processWithContext()
}
```

### MDC Propagation in Reactor

```kotlin
// Add reactor-context-propagation dependency
Mono.just("data")
    .doOnNext { log.info("Processing") } // MDC available
    .contextWrite { ctx ->
        ctx.put("mdc", MDC.getCopyOfContextMap() ?: emptyMap())
    }
```

---

## Backpressure

### What is Backpressure?

Backpressure is a mechanism for handling the scenario where a **producer emits data faster than the consumer can process it**.

```
Fast Producer ──────►  Buffer  ──────► Slow Consumer
     1000/s              ???              100/s
```

Without backpressure, the buffer grows unbounded → OutOfMemoryError

### Reactor Backpressure Strategies

```kotlin
Flux.range(1, 1_000_000)
    .onBackpressureBuffer(100)   // Buffer up to 100, then error
    .onBackpressureDrop()        // Drop items if overwhelmed
    .onBackpressureLatest()      // Keep only latest item
    .onBackpressureError()       // Error immediately
    .subscribe()
```

### Controlling Demand with limitRate

```kotlin
Flux.range(1, 1000)
    .limitRate(10)  // Request 10 at a time
    .subscribe()
```

### Kotlin Flow Backpressure

```kotlin
flow { 
    repeat(1000) { emit(it) }
}
.buffer(100)           // Buffer with capacity
.conflate()            // Keep latest, drop intermediates
.collectLatest { }     // Cancel previous if new arrives
.collect { }
```

---

## DOs and DONTs

### DO

#### 1. Use appropriate schedulers for blocking operations
```kotlin
// DO: Wrap blocking calls with boundedElastic
Mono.fromCallable { blockingDatabaseCall() }
    .subscribeOn(Schedulers.boundedElastic())
```

#### 2. Propagate context properly
```kotlin
// DO: Pass context through the chain
mono { fetchUser() }
    .contextWrite(Context.of("traceId", traceId))
```

#### 3. Use StepVerifier for testing Reactor
```kotlin
// DO: Use StepVerifier
StepVerifier.create(flux)
    .expectNext(1, 2, 3)
    .verifyComplete()
```

#### 4. Use runTest for testing coroutines
```kotlin
// DO: Use runTest for suspend functions
@Test
fun `test suspend function`() = runTest {
    val result = mySuspendFunction()
    result shouldBe expected
}
```

#### 5. Limit parallelism with flatMap concurrency
```kotlin
// DO: Limit concurrent operations
Flux.range(1, 100)
    .flatMap({ callApi(it) }, 10)  // Max 10 concurrent
```

#### 6. Handle empty cases explicitly
```kotlin
// DO: Handle empty Mono
mono.switchIfEmpty(Mono.just(defaultValue))

// DO: Handle empty Flow
flow.firstOrNull() ?: defaultValue
```

### DON'T

#### 1. Never block in reactive chains
```kotlin
// DON'T: This defeats the purpose of reactive
Mono.just(data)
    .map { it.block() }  // NEVER do this!
    
// DON'T: Blocking in subscribe
flux.subscribe { 
    Thread.sleep(1000)  // Blocks event loop!
}
```

#### 2. Don't use ThreadLocal without context propagation
```kotlin
// DON'T: ThreadLocal breaks across schedulers
val threadLocal = ThreadLocal<String>()
Mono.just("data")
    .publishOn(Schedulers.parallel())
    .map { threadLocal.get() }  // Will be null!
```

#### 3. Avoid side effects in map operators
```kotlin
// DON'T: Side effects in map
.map { 
    logger.info("Processing $it")  // Side effect!
    it * 2 
}

// DO: Use doOnNext for side effects
.doOnNext { logger.info("Processing $it") }
.map { it * 2 }
```

#### 4. Don't ignore backpressure
```kotlin
// DON'T: Unbounded buffer can cause OOM
.onBackpressureBuffer()  // Unbounded!

// DO: Set a limit
.onBackpressureBuffer(1000, BufferOverflowStrategy.DROP_LATEST)
```

#### 5. Never subscribe inside subscribe (callback hell)
```kotlin
// DON'T: Nested subscribes
mono1.subscribe { result1 ->
    mono2.subscribe { result2 ->  // Callback hell!
        // ...
    }
}

// DO: Use flatMap for composition
mono1.flatMap { result1 -> 
    mono2.map { result2 -> 
        combine(result1, result2) 
    }
}
```

#### 6. Don't forget to subscribe (nothing happens without subscription)
```kotlin
// DON'T: This does nothing!
Mono.just("data")
    .map { process(it) }
// Missing .subscribe() or return to framework

// DO: Subscribe or return to caller
Mono.just("data")
    .map { process(it) }
    .subscribe()  // Now it runs
```

---

## Running the Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.example.reactive.*MonoBasicsTest"

# Run with verbose output
./gradlew test --info

# Enable virtual threads for Reactor (Java 21+)
./gradlew test -Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true
```

---

## Testing Notes

### Kotest is Coroutine-Enabled

Kotest's `DescribeSpec` already supports suspend functions natively. You don't need `runTest` wrappers:

```kotlin
// No runTest needed!
class MyTest : DescribeSpec({
    it("suspend functions work directly") {
        delay(100)
        someValue shouldBe expected
    }
})
```

### Turbine for Flow Testing

We use [Turbine](https://github.com/cashapp/turbine) for elegant Flow testing:

```kotlin
flowOf(1, 2, 3).test {
    awaitItem() shouldBe 1
    awaitItem() shouldBe 2
    awaitItem() shouldBe 3
    awaitComplete()
}

// Error testing
flow { throw RuntimeException("Oops") }.test {
    awaitError().message shouldBe "Oops"
}
```

---

## Virtual Threads (Java 21+)

### Enabling Virtual Threads in Reactor

Set the system property to make `Schedulers.boundedElastic()` use virtual threads:

```
-Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true
```

Or create a custom virtual thread scheduler:

```kotlin
val virtualScheduler = Schedulers.fromExecutor(
    Executors.newVirtualThreadPerTaskExecutor()
)

Mono.fromCallable { blockingOperation() }
    .subscribeOn(virtualScheduler)
```

### Spring Boot 3.2+

Enable virtual threads in `application.properties`:

```properties
spring.threads.virtual.enabled=true
```

### Virtual Threads vs Reactive

| Use Case | Virtual Threads | Reactive (Flux/Flow) |
|----------|-----------------|----------------------|
| Blocking I/O | Excellent | Wrap with scheduler |
| Streaming | Not ideal | Excellent |
| Backpressure | Manual | Built-in |
| Existing blocking code | Easy migration | Requires rewrite |
| Complex async pipelines | Simple | Rich operators |

## Module Overview

| Module | File | Topics |
|--------|------|--------|
| 1 | `01_MonoBasicsTest.kt` | Mono creation, transformations, fallbacks |
| 2 | `02_FluxBasicsTest.kt` | Flux operations, combining publishers |
| 3 | `03_SuspendBasicsTest.kt` | Suspend functions, structured concurrency |
| 4 | `04_FlowBasicsTest.kt` | Flow creation, operators, Turbine testing |
| 5 | `05_JavaStreamsVsReactiveTest.kt` | Why Streams can't replace Flux/Flow |
| 6 | `06_ThreadingAndSchedulersTest.kt` | subscribeOn, publishOn, flowOn |
| 7 | `07_ReactorToCoroutinesTest.kt` | awaitSingle, asFlow conversions |
| 8 | `08_CoroutinesToReactorTest.kt` | mono{}, flux{}, asFlux conversions |
| 9 | `09_ErrorHandlingTest.kt` | Error operators, retry patterns |
| 10 | `10_ContextPropagationTest.kt` | Reactor Context, CoroutineContext |
| 11 | `11_BackpressureTest.kt` | Buffer, drop, latest strategies |
| 12 | `12_SpringWebFluxIntegrationTest.kt` | WebClient, reactive controllers |
| 13 | `13_DosAndDontsTest.kt` | Best practices, anti-patterns |
| 14 | `14_VirtualThreadsTest.kt` | Virtual threads (Project Loom) |

---

## Dependencies

```kotlin
// Reactor
implementation("org.springframework.boot:spring-boot-starter-webflux")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

// Testing
testImplementation("io.kotest:kotest-runner-junit5")
testImplementation("io.kotest:kotest-assertions-core")
testImplementation("io.projectreactor:reactor-test")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
testImplementation("app.cash.turbine:turbine:1.2.0")  // Flow testing
```

---

## License

MIT

