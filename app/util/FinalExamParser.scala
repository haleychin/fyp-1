package utils

import javax.inject._

// For Files
import java.nio.file.Paths
import java.io.File
import org.apache.poi.ss.usermodel.{WorkbookFactory, DataFormatter}

// Collections
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

@Singleton
class FinalExamParser {

  def parse(file: String) {
    val workbook = WorkbookFactory.create(new File(file))
    val formatter = new DataFormatter()
    val sheet = workbook.getSheetAt(0)

    val examInfoRow = sheet.getRow(1)
    val total = examInfoRow.getCell(2).getNumericCellValue()
    val weightage = examInfoRow.getCell(3).getNumericCellValue()

    for (row <- sheet) {
      // Since first two row is header.
      if (row.getRowNum > 2) {
        val studentId = row.getCell(0).getStringCellValue()
        val mark      = row.getCell(2).getNumericCellValue()
        val weightage = row.getCell(3).getNumericCellValue()
        println(s"$studentId: $mark\t$weightage")
      }
    }

  }
}
