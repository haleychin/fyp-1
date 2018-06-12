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

// =================
// StudentController
// =================
case class StudentAPI(
  student: Option[Student],
  courses: Seq[Course],
  attendances: CAttendanceAPI,
  courseworks: CCourseworkAPI,
  exams: CExamAPI)

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
  courseworkDetails: LinkedHashMap[Long,CCourseworkDetailsAPI])
case class CCourseworkDetailsAPI(
  course: Course,
  var courseworks: LinkedHashMap[String, Double],
  var totalMark: Double,
  var fullMark: Double,
  var status: String)

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


// =================
// Statistic related
// =================

// Insight - used to identify risky student
case class Insight(
  dangerLevel: Int = 0,
  reasons: Iterable[String] = Array[String]()
)

// Descriptive Statistic for Course Overview
// Exam and Coursework
case class DescriptiveStatistic(
  max: Double = 0.0,
  min: Double = 0.0,
  mean: Double = 0.0,
  median: Double = 0.0,
  mode: Double = 0.0,
  standardDeviation: Double = 0.0,
  variance: Double = 0.0
)

// Grade/Status for Exam and Coursework
case class Status(name: String = "-", reason: String = "")

// Overall Coursework Statistic
case class CwStatistic(
  averages: LinkedHashMap[String, Double],
  passCount: Int,
  failCount: Int,
  var gradeFrequency: LinkedHashMap[String,Int] = LinkedHashMap[String,Int]())

// Overall Exam Statistic
case class Statistic(
  averageMark: Double,
  averageWeightage: Double,
  passCount: Int,
  failCount: Int)

// Attendance Statistic
case class AStat(
  absent: Int = 0,
  attend: Int = 0,
  attendanceRate: Double = 0.0,
  consecutiveMissed: ArrayBuffer[ArrayBuffer[Date]] = ArrayBuffer[ArrayBuffer[Date]]()
)

// Question Statistic
case class QuestionStat(
  var total: Double,
  var maxMark: Double,
  var average: Double = 0,
  var frequency: Int = 1,
  var percentage: Double = 0
)

// Metric Statistic
case class MetricStat(
  var total: Double,
  var maxMark: Double,
  var questions: LinkedHashMap[String,QuestionStat] = LinkedHashMap[String,QuestionStat](),
  var description: String,
  var average: Double = 0,
  var percentage: Double = 0,
  var frequency: Int = 1
)
