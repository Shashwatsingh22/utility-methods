package com.equiruswealth.commons.utils

import com.equiruswealth.commons.constants.Constant
import org.apache.commons.lang3.time.StopWatch
import org.slf4j.LoggerFactory
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/**
 * @author Shashwat Singh
 * Utils helps resize the given image file by resolution and quality
 */
object ImageUtils {

  private var log = LoggerFactory.getLogger(this::class.java)

  /**
   * This method helps parse the metaData given file
   *
   * @param file For which metaData required
   *
   * @return Mutable of MetaData which contains currently width and height
   *
   * @throws ImageProcessingException
   *  - If ImageIO not able to parse metaData
   *  - file not found
   *  - If an error occurs during reading or when not able to create required ImageInputStream.
   *
   */
  fun getImageMetaData(file: File): ImageMetaData {
    try {
      file.inputStream().use {
        val image = ImageIO.read(file)

        if (image == null) {
          log.info("getImageMetaData - Not able to parse metadata for given image file <${file.path}>")
          throw ImageProcessingException(Constant.ImageProcessingException.NOT_ABLE_PARSE_METADATA)
        }

        log.info("getImageMetaData - Successfully parsed the metaData for given file <${file.path}>")
        return ImageMetaData().apply {
          this.height = image.height
          this.width = image.width
        }
      }
    } catch (e: FileNotFoundException) {
      log.error(
        "getImageMetaData - Given invalid path <${file.path}>, where file not found."
      )
      throw ImageProcessingException(Constant.FileProcessingExceptions.FILE_NOT_FOUND)
    } catch (e: IOException) {
      log.error(
        "getImageMetaData - Given invalid file <${file.name}>"
      )
      throw ImageProcessingException(Constant.FileProcessingExceptions.INVALID_FILE_RECEIVED)
    }
  }

  /**
   * This method helps to compress the given image first by resolution if still not achieved the required
   * maxRequired size the compressByQuality
   *
   * @param maxRequiredSize to maintain the threshold for the size of image
   * @param minRequiredQuality to maintain the threshold for the quality of image
   * @param maxRequiredResolution  to maintain the threshold for the resolution of image
   *
   * It first, compress image by the resolution then check the maxRequired size not achieved then
   * start compressing by quality of image.
   *
   * @throws ImageProcessingException
   * - MinRequiredQuality crosses but still not achieve the targetSize
   * - FileSize increases by decreasing quality of image
   * - Either originalImage or outputFile file not found
   * - Error occurs during writing or when not able to create required ImageOutputStream.
   * - If ImageIO not able to parse metaData
   * - If an error occurs during reading or when not able to create required ImageInputStream.
   * - File already under maximum required size.
   */
  fun resizeImage(
    originalImage: File,
    maxRequiredSize: Double,
    minRequiredQuality: Float,
    maxRequiredResolution: Int
  ): File {
    //Given file already under the maxRequiredSize
    if (originalImage.lengthInKb() <= maxRequiredSize) {
      log.info(
        "resizeImage - Original file <${originalImage.name}> whose size <${originalImage.lengthInKb()} kb> already " +
            " under size <${maxRequiredSize}>"
      )
      throw ImageProcessingException(Constant.ImageProcessingException.FILE_UNDER_MAX_REQUIRED_SIZE)
    }

    val timer = StopWatch()
    timer.start()

    log.debug(
      "resizeImage - File <${originalImage.path}> whose size is " +
          " <${originalImage.lengthInKb()} kb> given to resize it to <${maxRequiredSize} kb>."
    )

    val metadata = getImageMetaData(originalImage)

    val initialWidth = metadata.width
    val initialHeight = metadata.height
    var fileSize = originalImage.lengthInKb()

    var resizedImage: File? = null

    if (initialWidth == null || initialHeight == null) {
      log.info(
        "resizeImage - Not able to fetch resolution width or height from " +
            " given image file <${originalImage.path}>"
      )
      throw ImageProcessingException(Constant.ImageProcessingException.NOT_ABLE_PARSE_METADATA)
    }

    val max = maxOf(initialWidth, initialHeight)

    if (max > maxRequiredResolution) {
      //Resizing by Height and Width
      val requiredResolution = max / maxRequiredResolution

      val finalWidth = initialWidth / requiredResolution
      val finalHeight = initialHeight / requiredResolution
      resizedImage = compressByResolution(originalImage, finalWidth, finalHeight)
      fileSize = resizedImage.lengthInKb()
    }

    //This check required here, in case if given image file already under the minRequiredResolution
    // so, in that case compression by resolution step get skipped, then our resizedImage file size will be zero
    val fileForOptimisationByQuality = resizedImage ?: originalImage

    //Optimisation by quality
    if (fileSize > maxRequiredSize) {
      resizedImage = compressByQuality(fileForOptimisationByQuality, maxRequiredSize.toInt(), minRequiredQuality)
    }
    //Stop timer
    timer.stop()

    log.info(
      "resizeImage - Original file <${originalImage.name}> whose size <${originalImage.lengthInKb()} kb> " +
          " resized to <${resizedImage?.lengthInKb()} kb> output file <${resizedImage?.name}>, in time <$timer>"
    )
    return resizedImage!!
  }

