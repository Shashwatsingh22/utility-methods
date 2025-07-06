package com.equiruswealth.commons.utils

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
 * Service to generate power point presentation
 */
@Service
class PowerPointGeneratorUtils {

  private val log = LoggerFactory.getLogger(this::class.java)

  /**
   * Method to generate ppt file
   */
  fun generatePPT(
    pptParameters: PPTParameters,
    templateFileInputStream: InputStream
  ): File {
    //Validate provided parameters first
    try {
      CommonValidation.validateSchema(VALIDATOR_SCHEMA, pptParameters.toJacksonJson()!!)
    } catch (e: Exception) {
      log.error("generatePPT - Error <$e> found in power point parameters.")
      throw ApplicationException(ApplicationExceptionTypes.INVALID_PPT_PARAMETERS)
    }

    val outputFile = FileUtils.createTempFile("${pptParameters.fileName!!}${Constant.FileExtensions.pptx}")

    //Mark slide or content as deleted
    markDeleteIndOnShapeAndSlide(pptParameters.slides!!)

    templateFileInputStream.use { templateInputStream ->
      XMLSlideShow(templateInputStream).use { show ->
        //Populate data
        populateData(pptParameters.slides!!, show)

        outputFile.outputStream().use {
          show.write(it)
        }
      }
    }
    return outputFile
  }

  /**
   * Method to populate data based on provided details
   */
  private fun populateData(
    slidesToPopulate: List<Slide>,
    slideShow: XMLSlideShow
  ) {
    slideShow.slides.forEach { slide ->
      val slideToPopulate = fetchSlideToPopulate(slide, slidesToPopulate)

      //Check slide details found
      if (slideToPopulate == null) {
        log.error("populateData - No details found for the slide <${slide.slideNumber}>.")
        return@forEach
      }
      slideToPopulate.slideNumber = slide.slideNumber

      //Check if slide required to delete
      if (slideToPopulate.delete == true) {
        return@forEach
      }

      //Check slide details have contents to populate
      if (slideToPopulate.contents.isNullOrEmpty()) {
        log.info("populateData - No content found for slide <${slideToPopulate.identifier!!.id}>.")
        return@forEach
      }

      //First reposition the available shapes at correct place
      repositionShapes(slide, slideToPopulate)

      //Populate content
      slideToPopulate.contents!!.forEach {
        populateContent(content = it, slide)
      }
    }
    //Delete shapes and slides
    deleteShapesAndSlides(slidesToPopulate, slideShow)

    //Populate Headings of slides
    populateSlideHeadings(slidesToPopulate, slideShow)
  }

  /**
   * Method to populate slides headings
   * As this stage slide and shapes all are deleted now for populating heading we need
   * to traverse complete slides, find the text shape then populate data, we can't rely on existing
   * slideNumber or shapeId.
   */
  private fun populateSlideHeadings(
    slidesToPopulate: List<Slide>,
    slideShow: XMLSlideShow
  ) {
    slidesToPopulate.forEach { slide ->
      if (slide.identifier!!.data.isNullOrBlank()) return@forEach

      val identifierId = slide.identifier!!.id!!

      slideShow.slides.forEach { xslfSlide ->
        val textShape = getTextShape(identifierId, xslfSlide) ?: return@forEach
        populateTextDetails(slide.identifier!!, textShape)
      }
    }
  }

