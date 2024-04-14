package com.equiruswealth.commons.utils

import com.equiruswealth.commons.TestConstants.PROSPECT_TEMPLATE
import com.equiruswealth.commons.models.chart.GenericChart
import com.equiruswealth.commons.models.ppt_generator.CategorizedChartModel
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Date

/**
 * @author Shashwat Singh
 */

object PPTChartUtilsTest {

  private val outputFileName = "John_Doe_Prospect_${Date().time}"

  @Test
  fun generatePPT_forPopulatingProspectHNIPPT() {
    val pptParametersJson = IOUtils.toString(ClassPathResource("ppt_template/pptTemplate.json").inputStream, StandardCharsets.UTF_8)
    val outputFile = PPTGenerator.generatePPT(
      outputFileName = "john_doe_prospect",
      pptParameters = JsonUtil.objectMapper.readValue(pptParametersJson, object : TypeReference<List<PowerPointParameter>>() {}),
      templateFileInputStream = this.javaClass.classLoader.getResourceAsStream("ppt_template/pptTemplate.pptx")!!
    )
    Assertions.assertThat(outputFile.lengthInMb()).isGreaterThan(7.5)
  }
}