package com.equiruswealth.core.util

import com.equiruswealth.commons.utils.FileUtils
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.poi.sl.usermodel.PictureData.PictureType
import org.apache.poi.util.IOUtils
import org.apache.poi.xslf.usermodel.XMLSlideShow
import java.awt.Dimension
import java.awt.Rectangle
import java.io.File
import java.io.FileInputStream
import javax.imageio.ImageIO


object ConvertFromPDFtoPPTX {

  fun convertPDFtoPPTX(file: File): File? {
    val currentOutfile = "null.pptx"
    try {

        if (file.getName().endsWith(".pdf")) {
          try {
            val ppt = XMLSlideShow()
            val sourceDir = file.absolutePath // Pdf files are read from this folder
            val destinationDir = "temp/" + file.getName() + "/" // converted images from pdf document are saved here
            val sourceFile = File(sourceDir)
            val destinationFile = File(destinationDir)
            if (!destinationFile.exists()) {
              destinationFile.mkdir()
              println("Folder Created -> " + destinationFile.absolutePath)
            }
            if (sourceFile.exists()) {
              println("Images copied to Folder: " + destinationFile.getName())

              val document = Loader.loadPDF(file)
              val list = document.pages

              println("Total files to be converted -> " + list.count)

              val pdfRenderer = PDFRenderer(document)

              for (i in 0 until list.count) {

                val image = pdfRenderer.renderImageWithDPI(i, 300f, ImageType.RGB)

                val outputfile = File("test_${(i + 1)}.png")

                ImageIO.write(image, "png", outputfile)
                ppt.setPageSize(Dimension(1280, 720))
                val slide = ppt.createSlide()
                val pictureData = IOUtils.toByteArray(FileInputStream(outputfile.absolutePath))
                val pd = ppt.addPicture(pictureData, PictureType.PNG)
                val pic = slide.createPicture(pd)
                pic.setAnchor(Rectangle(0, 0, 1280, 720))
              }
              document.close()
              println("Converted Images are saved at -> " + destinationFile.absolutePath)
            } else {
              System.err.println(sourceFile.getName() + " File not exists")
            }

            val file = FileUtils.createTempFile("output.pptx")

            file.outputStream().use {
              ppt.write(it)
              ppt.close()
            }
            return file
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
    } catch (e: Exception) {
      println("--FAILURE: $currentOutfile")
      e.printStackTrace()
    }

    return null
  }

}