package utils

import javax.inject._
import models.CourseAPI

// Excel
import org.apache.poi.xssf.usermodel._
import org.apache.poi.ss.usermodel._
import org.apache.poi.ss.util.CellRangeAddress
import java.io.FileOutputStream

@Singleton
class CourseExporter {

  def export(filename: String, courseApi: CourseAPI) {
    val wb = new XSSFWorkbook()
    val createHelper = wb.getCreationHelper()
    val sheet = wb.createSheet("Sheet1")
    val format = wb.createDataFormat()
    // =====
    // Style
    // =====

    // Bold Style
    val centerboldStyle = wb.createCellStyle()
    val font = wb.createFont()
    font.setBold(true)
    val halign = HorizontalAlignment.CENTER
    val valign = VerticalAlignment.CENTER
    centerboldStyle.setAlignment(halign)
    centerboldStyle.setVerticalAlignment(valign)
    centerboldStyle.setFont(font)

    val doubleStyle = wb.createCellStyle()
    doubleStyle.setDataFormat(format.getFormat("0.0"))

    var percentageStyle = wb.createCellStyle()
    percentageStyle.setDataFormat(format.getFormat("0%"))
    percentageStyle.setAlignment(halign)
    percentageStyle.setVerticalAlignment(valign)
    percentageStyle.setFont(font)

    // ==============
    // Creating Row 3
    // ==============
    val row3  = sheet.createRow(2)
    // Minus one to remove exam from courseworks number
    val courseworkNumber = courseApi.coursework.courseworks.size - 1
    val totalCWCellNumber = 5 + courseworkNumber - 1

    // CW Breakdown
    val cwBdCell = row3.createCell(5)
    sheet.addMergedRegion(new CellRangeAddress(
      2, //first row (0-based)
      2, //last row  (0-based)
      5, //first column (0-based)
      totalCWCellNumber//last column  (0-based)
    ))
    // Total CW | Total CW | Exam | Exam | Total | Grade | Remark
    val cwCell = row3.createCell(totalCWCellNumber + 1)
    val cw2Cell = row3.createCell(totalCWCellNumber + 2)
    val examCell = row3.createCell(totalCWCellNumber + 3)
    val exam2Cell = row3.createCell(totalCWCellNumber + 4)
    val totalCell = row3.createCell(totalCWCellNumber + 5)
    val gradeCell = row3.createCell(totalCWCellNumber + 6)
    sheet.addMergedRegion(new CellRangeAddress(
      2,
      3,
      totalCWCellNumber + 6,
      totalCWCellNumber + 6
    ))
    val remarkCell = row3.createCell(totalCWCellNumber + 7)
    sheet.addMergedRegion(new CellRangeAddress(
      2,
      3,
      totalCWCellNumber + 7,
      totalCWCellNumber + 7
    ))

    cwBdCell.setCellValue(
      createHelper.createRichTextString("CW Breakdown")
    )
    cwCell.setCellValue(
      createHelper.createRichTextString("Total CW")
    )
    cw2Cell.setCellValue(
      createHelper.createRichTextString("Total CW")
    )
    exam2Cell.setCellValue(
      createHelper.createRichTextString("Exam")
    )
    examCell.setCellValue(
      createHelper.createRichTextString("Exam")
    )
    totalCell.setCellValue(
      createHelper.createRichTextString("Total")
    )
    gradeCell.setCellValue(
      createHelper.createRichTextString("Grade")
    )
    remarkCell.setCellValue(
      createHelper.createRichTextString("Remark")
    )

    cwBdCell.setCellStyle(centerboldStyle)
    cwCell.setCellStyle(centerboldStyle)
    cw2Cell.setCellStyle(centerboldStyle)
    exam2Cell.setCellStyle(centerboldStyle)
    examCell.setCellStyle(centerboldStyle)
    totalCell.setCellStyle(centerboldStyle)
    gradeCell.setCellStyle(centerboldStyle)
    remarkCell.setCellStyle(centerboldStyle)

    // ==============
    // Creating Row 4
    // ==============
    val row4  = sheet.createRow(3)

    val studentIdCell = row4.createCell(0)
    val icCell = row4.createCell(1)
    val ccCell = row4.createCell(2)
    val intakeCell = row4.createCell(3)
    val statusCell = row4.createCell(4)

    val cwBtmCell = row4.createCell(totalCWCellNumber + 1)
    val cw2BtmCell = row4.createCell(totalCWCellNumber + 2)
    val examBtmCell = row4.createCell(totalCWCellNumber + 3)
    val exam2BtmCell = row4.createCell(totalCWCellNumber + 4)
    val totalBtmCell = row4.createCell(totalCWCellNumber + 5)

    courseApi.coursework.courseworks.zipWithIndex.foreach { case ((name, _), i) =>
      if (name != "Exam") {
        val cell = row4.createCell(5 + i)
        cell.setCellValue(name)
        cell.setCellStyle(centerboldStyle)
      }
    }

    studentIdCell.setCellValue("Student UID")
    icCell.setCellValue("IC/Passport No")
    ccCell.setCellValue("Course Code")
    intakeCell.setCellValue("Intake")
    statusCell.setCellValue("Status")

    cw2BtmCell.setCellValue(1)
    examBtmCell.setCellValue(1)
    totalBtmCell.setCellValue(courseApi.coursework.total / 100.0)
    exam2BtmCell.setCellValue(courseApi.exam.weightage / 100.0)

    studentIdCell.setCellStyle(centerboldStyle)
    icCell.setCellStyle(centerboldStyle)
    ccCell.setCellStyle(centerboldStyle)
    intakeCell.setCellStyle(centerboldStyle)
    statusCell.setCellStyle(centerboldStyle)

    cw2BtmCell.setCellStyle(percentageStyle)
    examBtmCell.setCellStyle(percentageStyle)
    exam2BtmCell.setCellStyle(percentageStyle)
    totalBtmCell.setCellStyle(percentageStyle)

    // ===============
    // Student Details
    // ===============
    courseApi.students.zipWithIndex.foreach { case (student, i) =>
      // Create a row and put some cells in it. Rows are 0 based.
      val row = sheet.createRow(i + 4)

      // Create a cell and put a value in it.
      val idCell         = row.createCell(0)
      val icCell         = row.createCell(1)
      val courseCodeCell = row.createCell(2)
      val intakeCell     = row.createCell(3)
      val statusCell     = row.createCell(4)

      idCell.setCellValue(
        createHelper.createRichTextString(student.studentId)
      )
      icCell.setCellValue(
        createHelper.createRichTextString(student.icOrPassport)
      )
      courseCodeCell.setCellValue(
        createHelper.createRichTextString(courseApi.course.get.title)
      )
      intakeCell.setCellValue(
        createHelper.createRichTextString(student.intake)
      )
      statusCell.setCellValue(
        createHelper.createRichTextString("Active")
      )

      // ===========
      // Courseworks
      // ===========
      val courseworkDetail = courseApi.coursework.courseworkDetails.get(student.id)
      var lastIndex = 0
      if (courseworkDetail.isDefined) {
        var cwFullMark = 0.0
        var cwTotalMark = 0.0
        courseApi.coursework.courseworks.zipWithIndex.foreach { case (c, i) =>

          val name      = c._1
          val mark      = courseworkDetail.get.courseworks.get(name).getOrElse(0.0)
          var startIndex = 5

          if (name == "Exam") {
            startIndex += 3
          } else {
            cwFullMark += c._2
            cwTotalMark += mark
          }

          lastIndex = i + startIndex
          val cell = row.createCell(lastIndex)
          cell.setCellValue(mark)
          cell.setCellStyle(doubleStyle)
        }

        // =====
        // Total
        // =====
        val totalCell = row.createCell(lastIndex + 1)
        totalCell.setCellValue(courseworkDetail.get.total)
        totalCell.setCellStyle(doubleStyle)

        // ==============
        // Grade & Remark
        // ==============
        val gradeCell = row.createCell(lastIndex + 2)
        gradeCell.setCellValue(courseworkDetail.get.grade.name)
        val remarkCell = row.createCell(lastIndex + 3)
        remarkCell.setCellValue(courseworkDetail.get.grade.reason)

        // ==================
        // Calculate Total CW
        // ==================
        val hundredCWCell = row.createCell(lastIndex - 2)
        hundredCWCell.setCellValue(cwTotalMark / cwFullMark * 100)
        hundredCWCell.setCellStyle(doubleStyle)
        val weigtageCwCell = row.createCell(lastIndex - 3)
        weigtageCwCell.setCellValue(cwTotalMark)
        weigtageCwCell.setCellStyle(doubleStyle)

        cwBtmCell.setCellValue(cwFullMark / 100.0)
        cwBtmCell.setCellStyle(percentageStyle)
      }

      // ======================
      // Calculate Hundred Exam
      // ======================
      val examDetail = courseApi.exam.examDetails.get(student.id)
      if (examDetail.isDefined) {
        val hundredExamCell = row.createCell(lastIndex - 1)
        hundredExamCell.setCellValue(examDetail.get.exam._1)
        hundredExamCell.setCellStyle(doubleStyle)
      }
    }

    // =========
    // Autowidth
    // =========
    for (i <- 0 to (totalCWCellNumber + 6)) {
      sheet.autoSizeColumn(i)
    }


    // Write the output to a file
    val fileOut = new FileOutputStream(filename)
    wb.write(fileOut);
  }

}
