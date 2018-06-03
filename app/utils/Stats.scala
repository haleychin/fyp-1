package utils

import scala.math._
import breeze.stats._

case class DescriptiveStatistic(
  max: Double = 0.0,
  mean: Double = 0.0,
  median: Double = 0.0,
  mode: Double = 0.0,
  multiMode: Map[Double, Int] = Map[Double, Int](),
  standardDeviation: Double = 0.0,
  variance: Double = 0.0
)

object Stats {

  def computeDescriptiveStatistic(values: Seq[Double]): DescriptiveStatistic = {
    if (values.isEmpty) {
      DescriptiveStatistic()
    }  else {
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

}
