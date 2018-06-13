package models

import scala.collection.mutable.{Map,ArrayBuffer,LinkedHashMap,LinkedHashSet}
import java.sql.Date

// =================
// StudentController
// =================
case class StudentAPI(
  student: Option[Student],
  courses: Seq[Course],
  attendances: CAttendanceAPI,
  courseworks: CCourseworkAPI)

// ----------
// Attendance
// ----------
case class CAttendanceAPI(
  studentDetails: LinkedHashMap[Long,CourseDetailsAPI])
case class CourseDetailsAPI(
  coures: Course,
  var attendances: LinkedHashMap[Date,String],
  var attendanceRate: Double)

// ----------
// Coursework
// ----------
case class CCourseworkAPI(
  courseworkDetails: LinkedHashMap[Long,CCourseworkDetailsAPI],
  statistic: CwStatistic,
  var descStat: DescriptiveStatistic)
case class CCourseworkDetailsAPI(
  course: Course,
  var courseworks: LinkedHashMap[String, Double],
  var totalMark: Double,
  var fullMark: Double,
  var status: String,
  var grade: Status = Status())

// ----
// Exam
// ----
case class CExamAPI(
  examDetails: LinkedHashMap[Long,CExamDetailsAPI])
case class CExamDetailsAPI(
  course: Course,
  // (Total, Weightage, Pass/Fail)
  var exam: (Double, Double, String),
  fullMark: Int,
  fullWeightage: Int)


