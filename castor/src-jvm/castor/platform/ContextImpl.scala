package castor.platform

import java.util.concurrent.{Executors, ThreadFactory, TimeUnit}

import castor._

private [castor] trait ContextImpl extends Context { this: Context.Impl =>
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
