package utils

import scala.math._
import breeze.stats._
import models.DescriptiveStatistic

object Stats {
  def computeDescriptiveStatistic(values: Seq[Double]): DescriptiveStatistic = {
    if (values.isEmpty) {
      DescriptiveStatistic()
    }  else {
      val total = values.sum
      val mav  = meanAndVariance(values)
      val max = values.max
      val min = values.min

      val modeResult = mode(values)
      val medianValue = median(values)
      val standardDeviation = sqrt(mav.variance)

      DescriptiveStatistic(
        max,
        min,
        mav.mean,
        medianValue,
        modeResult.mode,
        standardDeviation,
        mav.variance)
    }
  }

}
