package com.mybraintech.sdk.core.acquisition.eeg.signalprocessing

import com.mybraintech.android.jnibrainbox.QualityChecker

class EEGQualityProcessor(
  private val sampleRate: Int = 250
) {

  private val qualityChecker: QualityChecker = QualityChecker(sampleRate)

  val qualityCheckerVersion: String
  get() {
//    qualityChecker.
    return "0.0.0"
  }

  fun computeQualityValue(buffer: ArrayList<ArrayList<Float>>): FloatArray {
    return qualityChecker.computeQualityChecker(buffer)
  }

}