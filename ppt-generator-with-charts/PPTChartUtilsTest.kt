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
  private val templateFile = File(this.javaClass.classLoader.getResource(PROSPECT_TEMPLATE)!!.path)

  enum class ChartTypes(val id: Int) {
    ASSET_ALLOCATION(0),
    TOP_5_AMCS(1),
    PORTFOLIO_ALLOCATION(2),
    CATEGORY_WISE_ALLOCATION(3),
    PRODUCT_ALLOCATION(4);

    companion object {
      fun fromId(id: Int) = values().firstOrNull { it.id == id }!!
    }
  }

  private fun getCharts(): List<CategorizedChartModel> {
    return listOf(
      CategorizedChartModel().apply {
        this.id = 0
        this.name = "Asset Allocation"
        this.slideNumber = 0
        this.shapeId = 0
      },
      CategorizedChartModel().apply {
        this.id = 1
        this.name = "Top 5 AMCs"
        this.slideNumber = 1
        this.shapeId = 0
      },
      CategorizedChartModel().apply {
        this.id = 2
        this.name = "Portfolio Allocation"
        this.slideNumber = 2
        this.shapeId = 0
      },
      CategorizedChartModel().apply {
        this.id = 3
        this.name = "Category-wise Allocation"
        this.slideNumber = 2
        this.shapeId = 1
      },
      CategorizedChartModel().apply {
        this.id = 4
        this.name = "Product Allocation"
        this.slideNumber = 3
        this.shapeId = 0
      }
    )
  }

  /**
   * Method which provide category data based on chartId
   */
  private fun getChartData(): Map<ChartTypes, List<GenericChart<Double>>> {
    return mapOf(
      //Asset Allocation Chart Data
      Pair(
        ChartTypes.ASSET_ALLOCATION,
        listOf(
          GenericChart<Double>().apply {
            this.name = "Fixed Income"
            this.values = mutableListOf(49.29)
          },
          GenericChart<Double>().apply {
            this.name = "Equity"
            this.values = mutableListOf(21.9)
          },
          GenericChart<Double>().apply {
            this.name = "Real Estate"
            this.values = mutableListOf(16.35)
          },
          GenericChart<Double>().apply {
            this.name = "Other Assets"
            this.values = mutableListOf(4.84)
          },
          GenericChart<Double>().apply {
            this.name = "Debt"
            this.values = mutableListOf(3.1)
          },
          GenericChart<Double>().apply {
            this.name = "Cash and Equivalents"
            this.values = mutableListOf(2.36)
          },
          GenericChart<Double>().apply {
            this.name = "Offshore Investments"
            this.values = mutableListOf(2.16)
          },
          GenericChart<Double>().apply {
            this.name = "Aggressive Hybrid Funds"
            this.values = mutableListOf(0.0)
          }
        )
      ),

      //Top 5 AMCs Chart Data
      Pair(
        ChartTypes.TOP_5_AMCS,
        listOf(
          GenericChart<Double>().apply {
            this.name = "Aditya Birla Sunlife Asset Management Company Ltd."
            this.values = mutableListOf(39.60)
          },
          GenericChart<Double>().apply {
            this.name = "Sample sp"
            this.values = mutableListOf(14.55)
          },
          GenericChart<Double>().apply {
            this.name = "Kotak Mahindra Asset Management Company Ltd."
            this.values = mutableListOf(7.95)
          },
          GenericChart<Double>().apply {
            this.name = "SBI Funds Management Pvt Ltd."
            this.values = mutableListOf(7.81)
          },
          GenericChart<Double>().apply {
            this.name = "HDFC Asset Management Company Ltd."
            this.values = mutableListOf(4.64)
          }
        )
      ),

      //Portfolio Allocation Chart Data
      Pair(
        ChartTypes.PORTFOLIO_ALLOCATION,
        listOf(
          GenericChart<Double>().apply {
            this.name = "Fixed Income"
            this.values = mutableListOf(95.0, 83.0)
          },
          GenericChart<Double>().apply {
            this.name = "Offshore Investments"
            this.values = mutableListOf(40.0, 30.0)
          },
          GenericChart<Double>().apply {
            this.name = "Commodities"
            this.values = mutableListOf(19.0, 12.0)
          },
          GenericChart<Double>().apply {
            this.name = "Alternate Assets"
            this.values = mutableListOf(7.0, 2.0)
          },
          GenericChart<Double>().apply {
            this.name = "Equity"
            this.values = mutableListOf(2.0, 0.92)
          },
          GenericChart<Double>().apply {
            this.name = "Hybrid"
            this.values = mutableListOf(2.0, 0.83)
          }
        )
      ),

      //Category-wise Allocation Chart Data
      Pair(
        ChartTypes.CATEGORY_WISE_ALLOCATION,
        listOf(
          GenericChart<Double>().apply {
            this.name = "Structured Product"
            this.values = mutableListOf(95.0)
          },
          GenericChart<Double>().apply {
            this.name = "Discretionary Strategy"
            this.values = mutableListOf(40.0)
          },
          GenericChart<Double>().apply {
            this.name = "Offshore Investment MF"
            this.values = mutableListOf(15.0)
          },
          GenericChart<Double>().apply {
            this.name = "Bonds/Debentures"
            this.values = mutableListOf(5.0)
          },
          GenericChart<Double>().apply {
            this.name = "PE/VC Funds"
            this.values = mutableListOf(1.0)
          },
          GenericChart<Double>().apply {
            this.name = "Hybrid MF"
            this.values = mutableListOf(1.0)
          },
          GenericChart<Double>().apply {
            this.name = "REITs/InvITs"
            this.values = mutableListOf(1.0)
          },
          GenericChart<Double>().apply {
            this.name = "Unlisted Equity"
            this.values = mutableListOf(1.0)
          }
        )
      ),

      //Product Allocation Chart Data
      Pair(
        ChartTypes.PRODUCT_ALLOCATION,
        listOf(
          GenericChart<Double>().apply {
            this.name = "Direct Equity"
            this.values = mutableListOf(13.7)
          },
          GenericChart<Double>().apply {
            this.name = "AIF"
            this.values = mutableListOf(19.05)
          },
          GenericChart<Double>().apply {
            this.name = "Mutual Fund"
            this.values = mutableListOf(34.0)
          },
          GenericChart<Double>().apply {
            this.name = "Bonds and Debentures"
            this.values = mutableListOf(33.25)
          }
        )
      )
    )
  }

  @Test
  fun populateChartData_success() {
    val charts = getCharts().onEach {
      it.categorizedData = getChartData()[ChartTypes.fromId(it.id!!)]
    }
    val outputFile = PPTGeneratorUtils.populateChartData(
      existingPPT = templateFile,
      chartData = charts,
      outputFileName = outputFileName
    )
    Assertions.assertThat(outputFile.lengthInKb()).isGreaterThan(80.0)
  }

}