  /**
   * Method to delete shapes or slides based on indicators
   */
  private fun deleteShapesAndSlides(
    slidesToPopulate: List<Slide>,
    slideShow: XMLSlideShow
  ) {
    //Create pair of deleted slide number, shapeId
    val deletedShapeIdWithSlideNumber = mutableListOf<Triple<Int, Shape, ShapeType>>()

    //List of deleted slideIds
    val deletedSlideIds = mutableListOf<Int>()

    slidesToPopulate.forEach {
      val slideNumber = it.slideNumber!!

      if (it.delete == true) {
        deletedSlideIds.add(slideNumber)
        return@forEach
      }

      if (it.identifier!!.data.isNullOrBlank()) {
        deletedShapeIdWithSlideNumber.add(Triple(slideNumber, it.identifier!!, ShapeType.TEXT))
      }

      val deletedContents =
        it.contents?.filter { it.chart?.delete == true || it.text?.delete == true || it.table?.delete == true }
      deletedContents?.forEach { content ->
        when (content.type!!) {
          ShapeType.TEXT -> {
            deletedShapeIdWithSlideNumber.add(Triple(slideNumber, content.text!!, ShapeType.TEXT))
          }

          ShapeType.CHART -> {
            deletedShapeIdWithSlideNumber.add(Triple(slideNumber, content.chart!!, ShapeType.CHART))
            content.chart!!.label?.let {
              deletedShapeIdWithSlideNumber.add(Triple(slideNumber, it.table!!, ShapeType.TABLE))
            }
          }

          ShapeType.TABLE -> {
            deletedShapeIdWithSlideNumber.add(Triple(slideNumber, content.table!!, ShapeType.TABLE))
            content.table!!.label?.let {
              deletedShapeIdWithSlideNumber.add(Triple(slideNumber, it.table!!, ShapeType.TABLE))
            }
          }
        }
      }
    }

    if (deletedShapeIdWithSlideNumber.isNotEmpty()) {
      //Delete shapes first
      //Shapes to removed from last due to ConcurrentModificationException
      deletedShapeIdWithSlideNumber.sortByDescending {
        it.second.shapeId ?: it.second.shapeId ?: it.second.shapeId
      }
      deletedShapeIdWithSlideNumber.forEach { (slideNumber, shape, shapeType) ->
        //As slide index starts from 1 according to our implementation
        //but actually it will starting from 0
        val validSlide = slideShow.slides[slideNumber - 1] ?: return@forEach

        //Get shape based on given content type
        val validShape = when (shapeType) {
          ShapeType.TEXT -> getTextShape(shape.id!!, validSlide)
          ShapeType.TABLE -> getTableShape(shape.id!!, validSlide)
          ShapeType.CHART -> getChartShape(shape.id!!, validSlide)
        } ?: return@forEach

        //Finally remove shape
        validSlide.removeShape(validShape)
      }
    }

    //Finally delete slides
    deletedSlideIds.reversed().forEach {
      //As index starts from 1 according to our implementation but actually
      //it will starting from 0
      val validSlide = slideShow.slides[it - 1] ?: return@forEach
      slideShow.removeSlide(validSlide.slideNumber - 1)
    }
  }


  /**
   * Method which reposition non-deleted shape at correct places
   */
  private fun repositionShapes(
    slide: XSLFSlide,
    slideToPopulate: Slide
  ) {
    val deletedShapes = slideToPopulate.contents?.count {
      it.chart?.delete == true ||
          it.table?.delete == true
    } ?: 0

    if (deletedShapes == 0) {
      log.info(
        "repositionShapes - As no content found as deleted for slide <${slideToPopulate.identifier?.id}>" +
            " skipping repositioning process."
      )
      return
    }

    val nonDeletedShapes = slideToPopulate.contents!!.size - deletedShapes
    var shapeIndex = 0
    slideToPopulate.contents!!.forEach { content ->

      //Reposition non deleted shape
      when (content.type!!) {
        ShapeType.CHART -> {
          if (content.chart!!.delete == true) return@forEach
          shapeIndex++

          val chartShape = getChartShape(content.chart!!.id!!, slide) ?: return@forEach
          val shapeLabel = content.chart?.label?.table?.let {
            getTableShape(it.id!!, slide)
          }

          content.chart!!.shapeId = chartShape.shapeId

          setShatAtPosition(
            chartShape,
            nonDeletedShapes,
            slide,
            shapeIndex
          )

          shapeLabel?.let {
            setShatAtPosition(it, nonDeletedShapes, slide, shapeIndex)
          }
        }

        ShapeType.TABLE -> {
          if (content.table!!.delete == true) return@forEach
          shapeIndex++

          val tableShape = getTableShape(content.table!!.id!!, slide) ?: return@forEach
          val shapeLabel = content.table?.label?.table?.let {
            getTableShape(it.id!!, slide)
          }

          setShatAtPosition(tableShape, nonDeletedShapes, slide, shapeIndex)
          shapeLabel?.let {
            setShatAtPosition(it, nonDeletedShapes, slide, shapeIndex)
          }
        }

        else -> {
          log.info("repositionShapes1 - No repositioning required for contentId <${content.text?.id}>.")
          return@forEach
        }
      }
    }
  }

