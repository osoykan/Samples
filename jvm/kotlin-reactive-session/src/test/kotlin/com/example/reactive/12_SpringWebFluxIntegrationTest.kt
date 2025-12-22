package com.example.reactive

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import org.springframework.web.reactive.function.client.bodyToFlow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

/**
 * # Module 12: Spring WebFlux Integration
 *
 * Spring WebFlux provides first-class support for both
 * Project Reactor and Kotlin Coroutines.
 *
 * Key concepts:
 * - Reactive controllers (Mono/Flux vs suspend/Flow)
 * - WebClient with coroutines
 * - Repository patterns
 */
class SpringWebFluxIntegrationTest :
    DescribeSpec({

        describe("Controller return types") {

            it("Returning Mono from controller (Java-style)") {
                // @GetMapping("/user/{id}")
                // fun getUser(@PathVariable id: String): Mono<User>

                // Simulating controller logic
                fun getUser(id: String): Mono<User> = Mono.just(User(id, "John Doe"))

                StepVerifier.create(getUser("123"))
                    .expectNext(User("123", "John Doe"))
                    .verifyComplete()
            }

            it("Returning Flux from controller (streaming)") {
                // @GetMapping("/users", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
                // fun getAllUsers(): Flux<User>

                fun getAllUsers(): Flux<User> = Flux.just(
                    User("1", "Alice"),
                    User("2", "Bob"),
                    User("3", "Charlie"),
                )

                StepVerifier.create(getAllUsers())
                    .expectNextCount(3)
                    .verifyComplete()
            }

            it("Returning suspend function result (Kotlin-style)") {
                // @GetMapping("/user/{id}")
                // suspend fun getUser(@PathVariable id: String): User

                suspend fun getUser(id: String): User {
                    delay(10) // Simulate async operation
                    return User(id, "John Doe")
                }

                val user = getUser("123")
                user shouldBe User("123", "John Doe")
            }

            it("Returning Flow from controller (Kotlin streaming)") {
                // @GetMapping("/users", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
                // fun getAllUsers(): Flow<User>

                fun getAllUsers(): Flow<User> = flow {
                    emit(User("1", "Alice"))
                    delay(10)
                    emit(User("2", "Bob"))
                    delay(10)
                    emit(User("3", "Charlie"))
                }

                val users = getAllUsers().toList()
                users.size shouldBe 3
            }

            it("Nullable suspend function return") {
                // @GetMapping("/user/{id}")
                // suspend fun findUser(@PathVariable id: String): User?

                suspend fun findUser(id: String): User? {
                    delay(10)
                    return if (id == "exists") User(id, "Found") else null
                }

                findUser("exists") shouldBe User("exists", "Found")
                findUser("not-exists") shouldBe null
            }
        }

        describe("Service layer patterns") {

            it("Reactor-based service") {
                class UserService {
                    private val users = mapOf(
                        "1" to User("1", "Alice"),
                        "2" to User("2", "Bob"),
                    )

                    fun findById(id: String): Mono<User> = Mono.justOrEmpty(users[id])

                    fun findAll(): Flux<User> = Flux.fromIterable(users.values)

                    fun save(user: User): Mono<User> = Mono.just(user)
                }

                val service = UserService()

                StepVerifier.create(service.findById("1"))
                    .expectNext(User("1", "Alice"))
                    .verifyComplete()

                StepVerifier.create(service.findById("999"))
                    .verifyComplete() // Empty

                StepVerifier.create(service.findAll())
                    .expectNextCount(2)
                    .verifyComplete()
            }

            it("Coroutine-based service") {
                class UserService {
                    private val users = mapOf(
                        "1" to User("1", "Alice"),
                        "2" to User("2", "Bob"),
                    )

                    suspend fun findById(id: String): User? {
                        delay(10) // Simulate DB access
                        return users[id]
                    }

                    fun findAll(): Flow<User> = flow {
                        users.values.forEach {
                            delay(5)
                            emit(it)
                        }
                    }

                    suspend fun save(user: User): User {
                        delay(10)
                        return user
                    }
                }

                val service = UserService()

                service.findById("1") shouldBe User("1", "Alice")
                service.findById("999") shouldBe null
                service.findAll().toList().size shouldBe 2
            }

            it("Mixed service: Reactor storage with suspend interface") {
                // Internal Reactor-based repository
                class ReactorUserRepository {
                    fun findById(id: String): Mono<User> = Mono.just(User(id, "User-$id"))

                    fun findAll(): Flux<User> = Flux.just(User("1", "A"), User("2", "B"))
                }

                // Coroutine-friendly service facade
                class UserService(private val repo: ReactorUserRepository) {
                    suspend fun findById(id: String): User? = repo.findById(id).awaitSingleOrNull()

                    suspend fun findAll(): List<User> = repo.findAll().asFlow().toList()
                }

                val service = UserService(ReactorUserRepository())

                service.findById("123")?.name shouldBe "User-123"
                service.findAll().size shouldBe 2
            }
        }

        describe("WebClient patterns") {

            // Note: These are simulation examples since we don't have a running server

            it("WebClient with Reactor (traditional)") {
                // Traditional Reactor style
                fun fetchUserReactor(client: WebClient, id: String): Mono<User> = client.get()
                    .uri("/users/{id}", id)
                    .retrieve()
                    .bodyToMono(User::class.java)

                // Can chain with other Reactor operators
                fun fetchUserWithFallback(client: WebClient, id: String): Mono<User> = fetchUserReactor(client, id)
                    .onErrorResume { Mono.just(User("0", "Unknown")) }
            }

            it("WebClient with coroutines") {
                // Coroutine style using awaitBody
                suspend fun fetchUser(client: WebClient, id: String): User = client.get()
                    .uri("/users/{id}", id)
                    .retrieve()
                    .awaitBody()

                // With nullable handling
                suspend fun fetchUserOrNull(client: WebClient, id: String): User? = client.get()
                    .uri("/users/{id}", id)
                    .retrieve()
                    .awaitBodyOrNull()
            }

            it("WebClient streaming with Flow") {
                // Get streaming response as Flow
                fun fetchUsersAsFlow(client: WebClient): Flow<User> = client.get()
                    .uri("/users")
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .bodyToFlow()

                // Usage in suspend function
                suspend fun processAllUsers(client: WebClient) {
                    fetchUsersAsFlow(client).collect { user ->
                        println("Processing: $user")
                    }
                }
            }

            it("Error handling with WebClient and coroutines") {
                suspend fun fetchUserSafe(client: WebClient, id: String): Result<User> = runCatching {
                    client.get()
                        .uri("/users/{id}", id)
                        .retrieve()
                        .awaitBody<User>()
                }

                // Or with try-catch
                suspend fun fetchUserWithFallback(client: WebClient, id: String): User = try {
                    client.get()
                        .uri("/users/{id}", id)
                        .retrieve()
                        .awaitBody()
                } catch (e: Exception) {
                    User("0", "Fallback User")
                }
            }
        }

        describe("Repository patterns") {

            it("Reactive repository pattern") {
                // Note: In real code, interfaces would be at class level
                // Here we use a concrete class for demonstration

                class InMemoryReactiveUserRepository {
                    private val users = mutableMapOf<String, User>()

                    fun findById(id: String): Mono<User> = Mono.justOrEmpty(users[id])

                    fun findAll(): Flux<User> = Flux.fromIterable(users.values)

                    fun save(user: User): Mono<User> {
                        users[user.id] = user
                        return Mono.just(user)
                    }

                    fun deleteById(id: String): Mono<Void> {
                        users.remove(id)
                        return Mono.empty()
                    }
                }

                val repo = InMemoryReactiveUserRepository()

                StepVerifier.create(repo.save(User("1", "Alice")))
                    .expectNext(User("1", "Alice"))
                    .verifyComplete()

                StepVerifier.create(repo.findById("1"))
                    .expectNext(User("1", "Alice"))
                    .verifyComplete()
            }

            it("Coroutine repository pattern") {
                class InMemoryCoroutineUserRepository {
                    private val users = mutableMapOf<String, User>()

                    suspend fun findById(id: String): User? {
                        delay(1) // Simulate async
                        return users[id]
                    }

                    fun findAll(): Flow<User> = flow {
                        users.values.forEach { emit(it) }
                    }

                    suspend fun save(user: User): User {
                        delay(1)
                        users[user.id] = user
                        return user
                    }

                    suspend fun deleteById(id: String) {
                        delay(1)
                        users.remove(id)
                    }
                }

                val repo = InMemoryCoroutineUserRepository()

                repo.save(User("1", "Alice"))
                repo.findById("1") shouldBe User("1", "Alice")
                repo.findAll().toList().size shouldBe 1
            }
        }

        describe("Controller exception handling") {

            it("Error handling in Reactor controller") {
                // @ExceptionHandler or onErrorResume in controller

                fun getUser(id: String): Mono<User> = Mono.defer {
                    if (id == "invalid") {
                        Mono.error(IllegalArgumentException("Invalid ID"))
                    } else {
                        Mono.just(User(id, "Name"))
                    }
                }.onErrorResume(IllegalArgumentException::class.java) {
                    Mono.just(User("0", "Default"))
                }

                StepVerifier.create(getUser("valid"))
                    .expectNext(User("valid", "Name"))
                    .verifyComplete()

                StepVerifier.create(getUser("invalid"))
                    .expectNext(User("0", "Default"))
                    .verifyComplete()
            }

            it("Error handling in suspend controller") {
                // Exceptions can be handled with try-catch or @ExceptionHandler

                suspend fun getUser(id: String): User {
                    if (id == "invalid") {
                        throw IllegalArgumentException("Invalid ID")
                    }
                    delay(10)
                    return User(id, "Name")
                }

                suspend fun getUserSafe(id: String): User = try {
                    getUser(id)
                } catch (e: IllegalArgumentException) {
                    User("0", "Default")
                }

                getUserSafe("valid") shouldBe User("valid", "Name")
                getUserSafe("invalid") shouldBe User("0", "Default")
            }
        }

        describe("Best practices for Spring WebFlux") {

            it("Prefer suspend functions for simple operations") {
                // Simple CRUD - use suspend
                suspend fun createUser(user: User): User {
                    delay(10)
                    return user
                }

                // Flow for streaming
                fun streamEvents(): Flow<String> = flow {
                    repeat(10) {
                        delay(100)
                        emit("Event $it")
                    }
                }
            }

            it("Use mono {} when integrating with Reactor dependencies") {
                // When you have a Reactor-based library
                class ReactorExternalService {
                    fun call(): Mono<String> = Mono.just("external")
                }

                suspend fun useExternalService(service: ReactorExternalService): String = service.call().awaitSingle()

                // Or expose coroutine service as Reactor API
                class MyService {
                    suspend fun process(): String {
                        delay(10)
                        return "result"
                    }
                }

                fun getAsReactor(service: MyService): Mono<String> = mono {
                    service.process()
                }
            }

            it("Handle empty results properly") {
                // For nullable return, Mono returns 404 by default for empty

                suspend fun findUser(id: String): User? = if (id == "exists") User(id, "Found") else null

                // Controller handles null appropriately
                // Spring will return 404 for null from suspend function
            }
        }
    }) {
    data class User(val id: String, val name: String)
}
