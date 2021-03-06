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
package com.linkedin.photon.ml.diagnostics.reporting

/**
 * Denotes a class capable of transforming a particular kind of logical report into a particular kind of
 * physical report.
 *
 * @tparam L
 *           Logical report type
 * @tparam P
 *           Physical report type
 */
trait LogicalToPhysicalReportTransformer[-L <: LogicalReport, +P <:PhysicalReport] {
  def transform(logical:L):P
}
