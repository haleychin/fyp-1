package utils

import scala.io.Source
import scala.io.Codec

object BlackboardParser {

  def parse(file: String) {
    val lines = Source.fromFile(file)(Codec("UTF-16LE")).getLines
    for (line <- lines) {
      val newLine = """\t+""".r.replaceAllIn(line, ",")
      println(newLine)
    }
  }

}
