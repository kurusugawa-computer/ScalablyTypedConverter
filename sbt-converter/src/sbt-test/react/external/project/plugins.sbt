{
  val pluginVersion = System.getProperty("plugin.version")
  if (pluginVersion == null)
    throw new RuntimeException("""|The system property 'plugin.version' is not defined.
                                  |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
  else addSbtPlugin("jp.kurusugawa.scalablytyped" % """sbt-converter""" % pluginVersion)
}
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.8.0")
