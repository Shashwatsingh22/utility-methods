package com.equiruswealth.fundboss.utils

import com.equiruswealth.commons.utils.isLetterOrDigitOrUnderScore
import com.equiruswealth.commons.utils.lengthInMb
import com.equiruswealth.fundboss.mapper.FundBossModelMapper
import org.slf4j.LoggerFactory
import org.springframework.util.StopWatch
import java.io.BufferedReader
import java.io.File
import java.util.function.Consumer

/**
 * @author Shashwat Singh
 * This utility object helps to parse the given json file,
 * in producer and consumer concept.
 */
object JsonParser {

  private val log = LoggerFactory.getLogger(this::class.java)

  /**
   * Method will help to process the given large json file in chunks, with concept of producer and consumer.
   */
  fun <T> parseReportTable(
    jsonFile: File,
    clazzToMapValue: Class<T>,
    keyName: String,
    limitParsingTill: Int,
    fetchMoreData: Consumer<ProcessedData<T>>
  ): Int {
    var totalClosingBracketReads = 0
    val jsonValue = StringBuilder()
    var totalDataParsed = 0

    val timer = StopWatch()
    timer.start()

    log.debug("parseReportTable - File <{}>, provided to parse data for key <{}>.", jsonFile, "ReportTable")
    jsonFile.bufferedReader().use { reader ->

      //First fetch the index of bytes array at where we found the key
      val keyIndexPair = fetchKeyIndex(reader, keyName)
      if(keyIndexPair == null) {
        log.error("parseReportTable - Not found given key <$keyName>, from file <$jsonFile> for current stream lets call next stream.")
        return@use
      }
      log.info("parseReportTable - Successfully found the key <${keyIndexPair.first}>, at index <${keyIndexPair.second}>")
      //Process the values of array or single element
      skipReaderTillStartingOfValue(reader)
      var currentChar: Char
      jsonValue.append("\"[")
      while (reader.read().also { currentChar = it.toChar() } != -1) {
        jsonValue.append(currentChar)

        val (nextChar, isLastElement) = validateElementForEnd(reader, currentChar, jsonValue)
        if(isLastElement) {
          totalClosingBracketReads++
          val totalDataSent =
            sendDataToConsumer(jsonValue, currentChar, totalClosingBracketReads, clazzToMapValue, limitParsingTill, nextChar!!, fetchMoreData)
              ?: continue
          totalDataParsed += totalDataSent

          jsonValue.clear()
          jsonValue.append("\"[")
          totalClosingBracketReads = 0

          if(currentChar == ']' || nextChar == ']') {
            timer.stop()
            log.info("parseReportTable - Consumer requested to stop data processing, or " +
                " Reached to last char of key <${keyIndexPair.first}>, total data parsed <$totalDataParsed>," +
                " where, total time <${timer.totalTimeSeconds}> sec taken to parse it, data processed in <$limitParsingTill> chunks at a time" +
                " and file size <${jsonFile.lengthInMb()}> MB.")
            return totalDataParsed
          }
        }
      }
    }
    timer.stop()
    log.info("parseReportTable - <$totalDataParsed>, total data parsed, " +
        "where, total time <${timer.totalTimeSeconds}> sec taken to parse it, data processed in <$limitParsingTill> chunks at a time, " +
        " and file size <${jsonFile.lengthInMb()}> MB.")
    return totalDataParsed
  }

  /**
   * Method to check condition for end of element or end of value
   * It return the nextChar and validation of end of element
   */
  private fun validateElementForEnd(reader: BufferedReader, currentChar: Char, jsonValue: StringBuilder): Pair<Char?,Boolean> {
    if(currentChar in setOf('}',']')) {
      val nextChar: Char
      reader.read().also { nextChar = it.toChar() }
      jsonValue.append(nextChar)
      val result = (currentChar == '}' &&  nextChar in setOf(',',']')) || (currentChar == ']' && nextChar == '"')
      return Pair(nextChar, result)
    }
    return Pair(null,false)
  }

  /**
   * Method fetch key from given bytes array,
   * if found return index else null
   */
  private fun fetchKeyIndex(reader: BufferedReader, keyName: String): Pair<String,Int>? {
    val currentKey = StringBuilder()
    var foundKey = ""
    var currentChar: Char
    var index = -1
    while (reader.read().also { currentChar = it.toChar() } != -1) {
      if(currentChar.isLetterOrDigitOrUnderScore()){
        currentKey.append(currentChar)
      }

      // if current char is quotes and last key is not blank, last key found as Letter, Digit or have underscored
      if (currentChar == '"' && currentKey.isNotBlank() && currentKey.last().isLetterOrDigitOrUnderScore()) {
        foundKey = currentKey.toString()
        currentKey.clear()
      }

      if(currentKey.isBlank() && foundKey == keyName) {
        log.info("fetchKeyIndex - Key <${foundKey}> found at index <$index>.")
        return Pair(foundKey, index)
      }
      index++
    }
    return null
  }

  /**
   * Method to skip the reader pointer till starting of key value
   */
  private fun skipReaderTillStartingOfValue(reader: BufferedReader) {
    var currentChar: Char
    //Skip reader pointer till ":
    while (reader.read().also { currentChar = it.toChar() } != -1) {
      if(currentChar == '[') {
        break
      }
    }
  }

  /**
   * Method to validate the limit of parsing at time, or if we are at the end of json element
   * then return the parsed data to consumer, wait for response from consumer method.
   *
   * if data sent consumer and consumer send response to process more
   *  then return size of parsed data
   */
  private fun <T> sendDataToConsumer(
    jsonValue: java.lang.StringBuilder,
    currentChar: Char,
    totalDataParsed: Int,
    clazzToMapValue: Class<T>,
    limitParsingTill: Int,
    nextChar: Char,
    predicate: Consumer<ProcessedData<T>>
  ): Int? {
    if(totalDataParsed == limitParsingTill || currentChar == ']' || nextChar == ']') {
      val validJson = filterValidJson(jsonValue)
      val listOfResult = FundBossModelMapper.mapModel(validJson, clazzToMapValue)
      predicate.accept(ProcessedData(listOfResult))
      return listOfResult.size
    }
    return null
  }



  /**
   * Filter out valid json
   */
  private fun filterValidJson(jsonValue: StringBuilder): String {
    if(jsonValue.last() != ']') jsonValue.append("]")
    jsonValue.append("\"")
    var validJson = jsonValue.toString().replace("[,", "[")
    validJson = validJson.replace(",]", "]") //This can happen when starting next iteration of data parsing
    validJson = validJson.replace("\"\"","\"")
    validJson = validJson.substring(1, validJson.length-1).replace("\\\"", "\"") // Remove opening and closing of double quotes
    return validJson
  }


  /**
   * Class for returning list of valid data and invalid data
   */
  data class ProcessedData<T>(val validData: List<T>)
}