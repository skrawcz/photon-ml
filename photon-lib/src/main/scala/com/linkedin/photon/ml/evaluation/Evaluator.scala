/*
 * Copyright 2017 LinkedIn Corp. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.linkedin.photon.ml.evaluation

import java.util.Objects

import org.apache.spark.rdd.RDD

import com.linkedin.photon.ml.Types.UniqueSampleId

/**
 * An interface for evaluation implementations at the [[RDD]] level.
 */
trait Evaluator {

  val defaultScore: Double = 0.0
  val evaluatorType: EvaluatorType

  protected[ml] val labelAndOffsetAndWeights: RDD[(UniqueSampleId, (Double, Double, Double))]

  /**
   * Evaluate the scores of the model.
   *
   * @param scores The scores to evaluate
   * @return An evaluation metric value
   */
  protected[ml] def evaluate(scores: RDD[(UniqueSampleId, Double)]): Double = {
    // Create a local copy of the defaultScore, so that the underlying object won't get shipped to the executor nodes
    val defaultScore = this.defaultScore
    val scoreAndLabelAndWeights = scores
      .rightOuterJoin(labelAndOffsetAndWeights)
      .mapValues { case (scoredDatumOption, (label, offset, weight)) =>
        val score = scoredDatumOption.getOrElse(defaultScore)
        (score + offset, label, weight)
      }
    evaluateWithScoresAndLabelsAndWeights(scoreAndLabelAndWeights)
  }

  /**
   * Evaluate scores with labels and weights.
   *
   * @param scoresAndLabelsAndWeights A [[RDD]] of (uniqueId, (score, label, weight)) pairs
   * @return An evaluation metric value
   */
  protected[ml] def evaluateWithScoresAndLabelsAndWeights(
    scoresAndLabelsAndWeights: RDD[(UniqueSampleId, (Double, Double, Double))]): Double

  /**
   * Determine the better between two scores returned by this [[Evaluator]]. In some cases, the better score is higher
   * (e.g. AUC) and in others, the better score is lower (e.g. RMSE).
   *
   * @param score1 The first score to compare
   * @param score2 The second score to compare
   * @return True if the first score is better than the second, false otherwise
   */
  def betterThan(score1: Double, score2: Double): Boolean

  /**
   * Get the name of this [[Evaluator]] object.
   *
   * @return The name of this [[Evaluator]].
   */
  def getEvaluatorName: String = evaluatorType.name

  /**
   * Compares two [[Evaluator]] objects.
   *
   * @param other Some other object
   * @return True if the both models conform to the equality contract and have the same model coefficients, false
   *         otherwise
   */
  override def equals(other: Any): Boolean = other match {
    case that: Evaluator =>
      (this.evaluatorType == that.evaluatorType) && (this.labelAndOffsetAndWeights == that.labelAndOffsetAndWeights)

    case _ => false
  }

  /**
   * Returns a hash code value for the object.
   *
   * @return An [[Int]] hash code
   */
  override def hashCode: Int = Objects.hash(evaluatorType, labelAndOffsetAndWeights)
}

object Evaluator {
  type EvaluationResults = Seq[(Evaluator, Double)]
}
