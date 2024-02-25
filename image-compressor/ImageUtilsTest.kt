package com.equiruswealth.commons.utils

import com.equiruswealth.commons.TestConstants
import com.equiruswealth.commons.constants.Constant
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * @author Shashwat Singh
 */

class ImageUtilsTest {

  @Test
  fun getImageMetaData_errorImageNotFound() {
    Assertions.assertThatThrownBy {
      ImageUtils.getImageMetaData(File(""))
    }.hasMessage(Constant.FileProcessingExceptions.FILE_NOT_FOUND)
  }

  @Test
  fun getImageMetaData_notAbleToParseMetaData() {
    Assertions.assertThatThrownBy {
      ImageUtils.getImageMetaData(
        File(this.javaClass.classLoader.getResource(
          TestConstants.SheetUtilTestFiles.excelTest)!!.path))
    }.hasMessage(Constant.ImageProcessingException.NOT_ABLE_PARSE_METADATA)
  }

  @Test
  fun getImageMetaData_success() {
    val metadata =
      ImageUtils.getImageMetaData(
        File(this.javaClass.classLoader.getResource(
          TestConstants.ImageForTest.file94kb_878X764)!!.path))

    Assertions.assertThat(metadata.height).isEqualTo(878)
    Assertions.assertThat(metadata.width).isEqualTo(764)
  }

  @Test
  fun compressByResolution_originalFileNotFound() {
    Assertions.assertThatThrownBy {
      ImageUtils.compressByResolution(
        File(""), 100, 200)
    }.hasMessage(Constant.FileProcessingExceptions.FILE_NOT_FOUND)
  }

  @Test
  fun compressByQuality_crossedThresholdQuality() {
    val inputFile = File(this.javaClass.classLoader.getResource(TestConstants.ImageForTest.oneMbSizeImage)!!.path)
    Assertions.assertThatThrownBy {
      ImageUtils.compressByQuality(
       inputFile, 200, 0.6f)
    }.hasMessageStartingWith(trimTillPercentageS(Constant.ImageProcessingException.CROSSES_REQUIRED_QUALITY))
  }

  @Test
  fun compressByQuality_fileSizeIncreases() {
    val inputFile = File(this.javaClass.classLoader.getResource(TestConstants.ImageForTest.checkMinimumQuality)!!.path)
    Assertions.assertThatThrownBy {
      ImageUtils.compressByQuality(
        inputFile, 5, 0.7f)
    }.hasMessageStartingWith(trimTillPercentageS(Constant.ImageProcessingException.FILE_SIZE_INCREASES_BY_DECREASING_QUALITY))
  }


  @Test
  fun resizeImage_receivedInvalidFile() {
    val file = File(this.javaClass.classLoader.getResource(TestConstants.SheetUtilTestFiles.excelTest)!!.path)
    Assertions.assertThatThrownBy {
      ImageUtils.resizeImage(file, 2.0, 0.5f, 1000)
    }.hasMessage(Constant.ImageProcessingException.NOT_ABLE_PARSE_METADATA)
  }

  @Test
  fun resizeImage_forSmallFileToSkipCompressions() {
    val file =  File(this.javaClass.classLoader.getResource(TestConstants.ImageForTest.file94kb_878X764)!!.path)
    Assertions.assertThatThrownBy {
      ImageUtils.resizeImage(file, 200.0, 0.5f, 1000).lengthInKb()
    }.hasMessage(Constant.ImageProcessingException.FILE_UNDER_MAX_REQUIRED_SIZE)
  }


  @Test
  fun resizeImage_forSamsungImageCompression() {
    val file =  File(this.javaClass.classLoader.getResource(TestConstants.ImageForTest.samsung)!!.path)
    Assertions.assertThat(
      ImageUtils.resizeImage(file, 200.0, 0.5f, 1000).lengthInKb()
    ).isLessThan(200.0)
  }


  @Test
  fun resizeImage_forOneplusImageCompression() {
    val file =  File(this.javaClass.classLoader.getResource(TestConstants.ImageForTest.onePlus)!!.path)
    Assertions.assertThat(
      ImageUtils.resizeImage(file, 200.0, 0.5f, 1000).lengthInKb()
    ).isLessThan(200.0)
  }

  @Test
  fun resizeImage_givenFileAlreadyUnderRequiredSize() {
    val file =  File(this.javaClass.classLoader.getResource(TestConstants.ImageForTest.invalidFormat)!!.path)
    Assertions.assertThatThrownBy {
      ImageUtils.resizeImage(file, 200.0, 0.5f, 1000).lengthInKb()
    }.hasMessage(Constant.ImageProcessingException.FILE_UNDER_MAX_REQUIRED_SIZE)
  }

  @Test
  fun resizeImage_compressionByQualityApplied() {
    val file = File(this.javaClass.classLoader.getResource(TestConstants.ImageForTest.qualityWiseCompressRequired)!!.path)
    Assertions.assertThat(
      ImageUtils.resizeImage(file, 200.0, 0.5f, 1000).lengthInKb()
    ).isLessThan(200.0)
  }

  private fun trimTillPercentageS(message: String): String {
    return message.substring(0, message.indexOf("%s"))
  }
}
