package com.equiruswealth.commons.utils

import com.equiruswealth.commons.constants.Constant
import com.equiruswealth.commons.enums.FileType
import com.equiruswealth.commons.models.RowData
import com.equiruswealth.commons.utils.JsonUtil.mapJsonToClass
import com.equiruswealth.commons.utils.excel.ExcelUtils.getStringValue
import com.google.gson.annotations.SerializedName
import com.linuxense.javadbf.DBFReader
import com.linuxense.javadbf.DBFRow
import com.univocity.parsers.annotations.Parsed
import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.time.format.DateTimeFormatter
import java.util.function.Predicate

/**
 * @author Shashwat Singh
 * Utils helps to process the files (csv, excel, dbf).
 */
object SheetUtils {

  private val log = LoggerFactory.getLogger(SheetUtils::class.java)
  private val settings = CsvParserSettings()

  /**
   * @method readSheet
   * Common method to process any given file type with required file.
   *
   * @param file - required file object
   * @param clazz - model class to be used for mapping file data (must have names similar to column names)
   * @param fileType - provide the type of file which
   * @param validateData - It's a method which is an optional, where it helps to validate data and also segregate invalid data.
   * @param headerRow Zero indexed row num to be provided in case of excel where headers can exist in row number other than 0
   * @param fetchMoreData - predicate to process file in batches of limit provided
   *
   * @return It returns the total rows processed.
   */
  fun <T> readSheet(
    file: File,
    clazz: Class<T>,
    fileType: FileType,
    delimiter: Char?= null,
    validateData: ((T) -> Boolean)?,
    headerRow: Int = 0,
    fetchMoreData: Predicate<ProcessedData<T>>
  ): Int {
    return try {
      val processedRows = when (fileType) {
        FileType.CSV -> {
          readCsv(file, clazz,  delimiter, validateData, fetchMoreData)
        }

        FileType.EXCEL -> {
          readExcel(file, clazz, validateData, headerRow, fetchMoreData)
        }

        FileType.DBF -> {
          readDbf(file, clazz, validateData, fetchMoreData)
        }

        else -> {
          log.info("readSheet - Provided file <${file.name}> of type <${fileType.name}> currently not supported.")
          throw FileProcessingException(Constant.FileProcessingExceptions.INVALID_FILE_RECEIVED)
        }
      }
      log.info("readSheet - Given file <${file.name}> of type <${fileType.name}> processed <$processedRows> rows.")
      processedRows
    } catch (e: FileNotFoundException) {
      log.info("readSheet - Given invalid path <${file.path}>, where file not found")
      throw FileProcessingException(Constant.FileProcessingExceptions.FILE_NOT_FOUND)
    } catch (e: IOException) {
      log.info("readSheet - Given invalid file <${file.name}>, expected file type <${fileType.name}>")
      throw FileProcessingException(Constant.FileProcessingExceptions.INVALID_FILE_RECEIVED)
    }
  }

  /**
   * @method readExcel
   * This method helps to parse, given Excel file object into list of objects of given class.
   *
   * @param file - File Object required
   * @param clazz - Pojo used for mapping the result with class members
   * @param validateData - It's a method which is an optional, where it helps to validate data and also segregate invalid data.
   * @param fetchMoreData - It helps to process the file in parts, and gives the List of validData and invalidData. [By Default 1000]
   *
   * @return It returns the total row processed.
   */
  fun <T> readExcel(
    file: File,
    clazz: Class<T>,
    validateData: ((T) -> Boolean)? = null,
    headerRow: Int = 0,
    fetchMoreData: Predicate<ProcessedData<T>>
  ): Int {
    var totalRows = 0
		// totalRow row denoted the stating point of header values
		if(headerRow != 0){
			totalRows = headerRow + Constant.SheetUtils.dataInitialRowNumber
		}
    FileInputStream(file).use { fileInputStream ->
      val workbook = WorkbookFactory.create(fileInputStream)
      val sheet = workbook.getSheetAt(0)
      val rows = sheet.iterator()
      var headerValues: Row? = null
      if (rows.hasNext()) {
        headerValues = rows.next().sheet.getRow(headerRow)
      }
      val fieldAnnotationValueMap = getFieldValueMap(clazz)
      val results = mutableListOf<T>()
      val errors = mutableListOf<SheetRowData>()
      while (rows.hasNext()) {
        val row = rows.next() as Row
        if (totalRows >= headerRow) {
          val objMap = mutableMapOf<String, String?>()
          mapHeaderValue(row, headerValues!!, objMap, fieldAnnotationValueMap)
          val parseData = SheetRowData(totalRows.plus(Constant.SheetUtils.dataInitialRowNumber), objMap)
          val data = mapJsonToClass(objMap.toJson(), clazz)
          validateData?.let {
            invokeValidationMethod(data, parseData, validateData, errors, results)
          } ?: run {
            (data as? RowData)?.let {
              it.rowNumber = row.rowNum
            }
            results.add(data)
          }
          if (breakFileProcessing(results, errors, fetchMoreData)) {
            break
          }
        }
        totalRows++
      }
      checkUnprocessedRows(results, errors, fetchMoreData)
    }
    log.info("readExcel - Total row processed <$totalRows> for file <${file.name ?: file.path}>")
    return totalRows - headerRow
  }

