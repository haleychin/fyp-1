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

    // =====
    // Style
    // =====

    // Bold Style
    val boldStyle = wb.createCellStyle();
    val font = wb.createFont();
    font.setBold(true);
    boldStyle.setFont(font);

    // Center Style
    val centerStyle = wb.createCellStyle();
    val halign = HorizontalAlignment.CENTER
    val valign = VerticalAlignment.CENTER
    centerStyle.setAlignment(halign);
    centerStyle.setVerticalAlignment(valign);


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
    cwBdCell.setCellStyle(boldStyle)
    cwBdCell.setCellStyle(centerStyle);
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
    cwCell.setCellStyle(boldStyle)
    cwCell.setCellStyle(centerStyle);

    cw2Cell.setCellValue(
      createHelper.createRichTextString("Total CW")
    )
    cw2Cell.setCellStyle(boldStyle)
    cw2Cell.setCellStyle(centerStyle);

    exam2Cell.setCellValue(
      createHelper.createRichTextString("Exam")
    )
    exam2Cell.setCellStyle(boldStyle)
    exam2Cell.setCellStyle(centerStyle);
    examCell.setCellValue(
      createHelper.createRichTextString("Exam")
    )
    examCell.setCellStyle(boldStyle)
    examCell.setCellStyle(centerStyle);

    totalCell.setCellValue(
      createHelper.createRichTextString("Total")
    )
    totalCell.setCellStyle(boldStyle)
    totalCell.setCellStyle(centerStyle);

    gradeCell.setCellValue(
      createHelper.createRichTextString("Grade")
    )
    gradeCell.setCellStyle(boldStyle)
    gradeCell.setCellStyle(centerStyle);

    remarkCell.setCellValue(
      createHelper.createRichTextString("Remark")
    )
    remarkCell.setCellStyle(boldStyle)
    remarkCell.setCellStyle(centerStyle);

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
    }

    // Write the output to a file
    val fileOut = new FileOutputStream(filename)
    wb.write(fileOut);
  }

}
