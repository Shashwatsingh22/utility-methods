package com.equiruswealth.commons.utils

import com.equiruswealth.commons.constants.Constant
import com.equiruswealth.commons.constants.Constant.PPTGenerator.ErrorMessages.FOUND_CATEGORY_SERIES_DATA_NULL
import com.equiruswealth.commons.constants.Constant.PPTGenerator.ErrorMessages.INCONSISTENT_SERIES_DATA_LENGTH
import com.equiruswealth.commons.models.chart.GenericChart
import com.equiruswealth.commons.models.ppt_generator.CategorizedChartModel
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xddf.usermodel.chart.XDDFChartData
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFChart
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.slf4j.LoggerFactory
import java.io.File

/**
 * @author Shashwat Singh
 * Utility method for generating PPT
 */
object PPTGeneratorUtils {

  private val log = LoggerFactory.getLogger(this::class.java)

  /**
   * Method to populate chart data in PPT
   */
  fun populateChartData(
    existingPPT: File,
    chartData: List<CategorizedChartModel>,
    outputFileName: String
    ): File {
    val output = FileUtils.createTempFile("${outputFileName}${Constant.FileExtensions.pptx}")

    existingPPT.inputStream().use { inputStream ->
      val slideShow = XMLSlideShow(inputStream)

      //Iterate through each chart, to populate data
      slideShow.charts.forEachIndexed { index, data ->

        //Get the chart data based on id [Index according to PPT]
        val chart = chartData.find { it.id == index }

        //Validate category wise series data list present or not, then remove the chart from PPT slide
        if(chart == null || chart.categorizedData.isNullOrEmpty()) {
          log.info("populateChartData - No categorizedData found for chart index <${index}>, name <${chart?.name}>")
          return@forEachIndexed
        }

        //Validate category wise series data is valid to process chart
        //If not valid then throw exception either it may corrupted generated PPT file
        validateInputChartData(chart.categorizedData!!)

        log.debug("populateChartData - Starting the process for populating data for chart <${chart.name}>")
        addChartData(data, chart.categorizedData!!)
      }
      //Delete charts whose category wise series data not found
      removeChartFromSlide(slideShow, chartData)

      output.outputStream().use {
        slideShow.write(it)
      }
    }
    return output
  }

  /**
   * Method to validate the category series list there series list should be of same length.
   */
  private fun validateInputChartData(chartDataList: List<GenericChart<Double>>) {
    val chartDataWithNullValues = chartDataList.filter { it.values == null }
    if(chartDataWithNullValues.isNotEmpty()) {
      log.error("validateInputChartData - Found series data null for categories <${chartDataWithNullValues.map { it.name }}>")
      throw PPTGeneratorException(message = FOUND_CATEGORY_SERIES_DATA_NULL)
    }

    val lengthOfEachSeries = chartDataList.first().values!!.size
    if(chartDataList.any { it.values == null || it.values!!.size != lengthOfEachSeries }) {
      log.error("validateInputChartData - Inconsistent series data lengths detected.")
      throw PPTGeneratorException(message = INCONSISTENT_SERIES_DATA_LENGTH)
    }
  }

  /**
   * Method to remove chart from provided slide for given index
   */
  private fun removeChartFromSlide(slideShow: XMLSlideShow, chartDataList: List<CategorizedChartModel>) {
    val shapesToDelete = chartDataList.filter { it.categorizedData.isNullOrEmpty() }

    if(shapesToDelete.isEmpty()) {
      log.info("removeChartFromSlide - No charts to remove as all data are already present.")
      return
    }

    shapesToDelete.forEach { chart ->
      log.debug("removeChartFromSlide - Removing chart from slide <${chart.slideNumber}>, shapeId <${chart.shapeId}> " +
          " whose index <${chart.id}> and name <${chart.name}>")
      val affectedSlide = slideShow.slides[chart.slideNumber!!]
      affectedSlide.removeShape(affectedSlide.shapes[chart.shapeId!!])
    }
  }

  private fun addChartData(chart: XSLFChart, chartDataList: List<GenericChart<Double>>) {
    val chartWorkbook = chart.workbook
    val chartSheetName = chartWorkbook.getSheetName(0)
    val chartSheet = chartWorkbook.getSheet(chartSheetName)

    var columnIndex = 0 //Initial Category Column

    val chartData = chart.chartSeries[0]

    //Set Category
    val categoryTypes = chartDataList.map { it.name }
    categoryTypes.forEachIndexed { index, category ->
      val sheetNewRow = chartSheet.createRow(index)
      sheetNewRow.getCell(columnIndex) ?: sheetNewRow.createCell(columnIndex).apply {
        setCellValue(category)
        cellType = CellType.STRING
      }
    }

    val category = XDDFDataSourcesFactory.fromStringCellRange(chartSheet, CellRangeAddress(0, categoryTypes.size - 1, columnIndex, columnIndex))

    //Set Series data [of Type Numeric]
    //Adding Data column wise, where we get series or create new series then plot it.
    var cellStyle: XSSFCellStyle? = null
    val seriesDataList = chartDataList.mapNotNull { it.values }

    var seriesIndex = 0 //For plotting series [Starting from 0]
    for(col in 0 until seriesDataList[0].size) {
      columnIndex++ //Increment for Column
      for(row in seriesDataList.indices) {
        val sheetRow = chartSheet.getRow(row) ?: chartSheet.createRow(row)
        sheetRow.getCell(columnIndex) ?: sheetRow.createCell(columnIndex).apply {
          setCellValue(seriesDataList[row][col])
          cellType = CellType.NUMERIC
          if(cellStyle == null) {
            cellStyle = this.cellStyle
          }
          setCellStyle(cellStyle)
        }
      }
      val values = XDDFDataSourcesFactory.fromNumericCellRange(chartSheet, CellRangeAddress(0, seriesDataList.size - 1, columnIndex, columnIndex))

      var series : XDDFChartData.Series?

      //Series 1 already present in chart, first plot them then add new series
      // [May an exception occur due to java.lang.IndexOutOfBoundsException for getting series]
      try {
        //May an exception occur due to java.lang.IndexOutOfBoundsException
        // chartData.getSeries -> It not return nullable value
        series = chartData.getSeries(seriesIndex++)
        series.replaceData(category, values)
      } catch (e: Exception) {
        series = chartData.addSeries(category, values)
      }
      series!!.plot()
    }
  }

  /**
   * Model for throwing exceptions
   */
  class PPTGeneratorException(message: String): RuntimeException()
}