organization := "org.scalablytyped"
name := "react-bootstrap"
version := "0.32-a27e76"
scalaVersion := "3.1.2"
enablePlugins(ScalaJSPlugin)
libraryDependencies ++= Seq(
  "com.olvind" %%% "scalablytyped-runtime" % "2.4.2",
  "me.shadaj" %%% "slinky-web" % "0.7.2",
  "org.scalablytyped" %%% "react" % "16.9.2-3bd1af",
  "org.scalablytyped" %%% "std" % "0.0-unknown-dc99ee")
publishArtifact in packageDoc := false
scalacOptions ++= List("-encoding", "utf-8", "-feature", "-language:implicitConversions", "-language:higherKinds", "-language:existentials", "-no-indent", "-source:future")
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
