package models
import scala.collection.mutable.{ArrayBuffer,LinkedHashMap}
import java.sql.Date

// Insight - used to identify risky student
case class Insight(
  dangerLevel: Double = 0.0,
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
  var failCount: Int,
  var gradeFrequency: LinkedHashMap[String,Int] = LinkedHashMap[String,Int]())

// Overall Exam Statistic
case class Statistic(
  averageMark: Double,
  averageWeightage: Double,
  passCount: Int,
  failCount: Int,
  var gradeFrequency: LinkedHashMap[String,Int] = LinkedHashMap[String,Int]())


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
  var fullMark: Double,
  var average: Double = 0,
  var frequency: Int = 1,
  var percentage: Double = 0
)

// Metric Statistic
case class MetricStat(
  var total: Double,
  var maxMark: Double,
  var fullMark: Double,
  var questions: LinkedHashMap[String,QuestionStat] = LinkedHashMap[String,QuestionStat](),
  var description: String,
  var average: Double = 0,
  var percentage: Double = 0,
  var frequency: Int = 1
)
