package castor.platform

import castor._

import scala.concurrent.ExecutionContext

import java.util.concurrent.{Executors, ThreadFactory}

private [castor] trait ContextCompanionObject {
  /**
   * Castor actor context based on a fixed thread pool
   */
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
  class ThreadPool(numThreads: Int = Runtime.getRuntime().availableProcessors(),
                    daemon: Boolean = true,
                    logEx: Throwable => Unit = _.printStackTrace()) extends Context.Impl{
    val threadPool = makeThreadPool(numThreads, daemon)

    val executionContext = ExecutionContext.fromExecutorService(threadPool)

    def reportFailure(cause: Throwable): Unit = logEx(cause)
    def shutdown() = threadPool.shutdownNow()
  }

  /**
   * [[castor.Context.ThreadPool]] used for testing; tracks scheduling and completion of
   * tasks and futures, so we can support a `.waitForInactivity()` method to wait
   * until the system is quiescient
   */
  class TestThreadPool(numThreads: Int = Runtime.getRuntime().availableProcessors(),
                        daemon: Boolean = true,
                        logEx: Throwable => Unit = _.printStackTrace())
    extends ThreadPool(numThreads, daemon, logEx) with Context.TestBase

  trait ContextSimpleCompanionObject {
    lazy val threadPool = makeThreadPool(Runtime.getRuntime().availableProcessors(), true)
  }
}
