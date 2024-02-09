### Relevant Articles:
- [Non-blocking Spring Boot with Kotlin Coroutines](http://www.baeldung.com/non-blocking-spring-boot-with-kotlin-coroutines)


- Talk setup
Question
- experience with Threads, CompleteableFutures, Reactor/RxJava

- tell a story:

- there was once upon a time a happy programmer
- sequential code

- multicore revolution broke out
- fiddled with threads: and failed
- fiddled with better concurrency abstraction: and kind of lived
- but secretly he remembered the good old times

- then Kotlin gave him Coroutines the ultimate weapon 
- the weapon was kind of invisible 

- he went to the battle field called production and exposed himself to a heavy request traffic salvo while his collegues were watching him from a save distance
- and he succeeded


What is a coroutine?
- image processes/threads/coroutines
-- run example

Key ingredients for coroutines
- Coroutine Builders
- suspend functions
- Coroutine Scope 
- Coroutine Context


##Examples
- Get User
```
 curl http://localhost:8080/users/1
```

- Insert User
```
curl -X POST -H "Content-Type: application/json" -d '{"id":null,"userName":"JackRabbit","email":"Jack@Rabbit.com","avatarUrl":null}'  http://localhost:8080/users/
```

- Sync avatar
```
curl -X PATCH  http://localhost:8080/users/1/sync-avatar
```


##Perf tests
See: https://github.com/parkghost/gohttpbench
To build:
```
 go build -v -o gob github.com/parkghost/gohttpbench
```
- Get Performance
```
gob  -c 100 -n 500 -k http://localhost:8080/blocking/users/3/sync-avatar

```

- Post Performance
```
 gob  -c 100 -n 500 -T "application/json" -p user.json  -k   http://localhost:8080/blocking/users/

```


https://stackoverflow.com/questions/35628764/completablefuture-vs-spring-transactions


# spring boot with coroutines combined with virtual threads
## The wrong way revisited
- Blocking IO - Spring mvc
  - BlockingUserController
  - case 1: virtual threads + coroutines
```shell
echo -n '{"id":null,"userName":"JackRabbit","email":"Jack@Rabbit.com","avatarUrl":null}' | http POST http://localhost:8080/neverDoThisWay/blocking/1/users\?delay\=200 --meta

Elapsed time: 0.436676875s

## da85b1b651 same thread

14:07:39.270 [tomcat-handler-12] da85b1b651 INFO  o.u.b.c.BlockingUserController - Start store user
14:07:39.475 [tomcat-handler-12] da85b1b651 INFO  o.u.b.c.BlockingUserController - fetch random avatar...
14:07:39.683 [tomcat-handler-12] da85b1b651 INFO  o.u.b.c.BlockingUserController - verify email Jack@Rabbit.com...
```


- case 2: virtual threads + coroutines with specific dispatcher(VirtualThreadDispatcher) - it's will lose the thread local context. e.g MDC, SecurityContextHolder, Transactional etc.
```shell
echo -n '{"id":null,"userName":"JackRabbit","email":"Jack@Rabbit.com","avatarUrl":null}' | http POST http://localhost:8080/neverDoThisWay/2/blocking/users\?delay\=200 --meta
Elapsed time: 0.225320292s
```

- case 3: virtual threads + coroutines with specific dispatcher(VirtualThreadDispatcher)
It's can not work as expected, but need add the context like: + MDCContext + TransactionalContext and so on.