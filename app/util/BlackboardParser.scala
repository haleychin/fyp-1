package utils

import scala.io.Source
import scala.io.Codec

case class CourseworkData(name: String, totalMark: Double)

object BlackboardParser {

  def parse(file: String) {
    val lines = Source
      .fromFile(file)(Codec("UTF-16LE"))
      .getLines
      .map(convertLine(_))

    if (lines.hasNext) {
      val header = extractHeader(lines.next)
      println(header)
    }

    lines.foreach { line =>
      // println(line)
    }

  }

  def convertLine(line: String): String = {
    """\t+""".r.replaceAllIn(line, ",")
  }

  def extractHeader(line: String): Map[String, Int] = {
    val elements = line.split(",")
    elements.zipWithIndex.map { case (value, index) =>
      println(s"$index: $value")
      (value, index)
    }.toMap
  }

  def extractResult(line: String): CourseworkData = {
    // Extract using Regex here
    CourseworkData("Project", 30.0)
  }

}
