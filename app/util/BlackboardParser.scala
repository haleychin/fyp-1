package utils

import scala.io.Source
import scala.io.Codec
import scala.collection.mutable.ArrayBuffer

case class CourseworkData(name: String, totalMark: Double)

object BlackboardParser {

  private var header: Map[String, Int] = Map[String, Int]()
  private var courseworks: ArrayBuffer[CourseworkData] = new ArrayBuffer[CourseworkData]()
  private var lines: ArrayBuffer[String] = new ArrayBuffer[String]()

  def parse(file: String, courseworkNames: Array[String]) {
    Source
      .fromFile(file)(Codec("UTF-16LE"))
      .getLines.foreach { line => lines += line }

    if (lines.length > 0) {
      header = extractHeader(lines(0))
    }

    courseworkNames.foreach { c =>
      val value = header.get(c).get
      courseworks += extractResult(c)
    }

    lines.foreach { line =>
      val courseCode = extractCommon(line, "First Name")
      val name = extractCommon(line, "Last Name")
      val studentId = extractCommon(line, "Username")
      val courseworksMarks = courseworks.zipWithIndex.map { case (c, index) =>
        val mark = extractCommon(line, courseworkNames(index))
        mark
      }
    }

  }

  def convertLine(line: String): String = {
    """\t+""".r.replaceAllIn(line, ",")
  }

  def extractCommon(line: String, head: String): String = {
    val index = header.get(head).getOrElse(0)
    line.split("\t")(index)
  }

  def extractCoursework(line: String, name: String): CourseworkData = {
    val index = header.get(name).get
    val newLine = line.split("\t")(index)
    extractResult(line) }

  def extractHeader(line: String): Map[String, Int] = {
    val elements = line.split("\t")
    elements.zipWithIndex.map { case (value, index) =>
      println(s"$index: $value")
      (value, index)
    }.toMap
  }

  def extractResult(line: String): CourseworkData = {
    val markRegex = """Total Pts: (\d*) Score""".r
    val lines = line.split("\\[")
    val mark = markRegex.findFirstMatchIn(lines(1)).get.group(1)

    CourseworkData(lines(0).trim(), mark.toDouble)
  }

}
