package com.knoldus

import fmpp.setting.Settings
import org.javacc.parser.Main
import sbt.TrapExit
import sbt.util.{CacheStoreFactory, Logger}

import java.nio.charset.Charset
import java.util
import java.util.Locale
import javax.tools.{StandardLocation, ToolProvider}
import scala.jdk.CollectionConverters.asJavaIterableConverter
import scala.reflect.io.{Directory, File}

class ParserGenerator(logger: Logger) {

  def generateParserTemplate(
                              defaultConfig: File,
                              config: File,
                              templateDirectory: Directory,
                              outputDirectory: Directory
                            ): File = {
    logger.info(
      s"""Fmpp settings :-
         | config : ${config.toString()},
         | defaultConfig : ${defaultConfig.toString()},
         | template : ${templateDirectory.toString()},
         | target : ${outputDirectory.toString()}
         |""".stripMargin)

    val fmppSettings = new fmpp.setting.Settings(config.jfile)
    fmppSettings.set(
      Settings.NAME_DATA,
      s"tdd(${config.jfile.getAbsolutePath.tddString()}), default: tdd(${defaultConfig.jfile.getAbsolutePath.tddString()})"
    )
    fmppSettings.set(Settings.NAME_SOURCE_ROOT, templateDirectory.jfile.getAbsolutePath)
    fmppSettings.set(Settings.NAME_OUTPUT_ROOT, outputDirectory.jfile.getAbsolutePath)
    fmppSettings.loadDefaults(config.jfile)
    fmppSettings.execute()
    logger.info("Processing done")

    outputDirectory.deepFiles.find(_.name.equalsIgnoreCase("parser.jj"))
      .getOrElse(throw new Exception("Processing was done but no parser.jj found"))
  }

  def generateParserSource(template: File, target: Directory, packageName: String, static: Boolean = false, lookAhead: Int = 1): Directory = {
    val packageDirectory = packageName.replace(".", File.separator)
    val args = Array(s"-STATIC=$static",
      s"-LOOKAHEAD:$lookAhead",
      s"-OUTPUT_DIRECTORY:${target.jfile}/$packageDirectory",
      s"${template.jfile}"
    )

    logger.info(
      s"""JavaCC settings :-
         |${args.mkString(",\n")}
         |""".stripMargin)

    //Prevent system.exit
    val securityManager = TrapExit.installManager()
    TrapExit(Main.main(args), sbt.util.Logger.Null)
    TrapExit.uninstallManager(securityManager)
    logger.info("Processing Done")

    target
  }

  def compileParserSource(sourceFiles: Seq[Directory], javacOptions: Seq[String], jar: File): File = {
    logger.info(s"Compiling sources at [${sourceFiles.mkString(",\n")}]")
    logger.info(s" with javac options [${javacOptions.mkString(",\n")}]")
    val compiledSources = compile(sourceFiles, javacOptions)
    packageJar(compiledSources, jar)
    logger.info(s"Packaging Done [${jar.toString()}]")
    jar
  }

  protected def compile(sourcesFiles: Seq[Directory], javacOptions: Seq[String]): Directory = {
    val targetDirectory = Directory.makeTemp()
    val compilerOptions = {
      if (javacOptions.isEmpty) null
      else javacOptions.asJava
    }
    val compiler = ToolProvider.getSystemJavaCompiler
    val fileManager = compiler.getStandardFileManager(null, Locale.getDefault, Charset.defaultCharset())
    fileManager.setLocation(StandardLocation.CLASS_OUTPUT, util.Arrays.asList(targetDirectory.jfile))
    val success = compiler.getTask(
      null,
      fileManager,
      null,
      compilerOptions,
      null,
      fileManager.getJavaFileObjectsFromFiles(
        sourcesFiles.flatMap(dir => dir.deepFiles.map(_.jfile))
          .toList.asJava
      )
    ).call()
    fileManager.close()
    assert(success, "Compilation failed")
    targetDirectory
  }

  protected def packageJar(from: Directory, toJar: File): Unit = {
    val mappings = from.deepFiles.map(file => {
      file.jfile -> from.relativize(file).toString()
    })
    sbt.Package.apply(
      new sbt.Package.Configuration(mappings.toSeq, toJar.jfile, Seq.empty),
      CacheStoreFactory(Directory.makeTemp().jfile),
      sbt.util.Logger.Null,
      Some(System.currentTimeMillis())
    )
  }

  implicit class TddString(stringData: String) {
    def tddString(): String = {
      val newString = stringData.replace("\\", "\\\\").replace("\"", "\\\"")
      "\"" + newString + "\""
    }
  }

}