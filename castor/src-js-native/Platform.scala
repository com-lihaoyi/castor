package castor.platform

import scala.concurrent.ExecutionContext

private [castor] object Platform {
  def executionContext = ExecutionContext.global
}
