package castor

trait Actor[T]{
  def send(t: T)
          (implicit fileName: sourcecode.FileName,
           line: sourcecode.Line): Unit

  def sendAsync(f: scala.concurrent.Future[T])
               (implicit fileName: sourcecode.FileName,
                line: sourcecode.Line): Unit
}