  /**
   * Method to get field annotation value map to map and populate data to each field of new model class
   */
  private fun <T> getFieldValueMap(clazz: Class<T>): MutableMap<String, String> {
    val fieldAnnotationValueMap = mutableMapOf<String, String>()
    clazz.declaredFields.forEach { field ->
      val annotationValue = field.getDeclaredAnnotation(SerializedName::class.java)
      if (annotationValue != null) {
        fieldAnnotationValueMap[annotationValue.value] = field.name
      }
    }
    return fieldAnnotationValueMap
  }

  private fun <T> checkUnprocessedRows(
    results: MutableList<T>,
    errors: MutableList<SheetRowData>,
    predicate: Predicate<ProcessedData<T>>
  ) {
    if (results.isNotEmpty() || errors.isNotEmpty()) {
      predicate.test(ProcessedData(results, errors))
      results.clear()
      errors.clear()
    }
  }

  private fun <T> invokeValidationMethod(
    data: T,
    sheetRowData: SheetRowData,
    validateData: (T) -> Boolean,
    errors: MutableList<SheetRowData>,
    results: MutableList<T>
  ) {
    if (validateData.invoke(data)) {
      results.add(data)
    } else {
      errors.add(sheetRowData)
    }
  }

  private fun <T> breakFileProcessing(
    results: MutableList<T>,
    errors: MutableList<SheetRowData>,
    predicate: Predicate<ProcessedData<T>>
  ): Boolean {
    if (results.size.plus(errors.size) == Constant.fileParsingLimit) {
      val fetchMore = predicate.test(ProcessedData(results, errors))
      results.clear()
      errors.clear()
      if (!fetchMore) {
        return true
      }
    }
    return false
  }

  /**
   * It check given cell [of Numeric Type] is formatted in percentage of not
   */
  private fun isCellPercentageFormatted(cell: Cell?): Boolean {
    val dataFormatter = DataFormatter()
    val formattedValue = dataFormatter.formatCellValue(cell)
    return formattedValue.endsWith("%")
  }

  private fun mapHeaderValue(
    row: Row,
    headerValues: Row,
    objMemberMap: MutableMap<String, String?>,
    fieldAnnotationValueMap: MutableMap<String, String>
  ) {
    val cells = row.iterator()
    while (cells.hasNext()) {
      val cell = cells.next() as Cell
      val columnIndex = cell.columnIndex
      val headerCellValue = headerValues.getStringValue(columnIndex)
      val value = getValueFromCell(cell)

      if (fieldAnnotationValueMap[headerCellValue] != null) {
        objMemberMap[fieldAnnotationValueMap[headerCellValue]!!] = value?.toString()
      }
    }
  }

  /**
   * Gets value of cell based upon cell type
   */
  private fun getValueFromCell(cell: Cell): Any? {

    val cellType = if (cell.cellType == CellType.FORMULA) {
      cell.cachedFormulaResultType
    } else {
      cell.cellType
    }

    return when (cellType) {
      CellType.NUMERIC -> if (DateUtil.isCellDateFormatted(cell)) {
        cell.localDateTimeCellValue.format(DateTimeFormatter.ISO_DATE_TIME)
      }  else if(isCellPercentageFormatted(cell)) {
        cell.numericCellValue * 100
      } else {
        String.format("%.0f", cell.numericCellValue)
      }
      CellType.STRING -> cell.stringCellValue
      CellType.BOOLEAN -> cell.booleanCellValue
      else -> null
    }
  }

  /**
   * @method readDbf
   * Method to process the dbf file and return the list of rows mapped to pojo class provided
   * in batches of limit provided.
   *
   * @param file - file object required
   * @param clazz - model class to be used for mapping csv (must have names similar to column names)
   * @param validateData - It's a method which is an optional, where it helps to validate data and also segregate invalid data.
   * @param fetchMoreData - predicate to process file in batches of limit provided
   *
   * @return It returns the total rows processed.
   */
  fun <T> readDbf(
    file: File,
    clazz: Class<T>,
    validateData: ((T) -> Boolean)? = null,
    fetchMoreData: Predicate<ProcessedData<T>>
  ): Int {
    var totalRows = 0

    FileInputStream(file).use { inputStream ->
      val reader = DBFReader(inputStream)
      val fieldAnnotationValueMap = getFieldValueMap(clazz)

      val results = mutableListOf<T>()
      val errors = mutableListOf<SheetRowData>()

      var row: DBFRow? = reader.nextRow()
      while (row != null) {
        val objMap = mutableMapOf<String, String?>()
        mapHeaderValue(clazz, row, reader, objMap, fieldAnnotationValueMap)
        val parseData = SheetRowData(totalRows.plus(Constant.SheetUtils.dataInitialRowNumber), objMap)
        val data = mapJsonToClass(objMap.toJson(), clazz)
        if (validateData != null) {
          invokeValidationMethod(data, parseData, validateData, errors, results)
        } else {
          results.add(data)
        }
        if (breakFileProcessing(results, errors, fetchMoreData)) {
          break
        }
        row = reader.nextRow()
        totalRows++
      }
      checkUnprocessedRows(results, errors, fetchMoreData)
    }
    log.info("readDbf - Total row processed <$totalRows> for file <${file.name ?: file.path}>")
    return totalRows
  }