  /**
   * Method to reposition an given shape
   */
  @Suppress("kotlin:S1871")
  private fun setShatAtPosition(
    shape: XSLFShape,
    nonDeletedShapes: Int,
    slide: XSLFSlide,
    shapeIndex: Int
  ): Double {
    return when {
      shape is XSLFGraphicFrame && shape.chart != null -> {
        shape.anchor = getNewPosition(shape, slide, nonDeletedShapes, shapeIndex)
        shape.anchor.x
      }

      shape is XSLFTable && shape.rows != null -> {
        shape.anchor = getNewPosition(shape, slide, nonDeletedShapes, shapeIndex)
        shape.anchor.x
      }

      else -> 0.0
    }
  }

  /**
   * Method to get new position for shape
   */
  private fun getNewPosition(
    shape: XSLFShape,
    slide: XSLFSlide,
    nonDeletedShapes: Int,
    shapeIndex: Int
  ): Rectangle2D {
    val slideWidth = slide.slideShow.pageSize.width
    //As change only occur in x-axis
    val xAxis = if (nonDeletedShapes == 1) (slideWidth - shape.anchor.width) / 2 else {
      val gap = (slideWidth - nonDeletedShapes * shape.anchor.width) / (nonDeletedShapes + 1);
      gap * shapeIndex + shape.anchor.width * (shapeIndex - 1)
    }
    return Rectangle2D.Double(
      xAxis,
      shape.anchor.y,
      shape.anchor.width,
      shape.anchor.height
    )
  }

  /**
   * Method to populate content
   */
  private fun populateContent(
    content: Content,
    slide: XSLFSlide
  ) {
    when (content.type!!) {
      ShapeType.TABLE -> {
        val tableDetails = content.table!!

        //Populate label [it also an type of shape which is not fixed]
        tableDetails.label?.let { label ->
          populateContent(label, slide)
        }

        //Populate Table
        val tableShape = getTableShape(tableDetails.id!!, slide) ?: return
        tableDetails.shapeId = tableShape.shapeId
        if (tableDetails.delete == true) {
          return
        }
        populateTableDetails(tableDetails, tableShape)
      }

      ShapeType.CHART -> {
        val chartDetails = content.chart!!

        //Populate label [it also an type of shape which is not fixed]
        chartDetails.label?.let {
          populateContent(it, slide)
        }

        //Populate chart
        populateChart(chartDetails, slide)
      }

      ShapeType.TEXT -> {
        populateText(content.text!!, slide)
      }
    }
  }

  /**
   * Method to populate chart details
   */
  private fun populateChart(
    chartDetails: ChartShape,
    slide: XSLFSlide
  ) {
    val chartShape = getChartShape(chartDetails.id!!, slide) ?: return
    chartDetails.shapeId = chartShape.shapeId

    if (chartDetails.delete == true) {
      return
    }

    //Check provided details to populate
    try {
      validateInputChartData(chartDetails.id!!, chartDetails.categorizedData!!)
    } catch (_: PPTGeneratorException) {
      chartDetails.delete = true
      return
    }

    val chart = chartShape.chart
    //As title we keep for uniqueId purpose only
    chart.removeTitle()

    //Populate it
    populateChartDetails(
      chartShape.chart!!,
      chartDetails.categorizedData!!
    )
  }

  /**
   * Method to populate text details
   */
  private fun populateText(
    textDetails: TextShape,
    slide: XSLFSlide
  ) {
    val textShape = getTextShape(textDetails.id!!, slide) ?: return
    textDetails.shapeId = textShape.shapeId
    if (textDetails.delete == false) {
      populateTextDetails(textDetails, textShape)
    }
  }

  /**
   * Method to get text shape for given id
   */
  private fun getTextShape(
    textShapeId: String,
    slide: XSLFSlide
  ): XSLFTextShape? {
    return slide.shapes.filterIsInstance<XSLFTextShape>().find {
      !it.text.isNullOrBlank()
          && it.text.sanitizeAndLowerCase() == textShapeId.sanitizeAndLowerCase()
    }
  }

