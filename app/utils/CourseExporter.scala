package utils

import javax.inject._
import controllers.CourseAPI

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
    val centerboldStyle = wb.createCellStyle();
    val font = wb.createFont();
    font.setBold(true);
    val halign = HorizontalAlignment.CENTER
    val valign = VerticalAlignment.CENTER
    centerboldStyle.setAlignment(halign);
    centerboldStyle.setVerticalAlignment(valign);
    centerboldStyle.setFont(font);

    val doubleStyle = wb.createCellStyle();
    doubleStyle.setDataFormat(format.getFormat("0.0"));

    // ==============
    // Creating Row 3
    // ==============
    val row3  = sheet.createRow(2)
    // Minus one to remove exam from courseworks number
    val courseworkNumber = courseApi.coursework.courseworks.size - 1
    val totalCWCellNumber = 5 + courseworkNumber - 1

    // CW Breakdown
    val cwBdCell = row3.createCell(5)
    cwBdCell.setCellValue(
      createHelper.createRichTextString("CW Breakdown")
    )
    cwBdCell.setCellStyle(centerboldStyle)
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
    val remarkCell = row3.createCell(totalCWCellNumber + 7)
    cwCell.setCellValue(
      createHelper.createRichTextString("Total CW")
    )
    cwCell.setCellStyle(centerboldStyle)

    cw2Cell.setCellValue(
      createHelper.createRichTextString("Total CW")
    )
    cw2Cell.setCellStyle(centerboldStyle)

    exam2Cell.setCellValue(
      createHelper.createRichTextString("Exam")
    )
    exam2Cell.setCellStyle(centerboldStyle)
    examCell.setCellValue(
      createHelper.createRichTextString("Exam")
    )
    examCell.setCellStyle(centerboldStyle)

    totalCell.setCellValue(
      createHelper.createRichTextString("Total")
    )
    totalCell.setCellStyle(centerboldStyle)

    gradeCell.setCellValue(
      createHelper.createRichTextString("Grade")
    )
    gradeCell.setCellStyle(centerboldStyle)

    remarkCell.setCellValue(
      createHelper.createRichTextString("Remark")
    )
    remarkCell.setCellStyle(centerboldStyle)

    courseApi.students.zipWithIndex.foreach { case (student, i) =>
      // Create a row and put some cells in it. Rows are 0 based.
      val row = sheet.createRow(i + 3)

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

      // Courseworks
      val courseworkDetail = courseApi.coursework.courseworkDetails.get(student.id)
      if (courseworkDetail.isDefined) {
        courseApi.coursework.courseworks.zipWithIndex.foreach { case (c, i) =>

          val totalMark = c._2
          val name      = c._1
          val mark      = courseworkDetail.get.courseworks.get(name).getOrElse(0.0)
          var startIndex = 5

          if (name == "Exam") {
            startIndex += 3
          }

          val cell = row.createCell(i + startIndex)
          cell.setCellValue(mark)
          cell.setCellStyle(doubleStyle)
        }
      }
    }

    // Write the output to a file
    val fileOut = new FileOutputStream(filename)
    wb.write(fileOut);
  }

}
