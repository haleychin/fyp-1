package models

import scala.collection.mutable.{Map,ArrayBuffer,LinkedHashMap,LinkedHashSet}
import java.sql.Date

// ================
// CourseController
// ================
case class CourseAPI(
  course: Option[Course],
  students: Seq[Student],
  attendance: AttendanceAPI,
  coursework: CourseworkAPI,
  exam: ExamAPI,
  programmeToIntake: Map[String,String])

// ----------
// Attendance
// ----------
case class AttendanceAPI(
  studentDetails: LinkedHashMap[Long,StudentDetailsAPI],
  dates: LinkedHashSet[(Int, Date)])
case class StudentDetailsAPI(
  student: Student,
  var attendances: LinkedHashMap[Date,String],
  var stat: AStat = AStat(),
  var insight: Insight = Insight())

// ----------
// Coursework
// ----------
case class CourseCwAPI(
  course: Option[Course],
  students: Seq[Student],
  courseworks: CourseworkAPI)
case class CourseworkAPI(
  courseworkDetails: LinkedHashMap[Long,CourseworkDetailsAPI],
  courseworks: LinkedHashSet[(String, Double)],
  var total: Double, // Maximum mark for the total of courseworks
  statistic: CwStatistic,
  var descStat: DescriptiveStatistic)
case class CourseworkDetailsAPI(
  student: Student,
  var courseworks: LinkedHashMap[String, Double],
  var courseworksTotal: LinkedHashMap[String,Double],
  var total: Double,
  var status: String = "",
  var grade: Status = Status(),
  var insight: Insight = Insight())

// ----
// Exam
// ----
case class ExamAPI(
  examDetails: LinkedHashMap[Long,ExamDetailsAPI],
  total: Int,
  weightage: Int,
  statistic: Statistic,
  descStat: DescriptiveStatistic)
case class ExamDetailsAPI(
  student: Student,
  // (Total, Weightage, Pass/Fail)
  var exam: (Double, Double, String))
case class ExamMetricAPI(
  course: Option[Course],
  students: Seq[Student],
  exams: ExamAPI,
  metrics: LinkedHashMap[String,MetricStat]
)

