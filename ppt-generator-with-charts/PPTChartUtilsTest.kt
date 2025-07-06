package com.equiruswealth.commons.utils.ppt

import com.fasterxml.jackson.core.type.TypeReference
import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.mockito.InjectMocks
import org.mockito.MockitoAnnotations
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.nio.charset.StandardCharsets
import org.mockito.Mock

/**
 * @author Shashwat Singh
 */
object class PowerPointGeneratorUtilsTest {

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  /**
   * Test 1 - All shapes data available will populate all of them.
   * Assert That -
   *    - Where only not required label which not have data should get deleted
   *    - Else all shapes data should get populated
   */
  @Test
  fun generatePPT_populateAllData() {
    val slideWiseDetailsJson = IOUtils.toString(ClassPathResource(PROSPECT_TEMPLATE_JSON_TEST_1).inputStream, StandardCharsets.UTF_8)
    val slides = JsonUtil.objectMapper.readValue(slideWiseDetailsJson, object : TypeReference<List<Slide>>() {})
    val outputFile = PowerPointGeneratorUtils.generatePPT(
      pptParameters = PPTParameters().apply {
        this.fileName = "john_doe_prospect"
        this.slides = slides
      },
      templateFileInputStream = this.javaClass.classLoader.getResourceAsStream(PROSPECT_TEMPLATE_TEST_1)!!
    )
    Assertions.assertThat(outputFile.lengthInKb()).isGreaterThan(10.0)
  }

  /**
   * Test 2 - Method to reposition the shapes and remove the deleted shapes
   * Assert That -
   *    - Slide [portfolioSummarySlide] - Asset Allocation Chart Gets Deleted and table get repositioned to mid
   *    - Slide [productAllocationSlide] - Both shape not have any data then slide gets deleted
   */
  @Test
  fun generatePPT_withDeletedShapeRepositionDone() {
    val slideWiseDetailsJson = IOUtils.toString(ClassPathResource(PROSPECT_TEMPLATE_JSON_TEST_2).inputStream, StandardCharsets.UTF_8)
    val slides = JsonUtil.objectMapper.readValue(slideWiseDetailsJson, object : TypeReference<List<Slide>>() {})
    val outputFile = PowerPointGeneratorUtilsTest.generatePPT(
      pptParameters = PPTParameters().apply {
        this.fileName = "john_doe_prospect"
        this.slides = slides
      },
      templateFileInputStream = this.javaClass.classLoader.getResourceAsStream(PROSPECT_TEMPLATE_TEST_1)!!
    )
    Assertions.assertThat(outputFile.lengthInKb()).isGreaterThan(10.0)
  }
}