  private fun <T> mapHeaderValue(
    clazz: Class<T>,
    row: DBFRow,
    reader: DBFReader,
    objMemberMap: MutableMap<String, String?>,
    fieldAnnotationValueMap: MutableMap<String, String>
  ) {
    for (i in 0 until reader.fieldCount) {
      val headerName = reader.getField(i).name
      val headerValue = row.getString(headerName)

      if (fieldAnnotationValueMap[headerName] == null) {
        log.debug(
          "mapHeaderValue - key <$headerName>, value <$headerValue>" +
              " not found in class <${clazz.name}>"
        )
      } else {
        objMemberMap[fieldAnnotationValueMap[headerName]!!] = headerValue
      }
    }
  }

  /**
   * @method readCsv
   * Method to process input stream of csv file and return list of rows mapped to pojo class provided
   * in batches of limit  provided
   *
   * @param file - file object required
   * @param clazz - model class to be used for mapping csv (must have names similar to column names)
   * @param validateData - It's a method which is an optional, where it helps to validate data and also segregate invalid data.
   * @param fetchMoreData - predicate to process file in batches of limit provided
   *
   * @return It returns the total rows processed.
   */
  private fun <T> readCsv(
    file: File,
    clazz: Class<T>,
    delimiter: Char?,
    validateData: ((T) -> Boolean)? = null,
    fetchMoreData: Predicate<ProcessedData<T>>
  ): Int {
    var totalRows = 0
    var currentRowNum = 1
    val results = mutableListOf<T>()
    val errors = mutableListOf<SheetRowData>()

    settings.isHeaderExtractionEnabled = true
    settings.isLineSeparatorDetectionEnabled = true
    if (delimiter != null) {
      settings.setDelimiterDetectionEnabled(true, delimiter)
    }
    val parser = CsvParser(settings)
    val fieldAnnotationValueMap = mutableMapOf<String, String>()
    clazz.declaredFields.forEach { field ->
      val annotationValue = field.getDeclaredAnnotation(Parsed::class.java)
      if (annotationValue != null) {
        fieldAnnotationValueMap[annotationValue.field.first().toString()] = field.name
      }
    }
    InputStreamReader(file.inputStream()).use { inputReader ->
      parser.beginParsing(inputReader)
      var record = parser.parseNextRecord()

      while (record != null) {
        currentRowNum++
        val recordHeaderMap = record.toFieldMap()
        val objMap = mutableMapOf<String, String?>()
        recordHeaderMap.forEach { recordEntry ->
          FileUtils.convertRecordToClazz(fieldAnnotationValueMap, recordEntry, objMap)
        }
        val parseData = SheetRowData(totalRows.plus(Constant.SheetUtils.dataInitialRowNumber), objMap)
        val data = mapJsonToClass(objMap.toJson(), clazz)
        (data as? RowData)?.let {
          it.rowNumber = currentRowNum
        }
        if (validateData != null) {
          invokeValidationMethod(data, parseData, validateData, errors, results)
        } else {
          results.add(data)
        }
        if (breakFileProcessing(results, errors, fetchMoreData)) {
          break
        }
        totalRows += 1
        record = parser.parseNextRecord()
      }
      checkUnprocessedRows(results, errors, fetchMoreData)
      parser.stopParsing()
    }
    log.info("readCsv - Total rows processed <$totalRows> for file - <${file.name ?: file.path}>")
    return totalRows
  }
}

/**
 * Class for returning list of valid data and invalid data
 */
data class ProcessedData<T>(val validData: List<T>, val invalidData: List<SheetRowData>)

/**
 * Class contains the data which are parsed from the file with row number
 * @param rowNumber - Data row number started from 2
 * @param data - It is of type Map<String, String?>, which contains header and its value (value can be null)
 */
data class SheetRowData(val rowNumber: Int, val data: Map<String, String?>)

/**
 * Exception class for error message, which are related to FileProcessing
 */
class FileProcessingException(message: String) : RuntimeException(message)