  /**
   * Method to get table shape for given id
   */
  private fun getTableShape(
    tableShapeId: String,
    slide: XSLFSlide
  ): XSLFTable? {
    return slide.shapes.filterIsInstance<XSLFTable>().find {
      it.rows.any {
        it.cells.any {
          !it.text.isNullOrBlank()
              && it.text.sanitizeAndLowerCase() == tableShapeId.sanitizeAndLowerCase()
        }
      }
    }
  }

  /**
   * Method to get chart shape for given id
   */
  private fun getChartShape(
    chartShapeId: String,
    slide: XSLFSlide
  ): XSLFGraphicFrame? {
    var chartShape: XSLFGraphicFrame? = null
    slide.shapes.forEach { shape ->
      if (shape !is XSLFGraphicFrame || shape.chart == null) return@forEach
      val chart = shape.chart
      if (chart.title?.body?.paragraphs?.mapNotNull { it.text.sanitizeAndLowerCase() }
          ?.contains(chartShapeId.sanitizeAndLowerCase()) == true) {
        chartShape = shape
        return@forEach
      }
    }
    return chartShape
  }


  /**
   * Method to populate chart content
   */
  private fun populateChartDetails(
    chart: XSLFChart,
    chartDataList: List<GenericChart<Double>>,
    seriesInPercentage: Boolean = true
  ) {
    val chartWorkbook = chart.workbook
    //Delete existing sheet if exists
    chartWorkbook.getSheetName(0)?.let { chartWorkbook.removeSheetAt(0) }
    chartWorkbook.createSheet("ChartData").also { chartSheet ->
      populateChartData(
        chartSheet,
        chart,
        chartDataList,
        seriesInPercentage
      )
    }
    chartWorkbook.write(chart.packagePart.outputStream)
  }

  /**
   * Method to populate chart details
   */
  private fun populateChartData(
    chartSheet: XSSFSheet,
    chart: XSLFChart,
    chartDataList: List<GenericChart<Double>>,
    seriesInPercentage: Boolean
  ) {
    var columnIndex = 0 //Initial Category Column

    val chartData = chart.chartSeries[0]

    //Set Category
    val categoryTypes = chartDataList.map { it.name }
    categoryTypes.forEachIndexed { index, category ->
      val sheetNewRow = chartSheet.getRow(index) ?: chartSheet.createRow(index)
      (sheetNewRow.getCell(columnIndex) ?: sheetNewRow.createCell(columnIndex)).apply {
        setCellValue(category)
        cellType = CellType.STRING
      }
    }

    val category = XDDFDataSourcesFactory.fromStringCellRange(
      chartSheet, CellRangeAddress(0, categoryTypes.size - 1, columnIndex, columnIndex)
    )

    //Set Series data [of Type Numeric]
    //Adding Data column wise, where we get series or create new series then plot it.
    var cellStyle: XSSFCellStyle? = null
    val seriesDataList = chartDataList.mapNotNull { it.values }

    var seriesIndex = 0 //For plotting series [Starting from 0]
    for (col in 0 until seriesDataList[0].size) {
      columnIndex++ //Increment for Column
      for (row in seriesDataList.indices) {
        val sheetRow = chartSheet.getRow(row) ?: chartSheet.createRow(row)
        (sheetRow.getCell(columnIndex) ?: sheetRow.createCell(columnIndex)).apply {
          val seriesValue = if (seriesInPercentage) {
            NumberUtils.limitDecimalPlaces(seriesDataList[row][col].div(100), decimalPlaces = 4)!!
          } else {
            seriesDataList[row][col]
          }
          setCellValue(seriesValue)
          cellType = CellType.NUMERIC
          if (cellStyle == null) {
            cellStyle = this.cellStyle
          }
          setCellStyle(cellStyle)
        }
      }
      val values = XDDFDataSourcesFactory.fromNumericCellRange(
        chartSheet,
        CellRangeAddress(0, seriesDataList.size - 1, columnIndex, columnIndex)
      )

      var series: XDDFChartData.Series?

      //Series 1 already present in chart, first plot them then add new series
      // [May an exception occur due to java.lang.IndexOutOfBoundsException for getting series]
      try {
        //May an exception occur due to java.lang.IndexOutOfBoundsException
        // chartData.getSeries -> It not return nullable value
        series = chartData.getSeries(seriesIndex++)
        series.replaceData(category, values)
      } catch (_: Exception) {
        series = chartData.addSeries(category, values)
      }
      series!!.plot()
    }
  }

