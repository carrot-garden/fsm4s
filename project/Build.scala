import java.io.File

import com.typesafe.config.ConfigFactory
import sbt.Build
import sbt.Credentials
import sbt.Keys.credentials

object HelloBuild extends Build {
  initializeSonatypeAccount()

  private def initializeSonatypeAccount(): Unit = {
    val file = new File("project/local.conf")
    if (file.exists()) {
      val config = ConfigFactory.parseFile(file)
      credentials +=Credentials("Sonatype Nexus Repository Manager",
        "oss.sonatype.org",
        config.getString("sonatype.username"),
        config.getString("sonatype.password"))
    }
  }
}
