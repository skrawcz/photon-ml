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
package com.linkedin.photon.ml.estimators

import scala.math.log

import java.util.Random

import breeze.linalg.DenseVector
import org.apache.spark.sql.DataFrame
import org.mockito.Mockito._
import org.testng.Assert._
import org.testng.annotations.{DataProvider, Test}

import com.linkedin.photon.ml.constants.MathConst
import com.linkedin.photon.ml.estimators.GameEstimator.GameOptimizationConfiguration
import com.linkedin.photon.ml.optimization._
import com.linkedin.photon.ml.optimization.game._
import com.linkedin.photon.ml.test.Assertions.assertIterableEqualsWithTolerance

/**
 * Unit tests for [[GameEstimatorEvaluationFunction]].
 */
class GameEstimatorEvaluationFunctionTest {

  private val mockOptimizerConfig = mock(classOf[OptimizerConfig])
  private val mockRegContext = mock(classOf[RegularizationContext])
  private val mockEstimator = mock(classOf[GameEstimator])
  private val mockData = mock(classOf[DataFrame])

  private val random = new Random(1)
  private val regWeights = Array.fill[Double](4) { random.nextDouble }
  private val tolerance = MathConst.EPSILON

  /**
   * Test that a [[GameOptimizationConfiguration]] can be correctly constructed from a hyperparameter vector.
   */
  @Test
  def testVectorToConfiguration(): Unit = {

    val configuration: GameOptimizationConfiguration = Map(
      ("a", FixedEffectOptimizationConfiguration(mockOptimizerConfig, mockRegContext, regWeights(0))),
      ("b", RandomEffectOptimizationConfiguration(mockOptimizerConfig, mockRegContext, regWeights(1))))

    val evaluationFunction = new GameEstimatorEvaluationFunction(mockEstimator, configuration, mockData, mockData)
    val hypers = DenseVector(log(regWeights(2)), log(regWeights(3)))
    val newConfiguration = evaluationFunction.vectorToConfiguration(hypers)

    assertEquals(
      newConfiguration("a").asInstanceOf[FixedEffectOptimizationConfiguration].regularizationWeight,
      regWeights(2),
      tolerance)
    assertEquals(
      newConfiguration("b").asInstanceOf[RandomEffectOptimizationConfiguration].regularizationWeight,
      regWeights(3),
      tolerance)
  }

  @DataProvider
  def invalidVectorProvider(): Array[Array[Any]] = {

    val configuration: GameOptimizationConfiguration = Map(
      ("a", FixedEffectOptimizationConfiguration(mockOptimizerConfig, mockRegContext, regWeights(0))),
      ("b", RandomEffectOptimizationConfiguration(mockOptimizerConfig, mockRegContext, regWeights(1))))

    Array(
      Array(configuration, DenseVector(log(regWeights(0)))),
      Array(configuration, DenseVector(log(regWeights(0)), log(regWeights(1)), log(regWeights(2)))))
  }

  /**
   * Test that errors caused by invalid vectors will be caught when attempting to construct a
   * [[GameOptimizationConfiguration]].
   *
   * @param config The base configuration to use as a template
   * @param hypers The hyperparameter vector
   */
  @Test(dataProvider = "invalidVectorProvider", expectedExceptions = Array(classOf[IllegalArgumentException]))
  def testInvalidVectorToConfiguration(config: GameOptimizationConfiguration, hypers: DenseVector[Double]): Unit = {

    val evaluationFunction = new GameEstimatorEvaluationFunction(mockEstimator, config, mockData, mockData)
    evaluationFunction.vectorToConfiguration(hypers)
  }

  @DataProvider
  def configurationProvider: Array[Array[Any]] =
    Array(
      Array(
        Map(("a", FixedEffectOptimizationConfiguration(mockOptimizerConfig, mockRegContext, regWeights(0)))),
        DenseVector(log(regWeights(0)))),
      Array(
        Map(("b", RandomEffectOptimizationConfiguration(mockOptimizerConfig, mockRegContext, regWeights(1)))),
        DenseVector(log(regWeights(1)))),
      Array(
        Map(
          ("a", FixedEffectOptimizationConfiguration(mockOptimizerConfig, mockRegContext, regWeights(0))),
          ("b", RandomEffectOptimizationConfiguration(mockOptimizerConfig, mockRegContext, regWeights(1)))),
        DenseVector(log(regWeights(0)), log(regWeights(1)))))

  /**
   * Test that a [[GameOptimizationConfiguration]] can be correctly converted to a hyperparameter vector.
   *
   * @param config The base configuration to use as a template
   * @param expected The expected hyperparameter vector for the configuration
   */
  @Test(dataProvider = "configurationProvider")
  def testConfigurationToVector(config: GameOptimizationConfiguration, expected: DenseVector[Double]): Unit = {

    val evaluationFunction = new GameEstimatorEvaluationFunction(mockEstimator, config, mockData, mockData)
    val result = evaluationFunction.configurationToVector(config)

    assertEquals(result.length, expected.length)
    assertIterableEqualsWithTolerance(result.toArray, expected.toArray, tolerance)
  }

  @DataProvider
  def invalidConfigurationProvider: Array[Array[Any]] = {

    val configuration1: GameOptimizationConfiguration = Map(
      ("a", FixedEffectOptimizationConfiguration(mockOptimizerConfig, mockRegContext, regWeights(0))))
    val configuration2: GameOptimizationConfiguration = Map(
      ("a", FixedEffectOptimizationConfiguration(mockOptimizerConfig, mockRegContext, regWeights(0))),
      ("b", RandomEffectOptimizationConfiguration(mockOptimizerConfig, mockRegContext, regWeights(1))))
    val configuration3: GameOptimizationConfiguration = Map(
      ("a", FixedEffectOptimizationConfiguration(mockOptimizerConfig, mockRegContext, regWeights(0))),
      ("c", RandomEffectOptimizationConfiguration(mockOptimizerConfig, mockRegContext, regWeights(1))))
    val configuration4: GameOptimizationConfiguration = Map(
      ("a", RandomEffectOptimizationConfiguration(mockOptimizerConfig, mockRegContext, regWeights(0))),
      ("c", RandomEffectOptimizationConfiguration(mockOptimizerConfig, mockRegContext, regWeights(1))))

    Array(
      // Configuration dimension size mismatch
      Array(configuration1, configuration2),
      Array(configuration2, configuration1),

      // Configuration coordinate mismatch
      Array(configuration2, configuration3),

      // Configuration coordinate type mismatch
      Array(configuration3, configuration4))
  }

  /**
   * Test that [[GameOptimizationConfiguration]] instances which do not match the template configuration will be caught
   * and throw error when attempting to construct a hyperparameter vector.
   *
   * @param baseConfig The base configuration to use as a template
   * @param vectorConfig A dissimilar configuration from which to attempt to construct a hyperparameter vector
   */
  @Test(dataProvider = "invalidConfigurationProvider", expectedExceptions = Array(classOf[IllegalArgumentException]))
  def testConfigurationToVector(
    baseConfig: GameOptimizationConfiguration,
    vectorConfig: GameOptimizationConfiguration): Unit = {

    val evaluationFunction = new GameEstimatorEvaluationFunction(mockEstimator, baseConfig, mockData, mockData)
    evaluationFunction.configurationToVector(vectorConfig)
  }
}
