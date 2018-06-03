package utils

import scala.io.Source
import scala.io.Codec
import scala.collection.mutable.ArrayBuffer
import javax.inject._

import models.CourseworkRepository

case class CourseworkData(name: String, totalMark: Double)

@Singleton
class BlackboardParser {

  var header: Map[String, Int] = Map[String, Int]()
  var courseworks: ArrayBuffer[CourseworkData] = new ArrayBuffer[CourseworkData]()
  var lines: ArrayBuffer[String] = new ArrayBuffer[String]()
  var courseworkNames: ArrayBuffer[String] = new ArrayBuffer[String]()

  def parse(file: String) {
    Source
      .fromFile(file)(Codec("UTF-16LE"))
      .getLines.foreach { line => lines += line }

    if (lines.length > 0) {
      header = extractHeader(lines(0))
    }
  }

  def getCourseworks(courseworkNames: List[String]) {
    courseworkNames.foreach { c =>
      this.courseworkNames += c
      courseworks += extractResult(c)
    }
  }

  def saveToDb(courseId: Long, repo: CourseworkRepository) {
    lines = lines.drop(1)
    println(lines)

    lines.foreach { line =>
      val courseCode = extractCommon(line, "First Name")
      val name = extractCommon(line, "Last Name")
      val studentId = extractCommon(line, "Username")
      println(courseCode, name, studentId)
      val courseworksMarks = courseworks.zipWithIndex.map { case (c, index) =>
        val name = courseworkNames(index)
        val mark = extractCommon(line, name)
        repo.create(courseId,studentId, c.name, mark.toDouble, c.totalMark)
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
    val index = header.get(name).getOrElse(0)
    val newLine = line.split("\t")(index)
    extractResult(line)
  }

  def extractHeader(line: String): Map[String, Int] = {
    val elements = line.split("\t")
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
