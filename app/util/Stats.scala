package utils

import scala.math._
import breeze.stats._

case class DescriptiveStatistic(
  max: Double,
  mean: Double,
  median: Double,
  mode: Double,
  multiMode: Map[Double, Int],
  standardDeviation: Double,
  variance: Double
)

object Stats {

  def computeDescriptiveStatistic(values: Seq[Double]): DescriptiveStatistic = {
    val total = values.sum
    val mav  = meanAndVariance(values)
    val max = values.max
    val min = values.min

    val frequencies = values.groupBy(x => x).map(x => (x._1, x._2.size))
    val modeFrequency = frequencies.map(_._2).max
    val multiMode = frequencies.filter(x => x._2 == modeFrequency)
    val modeResult = mode(values)

    val medianValue = median(values)
    val standardDeviation = sqrt(mav.variance)

    DescriptiveStatistic(
      max,
      mav.mean,
      medianValue,
      modeResult.mode,
      multiMode,
      standardDeviation,
      mav.variance)
  }

}
