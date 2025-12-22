package com.example.reactive

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * # Module 2: Flux Basics
 *
 * Flux represents a reactive sequence of 0 to N elements.
 * Think of it as a reactive equivalent of Stream<T> but with:
 * - Backpressure support
 * - Async capabilities
 * - Reusability (cold streams)
 *
 * Key concepts covered:
 * - Creating Flux instances
 * - Transformations (map, flatMap, concatMap)
 * - Filtering and slicing
 * - Aggregation and collection
 * - Combining multiple Flux streams
 */
class FluxBasicsTest :
    DescribeSpec({

        describe("Creating Flux instances") {

            it("Flux.just() - creates Flux from varargs") {
                val flux = Flux.just("A", "B", "C")

                StepVerifier.create(flux)
                    .expectNext("A", "B", "C")
                    .verifyComplete()
            }

            it("Flux.fromIterable() - creates Flux from any Iterable") {
                val list = listOf(1, 2, 3, 4, 5)
                val flux = Flux.fromIterable(list)

                StepVerifier.create(flux)
                    .expectNext(1, 2, 3, 4, 5)
                    .verifyComplete()
            }

            it("Flux.range() - creates Flux of sequential integers") {
                // range(start, count) - emits 'count' numbers starting from 'start'
                val flux = Flux.range(1, 5)

                StepVerifier.create(flux)
                    .expectNext(1, 2, 3, 4, 5)
                    .verifyComplete()
            }

            it("Flux.empty() - creates empty Flux") {
                val flux = Flux.empty<String>()

                StepVerifier.create(flux)
                    .verifyComplete()
            }

            it("Flux.error() - creates Flux that immediately errors") {
                val flux = Flux.error<String>(RuntimeException("Failed!"))

                StepVerifier.create(flux)
                    .expectError(RuntimeException::class.java)
                    .verify()
            }

            it("Flux.interval() - emits Long values periodically") {
                // interval() emits 0, 1, 2, 3... at specified intervals
                // IMPORTANT: runs on Schedulers.parallel() by default
                val flux = Flux.interval(Duration.ofMillis(100))
                    .take(3) // Only take first 3 elements

                StepVerifier.create(flux)
                    .expectNext(0L, 1L, 2L)
                    .verifyComplete()
            }

            it("Flux.generate() - programmatic synchronous generation") {
                // generate() is for stateful, synchronous generation
                // The generator can emit AT MOST one element per invocation
                val flux = Flux.generate<Int, Int>(
                    { 0 }, // Initial state
                    { state, sink ->
                        sink.next(state)
                        if (state >= 4) sink.complete()
                        state + 1 // Return new state
                    },
                )

                StepVerifier.create(flux)
                    .expectNext(0, 1, 2, 3, 4)
                    .verifyComplete()
            }

            it("Flux.create() - programmatic async generation") {
                // create() is for bridging async APIs
                // Can emit multiple elements, handle backpressure
                val flux = Flux.create<String> { sink ->
                    // Simulate async callback
                    listOf("A", "B", "C").forEach { sink.next(it) }
                    sink.complete()
                }

                StepVerifier.create(flux)
                    .expectNext("A", "B", "C")
                    .verifyComplete()
            }

            it("Flux.defer() - lazy Flux creation") {
                val counter = AtomicInteger(0)

                val lazyFlux = Flux.defer {
                    counter.incrementAndGet()
                    Flux.just("value")
                }

                // Not subscribed yet
                counter.get() shouldBe 0

                lazyFlux.blockFirst()
                lazyFlux.blockFirst()

                // Factory called on each subscription
                counter.get() shouldBe 2
            }
        }

        describe("Transformations") {

            it("map() - synchronous element transformation") {
                val flux = Flux.just(1, 2, 3)
                    .map { it * 2 }

                StepVerifier.create(flux)
                    .expectNext(2, 4, 6)
                    .verifyComplete()
            }

            it("flatMap() - async transformation, may interleave") {
                // flatMap subscribes to inner publishers EAGERLY
                // Elements may arrive in different order (interleaving)
                fun process(n: Int): Mono<String> = Mono.just("Processed: $n")

                val flux = Flux.just(1, 2, 3)
                    .flatMap { process(it) }

                StepVerifier.create(flux)
                    .expectNextCount(3)
                    .verifyComplete()
            }

            it("flatMap() with concurrency limit") {
                // You can control how many inner publishers are subscribed at once
                val processedOrder = mutableListOf<Int>()

                val flux = Flux.range(1, 10)
                    .flatMap(
                        { n ->
                            Mono.fromCallable {
                                processedOrder.add(n)
                                n
                            }
                        },
                        2, // Max 2 concurrent subscriptions
                    )

                flux.blockLast()
                processedOrder.size shouldBe 10
            }

            it("concatMap() - async transformation, preserves order") {
                // concatMap subscribes to inner publishers SEQUENTIALLY
                // Order is preserved but slower (no parallelism)
                val results = mutableListOf<String>()

                val flux = Flux.just(1, 2, 3)
                    .concatMap { n ->
                        Mono.fromCallable {
                            "Processed: $n".also { results.add(it) }
                        }
                    }

                flux.blockLast()

                // Order is guaranteed
                results shouldBe listOf("Processed: 1", "Processed: 2", "Processed: 3")
            }

            it("flatMapSequential() - async with order preservation") {
                // flatMapSequential subscribes eagerly but queues results
                // to preserve order - best of both worlds!
                val flux = Flux.just(1, 2, 3)
                    .flatMapSequential { n ->
                        Mono.just("Item: $n")
                            .delayElement(Duration.ofMillis((100 - n * 20).toLong()))
                    }

                StepVerifier.create(flux)
                    .expectNext("Item: 1", "Item: 2", "Item: 3")
                    .verifyComplete()
            }

            it("flatMapIterable() - one to many transformation") {
                val flux = Flux.just("Hello World", "Foo Bar")
                    .flatMapIterable { it.split(" ") }

                StepVerifier.create(flux)
                    .expectNext("Hello", "World", "Foo", "Bar")
                    .verifyComplete()
            }

            it("handle() - combined map and filter") {
                // handle() lets you transform AND/OR filter in one operation
                val flux = Flux.range(1, 10)
                    .handle<String> { value, sink ->
                        when {
                            value % 2 == 0 -> sink.next("Even: $value")
                            value > 7 -> sink.next("Large odd: $value")
                            // Others are filtered out
                        }
                    }

                StepVerifier.create(flux)
                    .expectNext("Even: 2", "Even: 4", "Even: 6", "Even: 8", "Large odd: 9", "Even: 10")
                    .verifyComplete()
            }
        }

        describe("Filtering and slicing") {

            it("filter() - keep elements matching predicate") {
                val flux = Flux.range(1, 10)
                    .filter { it % 2 == 0 }

                StepVerifier.create(flux)
                    .expectNext(2, 4, 6, 8, 10)
                    .verifyComplete()
            }

            it("take() - take first N elements") {
                val flux = Flux.range(1, 100)
                    .take(3)

                StepVerifier.create(flux)
                    .expectNext(1, 2, 3)
                    .verifyComplete()
            }

            it("takeLast() - take last N elements") {
                val flux = Flux.range(1, 10)
                    .takeLast(3)

                StepVerifier.create(flux)
                    .expectNext(8, 9, 10)
                    .verifyComplete()
            }

            it("takeWhile() - take while condition is true") {
                val flux = Flux.range(1, 10)
                    .takeWhile { it < 5 }

                StepVerifier.create(flux)
                    .expectNext(1, 2, 3, 4)
                    .verifyComplete()
            }

            it("takeUntil() - take until condition becomes true") {
                val flux = Flux.range(1, 10)
                    .takeUntil { it >= 5 }

                StepVerifier.create(flux)
                    .expectNext(1, 2, 3, 4, 5) // Includes the element that matched
                    .verifyComplete()
            }

            it("skip() - skip first N elements") {
                val flux = Flux.range(1, 10)
                    .skip(7)

                StepVerifier.create(flux)
                    .expectNext(8, 9, 10)
                    .verifyComplete()
            }

            it("skipWhile() - skip while condition is true") {
                val flux = Flux.range(1, 10)
                    .skipWhile { it < 5 }

                StepVerifier.create(flux)
                    .expectNext(5, 6, 7, 8, 9, 10)
                    .verifyComplete()
            }

            it("distinct() - removes duplicates") {
                val flux = Flux.just(1, 2, 1, 3, 2, 4, 3)
                    .distinct()

                StepVerifier.create(flux)
                    .expectNext(1, 2, 3, 4)
                    .verifyComplete()
            }

            it("distinctUntilChanged() - removes consecutive duplicates") {
                val flux = Flux.just(1, 1, 2, 2, 2, 1, 3, 3)
                    .distinctUntilChanged()

                StepVerifier.create(flux)
                    .expectNext(1, 2, 1, 3)
                    .verifyComplete()
            }

            it("elementAt() - get element at specific index") {
                val mono = Flux.range(1, 10)
                    .elementAt(3) // 0-based index

                StepVerifier.create(mono)
                    .expectNext(4)
                    .verifyComplete()
            }

            it("next() - get first element as Mono") {
                val mono = Flux.just("A", "B", "C")
                    .next()

                StepVerifier.create(mono)
                    .expectNext("A")
                    .verifyComplete()
            }

            it("last() - get last element as Mono") {
                val mono = Flux.just("A", "B", "C")
                    .last()

                StepVerifier.create(mono)
                    .expectNext("C")
                    .verifyComplete()
            }
        }

        describe("Aggregation and collection") {

            it("count() - count elements") {
                val mono = Flux.range(1, 100).count()

                StepVerifier.create(mono)
                    .expectNext(100L)
                    .verifyComplete()
            }

            it("reduce() - reduce to single value") {
                val mono = Flux.range(1, 5)
                    .reduce { acc, next -> acc + next }

                StepVerifier.create(mono)
                    .expectNext(15) // 1+2+3+4+5
                    .verifyComplete()
            }

            it("reduce() with initial value") {
                val mono = Flux.range(1, 5)
                    .reduce(100) { acc, next -> acc + next }

                StepVerifier.create(mono)
                    .expectNext(115) // 100+1+2+3+4+5
                    .verifyComplete()
            }

            it("scan() - running accumulation (emits intermediate results)") {
                val flux = Flux.range(1, 5)
                    .scan { acc, next -> acc + next }

                StepVerifier.create(flux)
                    .expectNext(1, 3, 6, 10, 15) // Running sum
                    .verifyComplete()
            }

            it("collectList() - collect to List") {
                val mono = Flux.just("A", "B", "C")
                    .collectList()

                StepVerifier.create(mono)
                    .expectNext(listOf("A", "B", "C"))
                    .verifyComplete()
            }

            it("collectMap() - collect to Map") {
                data class User(val id: Int, val name: String)

                val mono = Flux.just(
                    User(1, "Alice"),
                    User(2, "Bob"),
                ).collectMap({ it.id }, { it.name })

                StepVerifier.create(mono)
                    .expectNext(mapOf(1 to "Alice", 2 to "Bob"))
                    .verifyComplete()
            }

            it("collectMultimap() - collect to Map with list values") {
                data class Item(val category: String, val name: String)

                val mono = Flux.just(
                    Item("fruit", "apple"),
                    Item("fruit", "banana"),
                    Item("vegetable", "carrot"),
                ).collectMultimap({ it.category }, { it.name })

                StepVerifier.create(mono)
                    .assertNext { map ->
                        map["fruit"] shouldBe listOf("apple", "banana")
                        map["vegetable"] shouldBe listOf("carrot")
                    }
                    .verifyComplete()
            }

            it("all() - check if all elements match predicate") {
                val allPositive = Flux.just(1, 2, 3, 4, 5).all { it > 0 }
                val allEven = Flux.just(2, 4, 6, 7).all { it % 2 == 0 }

                StepVerifier.create(allPositive)
                    .expectNext(true)
                    .verifyComplete()

                StepVerifier.create(allEven)
                    .expectNext(false)
                    .verifyComplete()
            }

            it("any() - check if any element matches predicate") {
                val anyNegative = Flux.just(1, 2, -3, 4).any { it < 0 }
                val anyLarge = Flux.just(1, 2, 3).any { it > 100 }

                StepVerifier.create(anyNegative)
                    .expectNext(true)
                    .verifyComplete()

                StepVerifier.create(anyLarge)
                    .expectNext(false)
                    .verifyComplete()
            }

            it("hasElements() - check if Flux has any elements") {
                val hasElements = Flux.just(1, 2, 3).hasElements()
                val isEmpty = Flux.empty<Int>().hasElements()

                StepVerifier.create(hasElements)
                    .expectNext(true)
                    .verifyComplete()

                StepVerifier.create(isEmpty)
                    .expectNext(false)
                    .verifyComplete()
            }
        }

        describe("Combining Flux streams") {

            it("concat() - sequential combination (one after another)") {
                val flux1 = Flux.just(1, 2, 3)
                val flux2 = Flux.just(4, 5, 6)

                val combined = Flux.concat(flux1, flux2)

                StepVerifier.create(combined)
                    .expectNext(1, 2, 3, 4, 5, 6)
                    .verifyComplete()
            }

            it("merge() - interleaved combination (as elements arrive)") {
                // merge() subscribes to all sources eagerly
                // Elements arrive as they're emitted (may interleave)
                val flux1 = Flux.just(1, 2, 3).delayElements(Duration.ofMillis(50))
                val flux2 = Flux.just(10, 20, 30).delayElements(Duration.ofMillis(50))

                val combined = Flux.merge(flux1, flux2)

                StepVerifier.create(combined)
                    .expectNextCount(6)
                    .verifyComplete()
            }

            it("zip() - combines elements pairwise") {
                val flux1 = Flux.just("A", "B", "C")
                val flux2 = Flux.just(1, 2, 3)

                val zipped = Flux.zip(flux1, flux2) { letter, number -> "$letter$number" }

                StepVerifier.create(zipped)
                    .expectNext("A1", "B2", "C3")
                    .verifyComplete()
            }

            it("zip() stops when shortest Flux completes") {
                val flux1 = Flux.just("A", "B", "C", "D", "E")
                val flux2 = Flux.just(1, 2, 3)

                val zipped = Flux.zip(flux1, flux2) { letter, number -> "$letter$number" }

                StepVerifier.create(zipped)
                    .expectNext("A1", "B2", "C3")
                    .verifyComplete()
            }

            it("combineLatest() - combines latest values from each source") {
                // combineLatest emits when ANY source emits,
                // combining with latest from others
                val flux1 = Flux.just("A", "B").delayElements(Duration.ofMillis(100))
                val flux2 = Flux.just(1, 2, 3).delayElements(Duration.ofMillis(150))

                val combined = Flux.combineLatest(flux1, flux2) { letter, number -> "$letter$number" }

                StepVerifier.create(combined)
                    .expectNextCount(4) // A1, B1, B2, B3 or similar
                    .verifyComplete()
            }

            it("firstWithSignal() - use first Flux to emit") {
                val slow = Flux.just("slow").delayElements(Duration.ofSeconds(1))
                val fast = Flux.just("fast").delayElements(Duration.ofMillis(10))

                val first = Flux.firstWithSignal(slow, fast)

                StepVerifier.create(first)
                    .expectNext("fast")
                    .verifyComplete()
            }

            it("switchOnNext() - switches to latest Flux") {
                // Each time a new Flux is emitted, switch to it
                // and cancel the previous one
                val fluxOfFluxes = Flux.just(
                    Flux.just(1, 2, 3),
                    Flux.just(10, 20, 30),
                )

                val switched = Flux.switchOnNext(fluxOfFluxes)

                StepVerifier.create(switched)
                    .expectNextCount(6)
                    .verifyComplete()
            }
        }

        describe("Grouping and windowing") {

            it("groupBy() - groups elements by key") {
                val flux = Flux.just(1, 2, 3, 4, 5, 6)
                    .groupBy { if (it % 2 == 0) "even" else "odd" }
                    .flatMap { group ->
                        group.collectList().map { group.key() to it }
                    }

                StepVerifier.create(flux)
                    .expectNextMatches { (key, values) ->
                        (key == "odd" && values == listOf(1, 3, 5)) ||
                            (key == "even" && values == listOf(2, 4, 6))
                    }
                    .expectNextMatches { (key, values) ->
                        (key == "odd" && values == listOf(1, 3, 5)) ||
                            (key == "even" && values == listOf(2, 4, 6))
                    }
                    .verifyComplete()
            }

            it("buffer() - collects into batches") {
                val flux = Flux.range(1, 10)
                    .buffer(3) // Collect 3 elements at a time

                StepVerifier.create(flux)
                    .expectNext(listOf(1, 2, 3))
                    .expectNext(listOf(4, 5, 6))
                    .expectNext(listOf(7, 8, 9))
                    .expectNext(listOf(10))
                    .verifyComplete()
            }

            it("window() - splits into Flux windows") {
                val flux = Flux.range(1, 6)
                    .window(2)
                    .flatMap { it.collectList() }

                StepVerifier.create(flux)
                    .expectNext(listOf(1, 2))
                    .expectNext(listOf(3, 4))
                    .expectNext(listOf(5, 6))
                    .verifyComplete()
            }
        }

        describe("Error handling in Flux") {

            it("errors stop the stream") {
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

            it("onErrorReturn() - emit fallback value on error") {
                val flux = Flux.just(1, 2, 3)
                    .map {
                        if (it == 2) throw RuntimeException()
                        it
                    }
                    .onErrorReturn(-1)

                StepVerifier.create(flux)
                    .expectNext(1, -1) // Error replaced with -1, then completes
                    .verifyComplete()
            }

            it("onErrorResume() - switch to fallback Flux on error") {
                val flux = Flux.just(1, 2, 3)
                    .map {
                        if (it == 2) throw RuntimeException()
                        it
                    }
                    .onErrorResume { Flux.just(100, 200, 300) }

                StepVerifier.create(flux)
                    .expectNext(1, 100, 200, 300)
                    .verifyComplete()
            }

            it("onErrorContinue() - skip failed elements and continue") {
                // WARNING: onErrorContinue has complex semantics
                // Not all operators support it!
                val flux = Flux.just(1, 2, 3, 4, 5)
                    .map {
                        if (it == 3) throw RuntimeException("Skip 3")
                        it * 10
                    }
                    .onErrorContinue { _, value ->
                        println("Skipped: $value")
                    }

                StepVerifier.create(flux)
                    .expectNext(10, 20, 40, 50) // 3 is skipped
                    .verifyComplete()
            }
        }

        describe("Flux is lazy and cold") {

            it("nothing happens until subscription") {
                val log = mutableListOf<String>()

                val flux = Flux.create<Int> { sink ->
                    log.add("Creating flux")
                    sink.next(1)
                    sink.next(2)
                    sink.complete()
                }

                log.size shouldBe 0 // Not subscribed yet

                flux.blockFirst()

                log shouldBe listOf("Creating flux")
            }

            it("each subscription creates new stream (cold)") {
                val counter = AtomicInteger(0)

                val flux = Flux.fromIterable(1..3)
                    .doOnNext { counter.incrementAndGet() }

                flux.blockLast()
                flux.blockLast()

                counter.get() shouldBe 6 // 3 elements x 2 subscriptions
            }

            it("share() - converts cold to hot (shared)") {
                val counter = AtomicInteger(0)

                val sharedFlux = Flux.fromIterable(1..3)
                    .doOnSubscribe { counter.incrementAndGet() }
                    .share()

                // Multiple subscriptions share the same source
                sharedFlux.subscribe()
                sharedFlux.subscribe()

                // Note: share() behavior depends on timing
                // For demo purposes only
            }
        }
    })
