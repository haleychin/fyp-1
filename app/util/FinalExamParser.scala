package utils

// For Files
import java.nio.file.Paths
import java.io.File
import org.apache.poi.ss.usermodel.{WorkbookFactory, DataFormatter}

// Collections
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

object FinalExamParser {

  def parse(file: String) {
    println("it works")

    val workbook = WorkbookFactory.create(new File(file))
    val formatter = new DataFormatter()
    val sheet = workbook.getSheetAt(0)

    for (row <- sheet) {
      for (col <- row) {
        println(formatter.formatCellValue(col))
      }
    }

  }
}
