package castor
import java.util.concurrent.{Executors, ThreadFactory, TimeUnit}

import scala.concurrent.{Await, ExecutionContext, Future, Promise}

/**
 * An extended `scala.concurrent.ExecutionContext`; provides the ability to
 * schedule messages to be sent later, and hooks to track the current number of
 * outstanding tasks or log the actor message sends for debugging purposes
 */
trait Context extends ExecutionContext {
  def reportSchedule(): Context.Token = new Context.Token.Simple()

  def reportSchedule(fileName: sourcecode.FileName,
                     line: sourcecode.Line): Context.Token = {
    new Context.Token.Future(fileName, line)
  }

  def reportSchedule(a: Actor[_],
                     msg: Any,
                     fileName: sourcecode.FileName,
                     line: sourcecode.Line): Context.Token = {
    new Context.Token.Send(a, msg, fileName, line)
  }

  def reportRun(a: Actor[_],
                msg: Any,
                token: Context.Token): Unit = ()

  def reportComplete(token: Context.Token): Unit = ()

  def scheduleMsg[T](a: Actor[T], msg: T, time: java.time.Duration)
                    (implicit fileName: sourcecode.FileName,
                     line: sourcecode.Line): Unit

  def future[T](t: => T)
               (implicit fileName: sourcecode.FileName,
                line: sourcecode.Line): Future[T]

  def execute(runnable: Runnable): Unit
}

object Context{
  trait Token
  object Token{
    class Simple extends Token(){
      override def toString = "token@" + Integer.toHexString(hashCode())
    }

    class Future(val fileName: sourcecode.FileName,
                 val line: sourcecode.Line) extends Token(){
      override def toString =
        "token@" + Integer.toHexString(hashCode()) + "@" +
        fileName.value + ":" + line.value
    }

    class Send(val a: Actor[_],
               val msg: Any,
               val fileName: sourcecode.FileName,
               val line: sourcecode.Line) extends Token(){
      override def toString =
        "token@" + Integer.toHexString(hashCode()) + "@" +
        fileName.value + ":" + line.value
    }
  }

  /**
   * Castor actor context based on a Scala ExecutionContext
   */
  class Simple(ec: ExecutionContext, logEx: Throwable => Unit) extends Context.Impl {
    def executionContext = ec
    def reportFailure(t: Throwable) = logEx(t)
  }

  object Simple{
    lazy val threadPool = makeThreadPool(Runtime.getRuntime().availableProcessors(), true)
    lazy val executionContext = ExecutionContext.fromExecutorService(threadPool)
    implicit lazy val global: Simple = new Simple(executionContext, _.printStackTrace())
  }
  def makeThreadPool(numThreads: Int, daemon: Boolean) = Executors.newFixedThreadPool(
    numThreads,
    new ThreadFactory {
      override def newThread(r: Runnable) = {
        val t = new Thread(r)
        t.setDaemon(daemon)
        t
      }
    }
  )

  /**
   * Castor actor context based on a fixed thread pool
   */
  class ThreadPool(numThreads: Int = Runtime.getRuntime().availableProcessors(),
                   daemon: Boolean = true,
                   logEx: Throwable => Unit = _.printStackTrace()) extends Context.Impl{
    val threadPool = makeThreadPool(numThreads, daemon)

    val executionContext = ExecutionContext.fromExecutorService(threadPool)

    def reportFailure(cause: Throwable): Unit = logEx(cause)
    def shutdown() = threadPool.shutdownNow()
  }

  /**
   * [[castor.Context.Simple]] used for testing; tracks scheduling and completion of
   * tasks and futures, so we can support a `.waitForInactivity()` method to wait
   * until the system is quiescient
   */
  class Test(executionContext: ExecutionContext = Simple.executionContext,
             logEx: Throwable => Unit = _.printStackTrace())
    extends Simple(executionContext, logEx) with TestBase

  /**
   * [[castor.Context.ThreadPool]] used for testing; tracks scheduling and completion of
   * tasks and futures, so we can support a `.waitForInactivity()` method to wait
   * until the system is quiescient
   */
  class TestThreadPool(numThreads: Int = Runtime.getRuntime().availableProcessors(),
                       daemon: Boolean = true,
                       logEx: Throwable => Unit = _.printStackTrace())
    extends ThreadPool(numThreads, daemon, logEx) with TestBase

  trait TestBase extends Context.Impl {
    def executionContext: ExecutionContext
    private[this] val active = collection.mutable.Set.empty[Context.Token]
    private[this] var promise = concurrent.Promise.successful[Unit](())


    def handleReportSchedule(token: Context.Token) = synchronized{
      if (active.isEmpty) {
        assert(promise.isCompleted)
        promise = concurrent.Promise[Unit]
      }
      active.add(token)
      token
    }
    override def reportSchedule() = {
      handleReportSchedule(super.reportSchedule())
    }
    override def reportSchedule(fileName: sourcecode.FileName,
                                line: sourcecode.Line): Context.Token = {
      handleReportSchedule(super.reportSchedule(fileName, line))
    }

    override def reportSchedule(a: Actor[_],
                                msg: Any,
                                fileName: sourcecode.FileName,
                                line: sourcecode.Line): Context.Token = {
      handleReportSchedule(super.reportSchedule(a, msg, fileName, line))
    }

    override def reportComplete(token: Context.Token) = this.synchronized{
      assert(active.remove(token))

      if (active.isEmpty) promise.success(())
    }

    def waitForInactivity(timeout: Option[java.time.Duration] = None) = {
      Await.result(
        this.synchronized(promise).future,
        timeout match{
          case None => scala.concurrent.duration.Duration.Inf
          case Some(t) => scala.concurrent.duration.Duration.fromNanos(t.toNanos)
        }
      )
    }
  }

  trait Impl extends Context {
    def executionContext: ExecutionContext

    def execute(runnable: Runnable): Unit = {
      val token = reportSchedule()
      executionContext.execute(new Runnable {
        def run(): Unit = {
          try runnable.run()
          finally reportComplete(token)
        }
      })
    }

    def future[T](t: => T)
                 (implicit fileName: sourcecode.FileName,
                  line: sourcecode.Line): Future[T] = {
      val token = reportSchedule(fileName, line)
      val p = Promise[T]
      executionContext.execute(new Runnable {
        def run(): Unit = {
          p.complete(scala.util.Try(t))
          reportComplete(token)
        }
      })
      p.future
    }

    @deprecated("Do not depend on scheduler directly. Use scheduleMsg instead")
    lazy val scheduler = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactory {
        def newThread(r: Runnable): Thread = {
          val t = new Thread(r, "ActorContext-Scheduler-Thread")
          t.setDaemon(true)
          t
        }
      }
    )

    def scheduleMsg[T](a: Actor[T],
                       msg: T, delay: java.time.Duration)
                      (implicit fileName: sourcecode.FileName,
                       line: sourcecode.Line) = {
      val token = reportSchedule(a, msg, fileName, line)
      scheduler.schedule[Unit](
        new java.util.concurrent.Callable[Unit] {
          def call(): Unit = {
            a.send(msg)(fileName, line)
            reportComplete(token)
          }
        },
        delay.toMillis,
        TimeUnit.MILLISECONDS
      )
    }
  }

}
