package test.castor

import utest._
import scala.concurrent.duration._
import scala.concurrent.Promise
import scala.util.Try
object JsActorsTest extends TestSuite {

  object debounce {
    sealed trait Msg
    case class Debounced() extends Msg
    case class Text(value: String) extends Msg

    class Logger(
        debounceTime: scala.concurrent.duration.Duration,
        log: String => Unit
    )(implicit ac: castor.Context)
        extends castor.StateMachineActor[Msg] {
      override def initialState = Idle()
      case class Idle()
          extends State({
            case Text(value) =>
              ac.scheduleMsg(this, Debounced(), debounceTime)
              Buffering(Vector(value))
            case Debounced() =>
              Idle()
          })
      case class Buffering(buffer: Vector[String])
          extends State({
            case Text(value) => Buffering(buffer :+ value)
            case Debounced() =>
              log(buffer.mkString(" "))
              Idle()
          })
    }
  }

  def tests = Tests {

    test("debounce") {
      import debounce._

      implicit val ac = new castor.Context.Test()
      var log: Seq[String] = Nil
      val logger = new Logger(Duration(50, MILLISECONDS), l => log = log :+ l)

      logger.send(Text("I am cow"))
      logger.send(Text("hear me moo"))

      scalajs.js.timers.setTimeout(100) {
        logger.send(Text("I weight twice as much as you"))
        logger.send(Text("And I look good on the barbecue"))

        scalajs.js.timers.setTimeout(100) {
          logger.send(Text("Yoghurt curds cream cheese and butter"))
          logger.send(Text("Comes from liquids from my udder"))
          logger.send(Text("I am cow, I am cow"))
          logger.send(Text("Hear me moo, moooo"))
        }
      }

      log ==> Seq()

      val promise = Promise[Unit]()
      scalajs.js.timers.setTimeout(300) {
        promise.complete(Try({

          log ==> Seq(
            "I am cow hear me moo",
            "I weight twice as much as you And I look good on the barbecue",
            "Yoghurt curds cream cheese and butter Comes from liquids from my udder I am cow, I am cow Hear me moo, moooo"
          )

        }))
      }

      promise.future

    }
  }
}
