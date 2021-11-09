package castor.platform

import castor._
import scala.scalajs.js.timers.setTimeout

private[castor] trait ContextImpl extends Context { this: Context.Impl =>
  def scheduleMsg[T](
      a: Actor[T],
      msg: T,
      delay: scala.concurrent.duration.Duration
  )(implicit fileName: sourcecode.FileName, line: sourcecode.Line): Unit = {
    val token = reportSchedule(a, msg, fileName, line)
    setTimeout(delay.toMillis.toDouble) {
      a.send(msg)(fileName, line)
      reportComplete(token)
    }
  }
}
