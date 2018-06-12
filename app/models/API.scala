package models

import scala.collection.mutable.{Map,ArrayBuffer,LinkedHashMap,LinkedHashSet}
import java.sql.Date

case class CourseAPI(
  course: Option[Course],
  students: Seq[Student],
  attendance: AttendanceAPI,
  coursework: CourseworkAPI,
  exam: ExamAPI,
  programmeToIntake: Map[String,String])
case class StudentAPI(
  student: Option[Student],
  courses: Seq[Course],
  attendances: CAttendanceAPI,
  courseworks: CCourseworkAPI,
  exams: CExamAPI)

case class CAttendanceAPI(
  studentDetails: LinkedHashMap[Long,CourseDetailsAPI])
case class CourseDetailsAPI(
  coures: Course,
  var attendances: LinkedHashMap[Date,String],
  var attendanceRate: Double)
case class Insight(
  dangerLevel: Int = 0,
  reasons: Iterable[String] = Array[String]()
)
case class AStat(
  absent: Int = 0,
  attend: Int = 0,
  attendanceRate: Double = 0.0,
  consecutiveMissed: ArrayBuffer[ArrayBuffer[Date]] = ArrayBuffer[ArrayBuffer[Date]]()
)

case class CourseCwAPI(
  course: Option[Course],
  students: Seq[Student],
  courseworks: CourseworkAPI)
case class AttendanceAPI(
  studentDetails: LinkedHashMap[Long,StudentDetailsAPI],
  dates: LinkedHashSet[(Int, Date)])
case class StudentDetailsAPI(
  student: Student,
  var attendances: LinkedHashMap[Date,String],
  var stat: AStat = AStat(),
  var insight: Insight = Insight())
// Return a map of Course Id -> Courseworks Details
case class CCourseworkAPI(
  courseworkDetails: LinkedHashMap[Long,CCourseworkDetailsAPI])
case class CCourseworkDetailsAPI(
  course: Course,
  var courseworks: LinkedHashMap[String, Double],
  var totalMark: Double,
  var fullMark: Double,
  var status: String)

case class CExamAPI(
  examDetails: LinkedHashMap[Long,CExamDetailsAPI])
case class CExamDetailsAPI(
  course: Course,
  // (Total, Weightage, Pass/Fail)
  var exam: (Double, Double, String),
  fullMark: Int,
  fullWeightage: Int)

case class DescriptiveStatistic(
  max: Double = 0.0,
  min: Double = 0.0,
  mean: Double = 0.0,
  median: Double = 0.0,
  mode: Double = 0.0,
  standardDeviation: Double = 0.0,
  variance: Double = 0.0
)
case class Statistic(
  averageMark: Double,
  averageWeightage: Double,
  passCount: Int,
  failCount: Int)
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

case class QuestionStat(
  var total: Double,
  var maxMark: Double,
  var average: Double = 0,
  var frequency: Int = 1,
  var percentage: Double = 0
)

case class MetricStat(
  var total: Double,
  var maxMark: Double,
  var questions: LinkedHashMap[String,QuestionStat] = LinkedHashMap[String,QuestionStat](),
  var description: String,
  var average: Double = 0,
  var percentage: Double = 0,
  var frequency: Int = 1
)
