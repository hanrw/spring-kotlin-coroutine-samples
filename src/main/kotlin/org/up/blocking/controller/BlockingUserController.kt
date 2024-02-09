package org.up.blocking.controller

import jakarta.transaction.Transactional
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import org.jetbrains.annotations.BlockingExecutor
import org.springframework.web.bind.annotation.*
import org.up.blocking.model.UserDto
import org.up.blocking.model.UserJpa
import org.up.blocking.repository.BlockingAvatarService
import org.up.blocking.repository.BlockingEnrollmentService
import org.up.blocking.repository.BlockingUserDao
import org.up.utils.toNullable
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@RestController
class BlockingUserController(
    private val blockingUserDao: BlockingUserDao,
    private val blockingAvatarService: BlockingAvatarService,
    private val blockingEnrollmentService: BlockingEnrollmentService,
) {
    @GetMapping("/blocking/users/{user-id}")
    @ResponseBody
    fun getUser(
        @PathVariable("user-id") id: Long = 0,
    ): UserJpa? = blockingUserDao.findById(id).toNullable()

    @GetMapping("/blocking/users")
    @ResponseBody
    fun getUsers(): List<UserJpa> = blockingUserDao.findAll()

    @PostMapping("/blocking/users")
    @ResponseBody
    @Transactional
    fun storeUser(
        @RequestBody user: UserJpa,
        @RequestParam(required = false) delay: Long? = null,
    ): UserJpa {
        val emailVerified = blockingEnrollmentService.verifyEmail(user.email, delay)
        val avatarUrl = user.avatarUrl ?: blockingAvatarService.randomAvatar(delay).url
        return blockingUserDao.save(user.copy(avatarUrl = avatarUrl, emailVerified = emailVerified))
    }


    /**
     * This is wrong case to use spring mvc combined with kotlin coroutines
     * This is just for demonstration purpose
     *  async will park the thread
     *  virtual thread execute at this block - async
     */
    @PostMapping("/neverDoThisWay/1/blocking/users")
    @ResponseBody
    @Transactional
    fun wrongWayStoreUser(
        @RequestBody user: UserJpa,
        @RequestParam(required = false) delay: Long? = null,
    ): UserDto {
        logger.info("Start store user")

        return runBlocking {
            // here will park the couroutine thread. mounting the thread to heap
            // avatarUrl and emailVerified will be executed in the same thread
            // total time will be avatarUrl + emailVerified >= 400ms
            val avatarUrl = async {
                user.avatarUrl ?: blockingAvatarService.randomAvatar(delay).url.also {
                    logger.info("fetch random avatar...")
                }
            }

            val emailVerified = async {
                blockingEnrollmentService.verifyEmail(user.email, delay).also {
                    logger.info("verify email ${user.email}...")
                }
            }
            blockingUserDao.save(user.copy(avatarUrl = avatarUrl.await(), emailVerified = emailVerified.await()))
                .toDto()
        }
    }

    @PostMapping("/neverDoThisWay/2/blocking/users")
    @ResponseBody
    @Transactional
    fun wrongWay2StoreUser(
        @RequestBody user: UserJpa,
        @RequestParam(required = false) delay: Long? = null,
    ): UserDto {
        logger.info("Start store user")

        // this case use Dispatchers.VT as coroutine dispatcher. and the thread context, mdc will be lost
        // which means that the transactional context will be lost as well
        return runBlocking(Dispatchers.VT) {
            // here will park the couroutine thread. mounting the thread to heap
            // avatarUrl and emailVerified will be executed in the same thread
            // total time will be avatarUrl + emailVerified >= 400ms
            val avatarUrl = async {
                user.avatarUrl ?: blockingAvatarService.randomAvatar(delay).url.also {
                    logger.info("fetch random avatar...")
                }
            }

            val emailVerified = async {
                blockingEnrollmentService.verifyEmail(user.email, delay).also {
                    logger.info("verify email ${user.email}...")
                }
            }
            blockingUserDao.save(user.copy(avatarUrl = avatarUrl.await(), emailVerified = emailVerified.await()))
                .toDto()
        }
    }

    @PostMapping("/neverDoThisWay/3/blocking/users")
    @ResponseBody
    @Transactional
    fun wrongWay3StoreUser(
        @RequestBody user: UserJpa,
        @RequestParam(required = false) delay: Long? = null,
    ): UserDto {
        logger.info("Start store user")

        // this case use Dispatchers.VT as coroutine dispatcher. and the thread context, mdc will be lost
        // which means that the transactional context will be lost as well
        return runBlocking(Dispatchers.VT + MDCContext()) {
            // here will park the couroutine thread. mounting the thread to heap
            // avatarUrl and emailVerified will be executed in the same thread
            // total time will be avatarUrl + emailVerified >= 400ms
            val avatarUrl = async {
                user.avatarUrl ?: blockingAvatarService.randomAvatar(delay).url.also {
                    logger.info("fetch random avatar...")
                }
            }

            val emailVerified = async {
                blockingEnrollmentService.verifyEmail(user.email, delay).also {
                    logger.info("verify email ${user.email}...")
                }
            }
            blockingUserDao.save(user.copy(avatarUrl = avatarUrl.await(), emailVerified = emailVerified.await()))
                .toDto()
        }
    }

    companion object {
        val logger = org.slf4j.LoggerFactory.getLogger(BlockingUserController::class.java)
    }
}

/** Executes the specified block on thread in an IO thread pool. */
class VTExecutor : Executor by Executors.newVirtualThreadPerTaskExecutor() {
    fun named(name: String): Executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(name).factory())
    operator fun invoke(name: String): Executor = Executor { Thread.ofVirtual().name(name).start(it) }
}

val Dispatchers.VT: @BlockingExecutor CoroutineDispatcher get() = VTExecutor().named("VT").asCoroutineDispatcher()
