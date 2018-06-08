package utils

import javax.inject._

// For Files
import java.nio.file.Paths
import java.io.File
import org.apache.poi.ss.usermodel.{WorkbookFactory, DataFormatter}

// Collections
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

// Model
import models._

@Singleton
class FinalExamParser {

  def save(file: String, courseId: Long, repo: ExamRepository) {
    val workbook = WorkbookFactory.create(new File(file))
    val formatter = new DataFormatter()
    val sheet = workbook.getSheetAt(0)

    val examInfoRow = sheet.getRow(1)
    val total = examInfoRow.getCell(2).getNumericCellValue()
    val totalWeightage = examInfoRow.getCell(3).getNumericCellValue()

    for (row <- sheet) {
      // Since first two row is header.
      if (row.getRowNum > 1) {
        val studentId = formatter.formatCellValue(row.getCell(0))
        val mark      = row.getCell(2).getNumericCellValue()
        val weightage = row.getCell(3).getNumericCellValue()

        repo.create(courseId, studentId, mark, total, weightage, totalWeightage)
      }
    }

  }

  def saveWithMetric(
    file: String,
    courseId: Long,
    eRepo: ExamRepository,
    qRepo: QuestionRepository,
    mRepo: MetricRepository) {
  }
}
