package castor
import utest._
object ActorsTest extends TestSuite{
  def tests = Tests{
    test("hello"){
      import Context.Simple.global

      sealed trait Msg

      object logger extends SimpleActor[Msg]{
        def run(msg: Msg) = {

        }
      }
    }
    test("async"){
      import Context.Simple.global
      import scala.concurrent.Future
      object foo extends SimpleActor[Unit]{
        def run(msg: Unit) = ()
      }
      foo.sendAsync(Future(()))
    }
  }
}
