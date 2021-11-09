package castor.platform

import castor._

private [castor] trait ContextImpl extends Context { this: Context.Impl =>
  def scheduleMsg[T](
      a: Actor[T],
      msg: T,
      delay: scala.concurrent.duration.Duration
  )(implicit fileName: sourcecode.FileName, line: sourcecode.Line): Unit = {
    throw new NotImplementedError("scheduleMsg currently not available in native")
  }
}

