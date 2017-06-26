import java.io.File

import com.typesafe.config.ConfigFactory

name := "fsm4s"

version := "1.0"

scalaVersion := "2.11.11"

crossScalaVersions := Seq("2.10", "2.11", "2.12")

libraryDependencies ++= {
  Seq[ModuleID](
    "org.scalatest" %% "scalatest" % "3.0.3" % "test"
  )
}

// Your profile name of the sonatype account. The default is the same with the organization value
sonatypeProfileName := "fsm4s"
// To sync with Maven central, you need to supply the following information:
publishMavenStyle := true

// License of your choice
licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
homepage := Some(url("https://github.com/weicheng113/fsm4s"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/weicheng113/fsm4s"),
    "scm:git@github.com:weicheng113/fsm4s.git"
  )
)
developers := List(
  Developer(id="weicheng113", name="Cheng Wei", email="weicheng112@gmail.com", url=url("https://github.com/weicheng113"))
)