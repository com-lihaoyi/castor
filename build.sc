import mill._, scalalib._, scalajslib._, scalanativelib._, publish._
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.4`
import de.tobiasroeser.mill.vcs.version.VcsVersion
import $ivy.`com.github.lolgab::mill-mima::0.0.10`
import com.github.lolgab.mill.mima._
import mill.scalalib.api.Util.isScala3

val communityBuildDottyVersion = sys.props.get("dottyVersion").toList

val scala211 = "2.11.12"
val scala212 = "2.12.16"
val scala213 = "2.13.8"
val scala3 = "3.1.3"

val scalaVersions = scala3 :: scala213 :: scala212 :: scala211 :: communityBuildDottyVersion

val scalaJSVersions = scalaVersions.map((_, "1.10.1"))
val scalaNativeVersions = scalaVersions.map((_, "0.4.5"))

trait MimaCheck extends Mima {
  def mimaPreviousVersions = VcsVersion.vcsState().lastTag.toSeq
}

object castor extends Module {
  abstract class ActorModule(crossVersion: String) extends CrossScalaModule with PublishModule with MimaCheck {
    def publishVersion = VcsVersion.vcsState().format()

    // Temporary until the next version of Mima gets released with
    // https://github.com/lightbend/mima/issues/693 included in the release.
    def mimaPreviousArtifacts =
      if(isScala3(crossVersion)) Agg.empty[Dep] else super.mimaPreviousArtifacts()

    def crossScalaVersion = crossVersion
    def pomSettings = PomSettings(
      description = artifactName(),
      organization = "com.lihaoyi",
      url = "https://github.com/com-lihaoyi/castor",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("com-lihaoyi", "castor"),
      developers = Seq(
        Developer("lihaoyi", "Li Haoyi","https://github.com/lihaoyi")
      )
    )

    def artifactName = "castor"
    def platformSegment: String
    def millSourcePath = super.millSourcePath / os.up

    def sources = T.sources(
      millSourcePath / "src",
      millSourcePath / s"src-$platformSegment"
    )

    def ivyDeps = Agg(ivy"com.lihaoyi::sourcecode::0.2.8")
  }
  trait ActorTestModule extends ScalaModule with TestModule.Utest {
    def platformSegment: String
    def sources = T.sources(
      millSourcePath / "src",
      millSourcePath / s"src-$platformSegment"
    )
    def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.8.1")
  }

  object js extends Cross[ActorJsModule](scalaJSVersions:_*)
  class ActorJsModule(crossScalaVersion: String, crossScalaJsVersion: String) extends ActorModule(crossScalaVersion) with ScalaJSModule {
    def platformSegment = "js"
    def scalaJSVersion = crossScalaJsVersion
    def millSourcePath = super.millSourcePath / os.up
    override def sources = T.sources {
      super.sources() ++ Seq(PathRef(millSourcePath / "src-js-native"))
    }
    object test extends Tests with ActorTestModule {
      def platformSegment = "js"
      def scalaVersion = crossScalaVersion
    }
  }
  object jvm extends Cross[ActorJvmModule](scalaVersions: _*)
  class ActorJvmModule(crossScalaVersion: String) extends ActorModule(crossScalaVersion) {
    def platformSegment = "jvm"
    object test extends Tests with ActorTestModule{
      def platformSegment: String = "jvm"
      def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"com.lihaoyi::os-lib:0.8.1"
      )
    }
  }
  object native extends Cross[ActorNativeModule](scalaNativeVersions:_*)
  class ActorNativeModule(crossScalaVersion: String, crossScalaNativeVersion: String) extends ActorModule(crossScalaVersion) with ScalaNativeModule {
    def platformSegment = "native"
    def scalaNativeVersion = crossScalaNativeVersion
    def millSourcePath = super.millSourcePath / os.up
    override def sources = T.sources {
      super.sources() ++ Seq(PathRef(millSourcePath / "src-js-native"))
    }
    object test extends Tests with ActorTestModule {
      def platformSegment = "native"
    }
  }
}