  /**
   * Method to validate the category series list there series list should be of same length.
   *
   * Example -
   *    [Invalid as an category name found null]
   *    MF   ->  [49.0, 50.0]
   *    null -> [20.5, 34.0]
   *
   *    [Invalid series data length not equal to other categories series data]
   *    MF -> [49.0, 50.0]
   *    AIF -> [20.0, 34.7, 29.0]
   *    PMS -> [23.9, 34.0]
   */
  private fun validateInputChartData(
    chartId: String,
    chartDataList: List<GenericChart<Double>>
  ) {
    val chartDataWithNullValues = chartDataList.filter { it.values == null }
    if (chartDataWithNullValues.isNotEmpty()) {
      log.error(
        "validateInputChartData - Found series data null for categories <${chartDataWithNullValues.map { it.name }}>" +
            " chartIndex <${chartId}>."
      )
      throw PPTGeneratorException(FOUND_CATEGORY_SERIES_DATA_NULL.format(chartId))
    }

    val lengthOfEachSeries = chartDataList.first().values!!.size
    if (chartDataList.any { it.values == null || it.values!!.size != lengthOfEachSeries }) {
      log.error(
        "validateInputChartData - Inconsistent series data lengths detected." +
            " chartIndex <${chartId}>."
      )
      throw PPTGeneratorException(INCONSISTENT_SERIES_DATA_LENGTH.format(chartId))
    }
  }

  /**
   * Method to populate text details
   */
  private fun populateTextDetails(
    textDetails: TextShape,
    text: XSLFTextShape
  ) {
    text.text = textDetails.data
    val textMetaData = textDetails.metaData

    text.textParagraphs.forEach { paragraph ->
      paragraph.textRuns.forEach { textRun ->
        textRun.fontFamily = textMetaData?.fontFamily ?: textRun.fontFamily
        textMetaData?.textHexColor?.let {
          textRun.setFontColor(Color.decode(it))
        } ?: textRun.setFontColor(textRun.fontColor)
        textRun.fontSize = textMetaData?.fontSize ?: textRun.fontSize
        textRun.isBold = textMetaData?.isBold ?: textRun.isBold
      }
    }
  }


  /**
   * Method to populate table content
   */
  private fun populateTableDetails(
    tableDetails: TableShape,
    table: XSLFTable
  ) {
    val dataList = tableDetails.dataList!!

    //First row always be an header
    (table.rows.firstOrNull() ?: table.insertRow(0)).let { headerRow ->
      headerRow.cells.forEachIndexed { cellIndex, cell ->
        setCellData(
          data = dataList[0][cellIndex].toString(),
          backgroundColorHex = tableDetails.headerMetaData?.backGroundColorHex,
          borderColorHex = tableDetails.borderColorHex,
          textMetaData = tableDetails.headerMetaData?.textMetaData,
          cell
        )
      }
    }

    //Populate further details
    dataList.subList(1, dataList.size).forEachIndexed { dataRowIndex, dataRow ->
      val row = table.rows.getOrNull(dataRowIndex + 1) ?: table.insertRow(dataRowIndex + 1)
      val highlightLastRow = dataRowIndex.plus(1) == dataList.size.minus(1) && tableDetails.footerMetaData != null
      row.cells.forEachIndexed { cellIndex, cell ->
        setCellData(
          data = dataRow[cellIndex],
          backgroundColorHex = if (highlightLastRow) {
            tableDetails.footerMetaData?.backGroundColorHex
          } else {
            tableDetails.rowWiseMetadataList?.find {
              it.rowIndex == dataRowIndex.plus(1)
            }?.backGroundColorHex ?: tableDetails.normalRowsMetaData?.backGroundColorHex
          },
          borderColorHex = tableDetails.borderColorHex,
          textMetaData = if (highlightLastRow) {
            tableDetails.footerMetaData?.textMetaData
          } else {
            tableDetails.rowWiseMetadataList?.find {
              it.rowIndex == dataRowIndex.plus(1)
            }?.textMetaData ?: tableDetails.normalRowsMetaData?.textMetaData
          },
          cell
        )
      }
    }
  }

