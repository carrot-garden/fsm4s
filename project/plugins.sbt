addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "1.3.0")
addSbtPlugin("com.dwijnand" % "sbt-travisci" % "1.1.0")
addMavenResolverPlugin

libraryDependencies += "com.typesafe" % "config" % "1.3.1"