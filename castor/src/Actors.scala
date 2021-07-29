package castor
import collection.mutable

abstract class BaseActor[T]()(implicit ac: Context) extends Actor[T]{
  private val queue = new mutable.Queue[(T, Context.Token)]()
  def softQueueLimit = ac.softQueueLimit
  implicit def self = this
  private var scheduled = false

  def scheduleRun() = ac.execute(new Runnable{def run(): Unit = runWithItems()})


  def send(t: T)
          (implicit fileName: sourcecode.FileName,
           line: sourcecode.Line,
           sender: BaseActor[_]): Unit = synchronized{
    val token = ac.reportSchedule(this, t, fileName, line)
    queue.enqueue((t, token))
    if (queue.length > softQueueLimit) ac.reportBlocking(sender, this)
    if (!ac.isBlocked(this) && !scheduled){
      scheduled = true
      scheduleRun()
    }
  }
  def sendAsync(f: scala.concurrent.Future[T])
               (implicit fileName: sourcecode.FileName,
                line: sourcecode.Line,
                sender: BaseActor[_]) = {
    f.onComplete{
      case scala.util.Success(v) => this.send(v)(fileName, line, sender)
      case scala.util.Failure(e) => ac.reportFailure(e)
    }
  }

  def runBatch0(msgs: Seq[(T, Context.Token)]): Unit
  private[this] def runWithItems(): Unit = {
    val msgs = synchronized(queue.dequeueAll(_ => true))

    if (msgs.nonEmpty) runBatch0(msgs)

    synchronized{
      if (queue.nonEmpty) this.scheduleRun()
      else{
        assert(scheduled)
        for(noLongerBlocked <- ac.clearBlocking(this)){
          noLongerBlocked.scheduleRun()
        }
        scheduled = false
      }
    }
  }
}

abstract class BatchActor[T]()(implicit ac: Context) extends BaseActor[T]{
  def runBatch(msgs: Seq[T]): Unit
  def runBatch0(msgs: Seq[(T, Context.Token)]): Unit = {
    try {
      msgs.foreach{case (m, token) => ac.reportRun(this, m, token)}
      runBatch(msgs.map(_._1))
    }
    catch{case e: Throwable => ac.reportFailure(e)}
    finally msgs.foreach{case (m, token) => ac.reportComplete(token)}

  }
}

abstract class SimpleActor[T]()(implicit ac: Context) extends BaseActor[T]{
  def run(msg: T): Unit
  override def runBatch0(msgs: Seq[(T, Context.Token)]): Unit = {
    for((msg, token) <- msgs) {
      try {
        ac.reportRun(this, msg, token)
        run(msg)
      }
      catch{case e: Throwable => ac.reportFailure(e)}
      finally ac.reportComplete(token)
    }
  }
}

abstract class StateMachineActor[T]()(implicit ac: Context) extends SimpleActor[T]() {
  class State(run0: T => State = null){
    def run = run0
  }
  protected[this] def initialState: State
  private[this] var state0: State = null
  protected[this] def state = {
    if (state0 == null) state0 = initialState
    state0
  }
  def run(msg: T): Unit = {
    state0 = state.run(msg)
  }
}

class ProxyActor[T, V](f: T => V, downstream: Actor[V])
                      (implicit ac: Context) extends SimpleActor[T]{
  def run(msg: T): Unit = downstream.send(f(msg))
}

class SplitActor[T](downstreams: Actor[T]*)
                   (implicit ac: Context) extends SimpleActor[T]{
  def run(msg: T): Unit = downstreams.foreach(_.send(msg))
}