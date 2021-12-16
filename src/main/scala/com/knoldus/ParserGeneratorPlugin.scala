package com.knoldus

import sbt.Keys.{javaSource, javacOptions, streams, target}
import sbt.nio.Keys.fileInputs
import sbt.nio.file.**
import sbt.{AutoPlugin, Compile, Setting, fileToFileOps, settingKey, taskKey}

import scala.reflect.io.Path.jfile2path
import scala.reflect.io.{Directory, File}

object ParserGeneratorPlugin extends AutoPlugin {

  override def trigger = noTrigger

  object autoImport {
    val codegenDirectory = settingKey[Directory]("The codegen directory having template files and fmpp config")
    val outputTemplateDirectory = settingKey[Directory]("The directory where template is generated")
    val outputDirectory = settingKey[Directory]("The directory where java code is generated")
    val packageName = settingKey[String]("The package name of generated java files")
    val outputJar = settingKey[File]("The output jar file")
    val generateParserTemplate = taskKey[File]("Generates parser.jj file")
    val generateParserSource = taskKey[Directory]("Generates java source code")
    val compileParserSource = taskKey[File]("Compile code from java source to java jar")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    outputJar := File(target.value / "generatedJar" / "parser.jar"),
    generateParserTemplate / fileInputs += codegenDirectory.value.jfile.toGlob / **,
    generateParserTemplate := {
      if (generateParserTemplate.inputFileChanges.hasChanges || !outputTemplateDirectory.value.exists || outputTemplateDirectory.value.isEmpty) {
        val directory = codegenDirectory.value
        val configFile = directory.deepFiles.find(file => file.name.equalsIgnoreCase("config.fmpp"))
          .getOrElse(throw new Exception(s"No config.fmpp found at [$directory]"))
        val defaultConfigFile = directory.deepFiles
          .find(file => file.name.equalsIgnoreCase("default_config.fmpp"))
          .getOrElse(throw new Exception(s"No default_config.fmpp found at [$directory]"))
        val template = directory.dirs
          .find(dir => dir.name.equalsIgnoreCase("templates"))
          .getOrElse(throw new Exception(s"No templates found at [$directory]"))
        new ParserGenerator(streams.value.log).generateParserTemplate(defaultConfigFile, configFile, template, outputTemplateDirectory.value)
      }
      else outputTemplateDirectory.value.deepFiles.find(_.name.equalsIgnoreCase("parser.jj")).get
    },
    generateParserSource / fileInputs += codegenDirectory.value.jfile.toGlob / **,
    generateParserSource := {
      if (generateParserSource.inputFileChanges.hasChanges || !outputDirectory.value.exists || outputDirectory.value.isEmpty) {
        new ParserGenerator(streams.value.log).generateParserSource(generateParserTemplate.value, outputDirectory.value, packageName.value)
      }
      else outputDirectory.value
    },
    compileParserSource := {
      val javaSources = Seq(
        Directory((Compile / javaSource).value),
        generateParserSource.value
      )
      new ParserGenerator(streams.value.log).compileParserSource(javaSources, javacOptions.value, outputJar.value)
    }
  )

}
