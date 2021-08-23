Motivation:

<p><a href="https://calcite.apache.org/">Apache Calcite</a> has an in-built Sql Parser. This plugin helps to keep the template files and configuration to a different project. Thus,allowing to have cleaner build structure and project dependencies.</p>

Configurations:

1) codegenDirectory : Set a directory where fmpp template files and configurations are placed.
    <br>The recommended directory structure is<br>
   <pre>codegen/<br>
    templates/<br>
    includes/<br>
    config.fmpp<br></pre>

2) outputTemplateDirectory : Directory where template file is generated.

3) outputDirectory : Directory where java sources files are generated.

4) packageName : Package for generated java files.

5) outputJar : Required java jar file for generated sources. 
   
Usage: 

1) compileParserSource : Compile java sources from $outputDirectory and project build's javaSource into $outputJar

2) generateParserSource : Create java sources from templates in $outputTemplateDirectory from $outputDirectory

3) generateParserTemplate : Create templates files from $codegenDirectory into $outputTemplateDirectory.

Each task aggregates dependent task.
   
Example: 
 <pre>
    import scala.reflect.io.{Directory, File}
    val project=project
            .in(file("example"))
            .enablePlugins(ParserGeneratorPlugin)
            .settings(
                codegenDirectory := Directory(baseDirectory.value / "src/main/codegen"),
                outputTemplateDirectory := Directory(target.value / "fmpp"),
                outputDirectory := Directory(target.value / "javacc"),
                packageName := "my.package",
                outputJar := File(target.value / "generated" / "my_example.jar"),
                javacOptions ++= Seq(
                    "-classpath",
                    (Compile / dependencyClasspath).value.map(file => file.data.getAbsolutePath).mkString(":")
            )
  )
</pre>