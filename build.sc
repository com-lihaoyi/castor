import mill._, scalalib._, scalajslib._, scalanativelib._, publish._
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.1.4`
import de.tobiasroeser.mill.vcs.version.VcsVersion
import $ivy.`com.github.lolgab::mill-mima::0.0.9`
import com.github.lolgab.mill.mima._

val dottyVersions = sys.props.get("dottyVersion").toList

val scala211 = "2.11.12"
val scala212 = "2.12.13"
val scala213 = "2.13.4"
val scala30 = "3.0.0"
val scala31 = "3.1.1"

val scala2VersionsAndDotty = scala213 :: scala212 :: scala211 :: dottyVersions

val scalaJSVersions = for {
  scalaV <- scala30 :: scala2VersionsAndDotty
  scalaJSV <- Seq("0.6.33", "1.5.1")
  if scalaV.startsWith("2.") || scalaJSV.startsWith("1.")
} yield (scalaV, scalaJSV)

val scalaNativeVersions = for {
  scalaV <- scala31 :: scala2VersionsAndDotty
  scalaNativeV <- Seq("0.4.3")
} yield (scalaV, scalaNativeV)

object castor extends Module {
  abstract class ActorModule(crossVersion: String) extends CrossScalaModule with PublishModule with Mima {
    def publishVersion = VcsVersion.vcsState().format()
    def mimaPreviousVersions = VcsVersion.vcsState().lastTag.toSeq
    def crossScalaVersion = crossVersion
    def pomSettings = PomSettings(
      description = artifactName(),
      organization = "com.lihaoyi",
      url = "https://github.com/lihaoyi/castor",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("lihaoyi", "castor"),
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
    def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.7.11")
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
  object jvm extends Cross[ActorJvmModule](scala30 :: scala2VersionsAndDotty: _*)
  class ActorJvmModule(crossScalaVersion: String) extends ActorModule(crossScalaVersion) {
    def platformSegment = "jvm"
    object test extends Tests with ActorTestModule{
      def platformSegment: String = "jvm"
      def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"com.lihaoyi::os-lib:0.7.7"
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