  /**
   * It helps to compress image by given target width and height
   *
   * @param originalImage  image file which required to resized
   * @param targetWidth  image resized to given target width
   * @param targetHeight image resized to given target height
   *
   * @return resizedImage (return the resized file which is compressed by resolution.)
   *
   * @throws ImageProcessingException
   * - Occurs when fileNot found at the given place (either original image or resized image)
   * - Occurs when during writing or not able to create the output stream
   */
  fun compressByResolution(originalImage: File, targetWidth: Int, targetHeight: Int): File {
    log.debug(
      "compressByResolution - File <${originalImage.path}> whose size is <${originalImage.lengthInKb()} kb> " +
          " given to compress by resolution whose targetWidth <$targetWidth> and targetHeight <$targetHeight>."
    )
    try {
      val resizedImage = File.createTempFile(
        StringUtil.getSecureRandomString(Constant.RANDOM_FILE_NAME_LENGTH),
        "_output_${originalImage.nameWithoutExtension}.jpg"
      )

      val resizedImageBufferedImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)

      originalImage.inputStream().use { inputStream ->
        val g2d: Graphics2D = resizedImageBufferedImage.createGraphics()
        g2d.drawImage(ImageIO.read(inputStream), 0, 0, targetWidth, targetHeight, null)
        g2d.dispose()
      }

      resizedImage.outputStream().use { resizedOutputStream ->
        ImageIO.write(resizedImageBufferedImage, Constant.ImageMetaData.jpeg, resizedOutputStream)
      }

      log.info(
        "compressByResolution - Image file resized to <${originalImage.path}> to required resolution " +
            " initialFileSize: <${originalImage.lengthInKb()} kb> , output <${resizedImage.path}> " +
            " after compress by resolution finalSize: <${resizedImage.lengthInKb()} kb>."
      )
      return resizedImage
    } catch (e: FileNotFoundException) {
      log.error(
        "compressByResolution - Given invalid path input file <${originalImage.path}> where file not found."
      )
      throw ImageProcessingException(Constant.FileProcessingExceptions.FILE_NOT_FOUND)

    } catch (e: IOException) {
      log.error(
        "compressByResolution - Error occurs during writing or when not able to create required" +
            "  ImageOutputStream for input file <${originalImage.name}>"
      )
      throw ImageProcessingException(Constant.ImageProcessingException.ERROR_OCCURS_ON_WRITING)
    }
  }

  /**
   * It helps to compress image by quality it decreases the quality of image by 0.1f
   * till required size.
   *
   * @param originalImage image file which required to resized
   * @param targetSize image resized under given target size
   * @param minRequiredQuality
   *
   * @return outputFile (return outputFile which is compressed by quality)
   *
   * @throws ImageProcessingException
   * - MinRequiredQuality crosses but still not achieve the targetSize
   * - FileSize increases by decreasing quality of image
   * - Either originalImage or outputFile file not found
   * - Error occurs during writing or when not able to create required ImageOutputStream.
   */
  fun compressByQuality(originalImage: File, targetSize: Int, minRequiredQuality: Float): File {
    log.debug(
      "compressByQuality - File <${originalImage.path}> whose size is <${originalImage.lengthInKb()}> Kb" +
          " given to compress by quality whose targetSize <$targetSize> kb."
    )

    val outputFile = File.createTempFile(
      StringUtil.getSecureRandomString(Constant.RANDOM_FILE_NAME_LENGTH),
      "_output_${originalImage.nameWithoutExtension}.jpg"
    )

    var currentFileSize = originalImage.lengthInKb()
    var quality = Constant.ImageMetaData.maxQuality

    //Optimisation by quality
    while (currentFileSize > targetSize) {

      if (quality <= minRequiredQuality) {
        log.info(
          "compressByQuality - Given File <${originalImage.path}> which crosses the minRequiredQuality <$minRequiredQuality> currentQuality <$quality>" +
              " and we not able to achieve the target size <${targetSize} kb> currentSize <${currentFileSize} kb>"
        )
        throw ImageProcessingException(
          Constant.ImageProcessingException.CROSSES_REQUIRED_QUALITY.format(
            targetSize,
            currentFileSize,
            minRequiredQuality,
            quality
          )
        )
      }

      //Decrease quality
      quality -= 0.1f
      quality = NumberUtils.limitDecimalPlaces(quality.toDouble()).toFloat()

      try {
        val imageWriter = ImageIO.getImageWritersByFormatName(Constant.ImageMetaData.jpeg).next()
        val imageWriteParam: ImageWriteParam = imageWriter.defaultWriteParam
        imageWriteParam.compressionMode = ImageWriteParam.MODE_EXPLICIT
        imageWriteParam.compressionQuality = quality

        ImageIO.createImageOutputStream(outputFile).use { outputFileImageOutputStream ->
          imageWriter.output = outputFileImageOutputStream
          originalImage.inputStream().use { originalImageInputStream ->
            imageWriter.write(
              null, IIOImage(
                ImageIO.read(originalImageInputStream),
                null, null
              ), imageWriteParam
            )
            imageWriter.dispose()
          }
          log.info(
            "compressByQuality - Image file resized to <${originalImage.path}> " +
                " initialFileSize: <${originalImage.lengthInKb()} kb>, output <${outputFile.path}> " +
                " quality: <$quality>, finalSize: <${outputFile.lengthInKb()} kb>"
          )
        }

        //Handle on decreasing quality but size gets increase then throw Exception
        if (originalImage.lengthInKb() < outputFile.lengthInKb()) {
          log.info(
            "compressByQuality - Compressing given image <${originalImage.path}> by quality current quality <${quality}>" +
                " outfile size increases <${outputFile.lengthInKb()} kb> where as original size <${originalImage.lengthInKb()} kb>"
          )
          throw ImageProcessingException(
            Constant.ImageProcessingException.FILE_SIZE_INCREASES_BY_DECREASING_QUALITY.format(
              originalImage.lengthInKb(), outputFile.lengthInKb(), quality
            )
          )
        }

        currentFileSize = outputFile.lengthInKb()
      } catch (e: FileNotFoundException) {
        log.error(
          "compressByQuality  - Given invalid path for either original image <${originalImage.path}> or " +
              " output file <${outputFile.path}>."
        )
        throw ImageProcessingException(Constant.FileProcessingExceptions.FILE_NOT_FOUND)
      } catch (e: IOException) {
        log.error(
          "compressByQuality - Error occurs during writing or when not able to create required" +
              "  ImageOutputStream for input file <${originalImage.name}>, output file <${outputFile.name}>"
        )
        throw ImageProcessingException(Constant.ImageProcessingException.ERROR_OCCURS_ON_WRITING)
      }
    }
    return outputFile
  }
}

/**
 * Exception class for error message, which are related to Image Compression
 */
class ImageProcessingException(message: String) : RuntimeException(message)

/**
 * Class to maintain the metadata of image as per requirement
 */
class ImageMetaData {
  var height: Int? = null
  var width: Int? = null
}
