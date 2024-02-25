package com.equiruswealth.fundboss.utils

import com.equiruswealth.commons.utils.JsonUtil
import com.equiruswealth.fundboss.mapper.FundBossModelMapper
import com.equiruswealth.fundboss.models.FundbossApiResponse
import com.equiruswealth.fundboss.models.FundbossHolding
import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.nio.charset.StandardCharsets

class JsonParserTest {

  private val casStatementJsonPath = "cas-statement/AAAPA0000A.json"

  @Test
  fun parseReportTable_testingWithDataWithLimitOfParsing4() {
    val file = File(this.javaClass.classLoader.getResource(casStatementJsonPath)!!.path)
    val validParsedData = mutableListOf<FundbossHolding>()
    val processedData = JsonParser.parseReportTable(file, FundbossHolding::class.java, "ReportTable", 4) {
      validParsedData.addAll(it.validData)
    }
    val casJson = getFileContents(casStatementJsonPath)
    val fundbossApiResponse = JsonUtil.objectMapper.readValue(casJson, FundbossApiResponse::class.java)
    val holdings = FundBossModelMapper.mapModel(fundbossApiResponse.reportTable!!, FundbossHolding::class.java)
    Assertions.assertThat(validParsedData.size).isEqualTo(holdings.size)
    Assertions.assertThat(processedData).isEqualTo(holdings.size)
  }

  @Test
  fun parseReportTable_testingWithDataWithLimitOfParsing2() {
    val file = File(this.javaClass.classLoader.getResource(casStatementJsonPath)!!.path)
    val validParsedData = mutableListOf<FundbossHolding>()
    val processedData = JsonParser.parseReportTable(file, FundbossHolding::class.java, "ReportTable", 2) {
      validParsedData.addAll(it.validData)
    }
    val casJson = getFileContents(casStatementJsonPath)
    val fundbossApiResponse = JsonUtil.objectMapper.readValue(casJson, FundbossApiResponse::class.java)
    val holdings = FundBossModelMapper.mapModel(fundbossApiResponse.reportTable!!, FundbossHolding::class.java)
    Assertions.assertThat(validParsedData.size).isEqualTo(holdings.size)
    Assertions.assertThat(processedData).isEqualTo(holdings.size)
  }

  @Test
  fun parseReportTable_testingWithEmptyData() {
    val file = File(this.javaClass.classLoader.getResource(casStatementJsonPath)!!.path)
    val validParsedData = mutableListOf<FundbossHolding>()
    val processedData = JsonParser.parseReportTable(file, FundbossHolding::class.java, "ReportTable1", 3) {
      validParsedData.addAll(it.validData)
    }
    val casJson = getFileContents(casStatementJsonPath)
    val fundbossApiResponse = JsonUtil.objectMapper.readValue(casJson, FundbossApiResponse::class.java)
    val holdings = FundBossModelMapper.mapModel(fundbossApiResponse.reportTable1!!, FundbossHolding::class.java)
    Assertions.assertThat(validParsedData.size).isEqualTo(holdings.size)
    Assertions.assertThat(processedData).isEqualTo(holdings.size)
    Assertions.assertThat(validParsedData).isEmpty()
  }

  /**
   * Method to get the file content
   */
  private fun getFileContents(path: String): String {
    return IOUtils.toString(ClassPathResource(path).inputStream, StandardCharsets.UTF_8)
  }
}