  /**
   * Method to set the cell with given details
   */
  private fun setCellData(
    data: String,
    backgroundColorHex: String?,
    borderColorHex: String,
    textMetaData: TextMetaData?,
    cell: XSLFTableCell
  ) {
    //Set Data
    cell.text = data

    //Set Row Styling
    val validBorderColor = try {
      Color.decode(borderColorHex)
    } catch (_: NumberFormatException) {
      null
    }

    //Set border
    validBorderColor?.let {
      cell.setBorderColor(TableCell.BorderEdge.bottom, it)
      cell.setBorderColor(TableCell.BorderEdge.top, it)
      cell.setBorderColor(TableCell.BorderEdge.left, it)
      cell.setBorderColor(TableCell.BorderEdge.right, it)
    }

    // Set cell background color
    backgroundColorHex?.let {
      try {
        cell.setFillColor(Color.decode(it))
      } catch (_: NumberFormatException) {
        null
      }
    }

    //set font style and font size
    textMetaData?.let {
      applyTextFormatsOnCell(cell, it)
    }
    cell
  }

  /**
   * Method to apply text formats
   */
  private fun applyTextFormatsOnCell(
    cell: XSLFTableCell,
    textMetaData: TextMetaData?
  ) {
    cell.textParagraphs.forEach { paragraph ->
      paragraph.textRuns.forEach { textRun ->
        textRun.fontFamily = textMetaData?.fontFamily ?: textRun.fontFamily
        textMetaData?.textHexColor?.let {
          textRun.setFontColor(Color.decode(it))
        } ?: textRun.setFontColor(textRun.fontColor)
        textRun.fontSize = textMetaData?.fontSize ?: textRun.fontSize
        textRun.isBold = textMetaData?.isBold ?: false
      }
    }
  }

  /**
   * Method to get slide based on given id
   */
  private fun fetchSlideToPopulate(
    slide: XSLFSlide,
    slidesToPopulate: List<Slide>
  ): Slide? {
    slide.shapes.filterIsInstance<XSLFTextShape>().forEach { shape ->
      if (shape.text.isNullOrBlank()) return@forEach

      val data = slidesToPopulate.find {
        it.identifier?.id?.sanitizeAndLowerCase() == shape.text.sanitizeAndLowerCase()
      } ?: return@forEach

      return data
    }
    return null
  }

  /**
   * Method to mark for deletion on shape or slide
   * - Shape marked as deleted if no data found related to shape type
   * - Slide marked as deleted if all shapes are marked as deleted of it.
   */
  private fun markDeleteIndOnShapeAndSlide(slides: List<Slide>) {
    slides.forEach { slide ->
      //Mark delete Indicator on label/heading of slide no content required to populate for it
      val identifier = slide.identifier!!
      if (identifier.data.isNullOrBlank()) {
        identifier.delete = true
      }

      var deleteSlide = true

      //Mark delete indicator on shapes
      slide.contents?.forEach { content ->
        val isContentDeleted = checkContent(content)
        if (isContentDeleted == false) {
          deleteSlide = false
        }
      }
      slide.delete = deleteSlide && (!slide.contents.isNullOrEmpty() || identifier.delete == true)
    }
  }

  /**
   * Method to check content need to delete or not
   */
  private fun checkContent(content: Content): Boolean {
    when (content.type!!) {
      ShapeType.TEXT -> {
        val textDetails = content.text!!
        textDetails.delete = textDetails.data.isNullOrBlank()
      }

      ShapeType.CHART -> {
        val chartDetails = content.chart!!
        chartDetails.delete = chartDetails.categorizedData.isNullOrEmpty()
        chartDetails.label?.table?.let {
          it.delete = chartDetails.delete
        }
      }

      ShapeType.TABLE -> {
        val tableDetails = content.table!!
        tableDetails.delete = tableDetails.dataList.isNullOrEmpty()
        tableDetails.label?.table?.let {
          it.delete = tableDetails.delete
        }
      }
    }

    return content.chart?.delete == true || content.text?.delete == true || content.table?.delete == true
  }

  /**
   * Model for throwing exceptions
   */
  class PPTGeneratorException(message: String): RuntimeException(message)
}