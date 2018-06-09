package utils

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

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
class FinalExamParser @Inject()(implicit ec: ExecutionContext) {

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

    val workbook  = WorkbookFactory.create(new File(file))
    val formatter = new DataFormatter()
    val sheet     = workbook.getSheetAt(0)

    // =================
    // Save Metric to DB
    // =================
    val metricSheet = workbook.getSheetAt(1)
    for (row <- metricSheet) {
      val name         = formatter.formatCellValue(row.getCell(0))
      val description  = formatter.formatCellValue(row.getCell(1))
      mRepo.create(courseId, name, description)
    }

    // =================================================
    // Get first question and last question column index
    // =================================================
    val firstRow = sheet.getRow(0)
    var questionStart = 0
    var questionEnd   = 0
    for (cell <- firstRow) {
      val value = cell.getStringCellValue
      if (value == "Questions") {
        questionStart = cell.getColumnIndex()
      } else if (value == "Total") {
        questionEnd   = cell.getColumnIndex() - 1
      }
    }

    // =================================
    // Get exam total mark and weightage
    // =================================
    val rowOne         = sheet.getRow(1)
    val total          = rowOne.getCell(questionEnd + 1).getNumericCellValue()
    val totalWeightage = rowOne.getCell(questionEnd + 2).getNumericCellValue()

    // ===============
    // Save Exam to DB
    // ===============

    val examResults = sheet.drop(4).map { row =>
      if (row.getRowNum > 3) {
        val studentId = formatter.formatCellValue(row.getCell(0))
        val mark      = row.getCell(questionEnd + 1).getNumericCellValue()
        val weightage = row.getCell(questionEnd + 2).getNumericCellValue()

        // Create exam record
        eRepo.create(courseId, studentId, mark, total, weightage, totalWeightage).map(_ => studentId)
      } else {
        Future.successful(None)
      }
    }

    val future = Future.sequence(examResults).map { list =>
      list.foreach { studentId =>
        // ====================
        // Get question details
        // ====================
        var i: Int = 0;
        for (i <- questionStart to questionEnd) {
          val metricRow   = sheet.getRow(2)
          val fullMarkRow = sheet.getRow(3)

          // Question Info
          val name        = rowOne.getCell(i).getStringCellValue()
          val metric      = metricRow.getCell(i).getStringCellValue()
          val fullMark    = fullMarkRow.getCell(i).getNumericCellValue()

          // Unwrap Option[Exam]
          qRepo.create(courseId, studentId.toString, name, fullMark, 4)
        }
      }

    }
  }
}
