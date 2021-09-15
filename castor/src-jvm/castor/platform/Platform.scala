package castor.platform

import castor._

import scala.concurrent.ExecutionContext

import java.util.concurrent.{Executors, ThreadFactory, TimeUnit}

private [castor] object Platform {
  def executionContext = ExecutionContext.fromExecutorService(Context.Simple.threadPool)
}
