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
    }

    lines.foreach { line =>
      // println(line)
    }

  }

  def convertLine(line: String): String = {
    """\t+""".r.replaceAllIn(line, ",")
  }

  def extractHeader(line: String): Map[String, Int] = {
    println(extractResult("Project Part 1 [Total Pts: 10 Score] |17955"))
    val elements = line.split(",")
    elements.zipWithIndex.map { case (value, index) =>
      // println(s"$index: $value")
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
