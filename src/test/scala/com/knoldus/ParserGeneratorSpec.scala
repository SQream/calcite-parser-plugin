package com.knoldus

import org.scalatest.enablers.Existence.existenceOfFile
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import sbt.internal.util.ConsoleLogger

import scala.reflect.io.{Directory, File}

class ParserGeneratorSpec extends AnyFlatSpecLike with Matchers {

  def withTempDirectory(testCode: Directory => Any) {
    val outputDirectory = Directory.makeTemp()
    try {
      testCode(outputDirectory)
    }
    finally outputDirectory.deleteRecursively()
  }

  val codegenPath: String = getClass.getResource("/codegen").getFile
  val codeGenDir: Directory = Directory(codegenPath)
  val configFmpp: Option[File] = codeGenDir.files.find(_.name.equals("config.fmpp"))
  val defaultConfigFmpp: Option[File] = codeGenDir.files.find(_.name.equals("default_config.fmpp"))
  val templateDir: Option[Directory] = codeGenDir.dirs.find(_.name.equals("templates"))
  val parserGenerator = new ParserGenerator(ConsoleLogger(System.out))

  it should "generate parser.jj" in withTempDirectory(dir => {
    val finalFile = parserGenerator.generateParserTemplate(
      defaultConfigFmpp.get,
      configFmpp.get,
      templateDir.get,
      dir
    )
    finalFile.jfile should exist
  })

  it should "generate java files" in withTempDirectory(dir => {
    val parserTemplate = getClass.getResource("/javacc/Parser.jj").getPath
    val outputDirectory = parserGenerator.generateParserSource(File(parserTemplate), dir, "org.apache.calcite.sql.parser.impl")
    outputDirectory.jfile should exist
    outputDirectory.deepFiles should have length 7
  })

  it should "compile java files" in withTempDirectory(dir => {
    val sourceFiles = getClass.getResource("/javaSource").getPath
    val file = parserGenerator.compileParserSource(Seq(Directory(sourceFiles)), Seq.empty, dir / File("test.jar"))
    //ParserGenerator.compileParserSource(Seq(Directory(sourceFiles)), Directory("./result") / File("test.jar"))
    file.jfile should exist
  })

}
