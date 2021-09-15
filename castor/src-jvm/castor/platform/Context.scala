package castor.platform

import castor._

private [castor] trait Context {
  def scheduleMsg[T](a: Actor[T], msg: T, time: java.time.Duration)
                    (implicit fileName: sourcecode.FileName,
                    line: sourcecode.Line): Unit
}
