package com.equiruswealth.commons.utils

import com.equiruswealth.commons.TestConstants
import com.equiruswealth.commons.constants.Constant
import com.equiruswealth.commons.enums.FileType
import com.equiruswealth.commons.utils.SheetUtils.readSheet
import com.google.gson.annotations.SerializedName
import com.univocity.parsers.annotations.Parsed
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

/**
 * @author Shashwat Singh
 */
internal class SheetUtilsTest {

  private fun validateData(profile: Profile): Boolean {
    if (profile.age == null) {
      return false
    }
    if (profile.country.isNullOrEmpty()) {
      return false
    } else if (profile.country == "Great Britain") {
      return false
    }
    return true
  }

  @Test
  fun readSheetTest_testingForValidationMethod() {
    val file = File(this.javaClass.classLoader.getResource(TestConstants.SheetUtilTestFiles.excelTest)!!.path)
    val errors = mutableListOf<SheetRowData>()
    val correctResults = mutableListOf<Profile>()
    val processedData = SheetUtils.readSheet(file, Profile::class.java, FileType.EXCEL, delimiter = null, ::validateData) {
      errors.addAll(it.invalidData)
      correctResults.addAll(it.validData)
      true
    }
    Assertions.assertThat(errors.size).isEqualTo(2)
    Assertions.assertThat(correctResults.size).isEqualTo(7)
    Assertions.assertThat(processedData).isEqualTo(9)
  }

  private fun validateBankDetails(bankDetails: BankDetails): Boolean {
    if (bankDetails.ifsc.isNullOrEmpty() || bankDetails.phone == null) {
      return false
    }
    return ValidationUtils.validateIfscCode(bankDetails.ifsc!!) && (10000.minus(bankDetails.phone!!)) < 0
  }

  /**
   * Testing for 4 ifsc missing and 1 complete row missing
   * Total Rows = 17
   */
  @Test
  fun readSheetTest_dataValidation() {
    val file =
      File(this.javaClass.classLoader.getResource(TestConstants.SheetUtilTestFiles.bankDetailsMissingIfsc)!!.path)
    val errors = mutableListOf<SheetRowData>()
    val correctResults = mutableListOf<BankDetails>()
    val processedData =
      SheetUtils.readSheet(file, BankDetails::class.java, FileType.EXCEL,  delimiter = null, ::validateBankDetails) {
        errors.addAll(it.invalidData)
        correctResults.addAll(it.validData)
        true
      }
    Assertions.assertThat(errors.size).isEqualTo(5)
    Assertions.assertThat(correctResults.size).isEqualTo(12)
    Assertions.assertThat(processedData).isEqualTo(17)
  }

  /**
   * Required excel file given csv (FileProcessingException)
   */
  @Test
  fun readSheetTest_throwInvalidFile_requiredExcel() {
    val file = File(this.javaClass.classLoader.getResource(TestConstants.SheetUtilTestFiles.schemeNavDetails)!!.path)
    Assertions.assertThatThrownBy {
      SheetUtils.readSheet(file, BankDetails::class.java, FileType.EXCEL,  delimiter = null, ::validateBankDetails) {
        true
      }
    }.hasMessage(Constant.FileProcessingExceptions.INVALID_FILE_RECEIVED)
  }

  /**
   * File Not Exists, path invalid (FileProcessingException) (EXCEL)
   */
  @Test
  fun readSheetTest_throwInvalidFile_invalidPath_excel() {
    Assertions.assertThatThrownBy {
      SheetUtils.readSheet(File("/test"), BankDetails::class.java, FileType.EXCEL,  delimiter = null, ::validateBankDetails) {
        true
      }
    }.hasMessage(Constant.FileProcessingExceptions.FILE_NOT_FOUND)
  }

  /**
   * File Not Exists, path invalid (FileProcessingException) (DBF)
   */
  @Test
  fun readSheetTest_throwInvalidFile_invalidPath_dbf() {
    Assertions.assertThatThrownBy {
      SheetUtils.readSheet(File("/test"), BankDetails::class.java, FileType.DBF,  delimiter = null, ::validateBankDetails) {
        true
      }
    }.hasMessage(Constant.FileProcessingExceptions.FILE_NOT_FOUND)
  }

  /**
   * File Not Exists, path invalid (FileProcessingException) (CSV)
   */
  @Test
  fun readSheetTest_throwInvalidFile_invalidPath_csv() {
    Assertions.assertThatThrownBy {
      SheetUtils.readSheet(File("/test"), BankDetails::class.java, FileType.CSV,  delimiter = null, ::validateBankDetails) {
        true
      }
    }.hasMessage(Constant.FileProcessingExceptions.FILE_NOT_FOUND)
  }

  /**
   * Testing for
   * 4 ifsc missing,
   * 3 ifsc incorrect (2 ifsc length Invalid and 1 ifsc 5 char != 0)
   * 3 complete row missing
   * 4 phone number missing
   * 2 phone number given invalid (InvalidFormatException)
   * Total Rows = 1001
   * invalid rows = 16
   * valid rows = 985
   */
  @Test
  fun readSheetTest_dataValidation_typeCastingErrors_excel() {
    val file =
      File(this.javaClass.classLoader.getResource(TestConstants.SheetUtilTestFiles.bankDetailsInvalidFieldsExcel)!!.path)
    val errors = mutableListOf<SheetRowData>()
    val correctResults = mutableListOf<BankDetails>()
    val processedData =
      SheetUtils.readSheet(file, BankDetails::class.java, FileType.EXCEL,  delimiter = null, ::validateBankDetails) {
        errors.addAll(it.invalidData)
        correctResults.addAll(it.validData)
        true
      }
    Assertions.assertThat(errors.size).isEqualTo(16)
    Assertions.assertThat(correctResults.size).isEqualTo(985)
    Assertions.assertThat(processedData).isEqualTo(1001)
  }

  @Test
  fun readSheetTest_dataValidation_typeCastingErrors_csv() {
    val file =
      File(this.javaClass.classLoader.getResource(TestConstants.SheetUtilTestFiles.bankDetailsInvalidFieldsCsv)!!.path)
    val errors = mutableListOf<SheetRowData>()
    val correctResults = mutableListOf<BankDetails>()
    val processedData = SheetUtils.readSheet(file, BankDetails::class.java, FileType.CSV,  delimiter = null, ::validateBankDetails) {
      errors.addAll(it.invalidData)
      correctResults.addAll(it.validData)
      true
    }
    Assertions.assertThat(errors.size).isEqualTo(16)
    Assertions.assertThat(correctResults.size).isEqualTo(985)
    Assertions.assertThat(processedData).isEqualTo(1001)
  }

  @Test
  fun readSheetTest_dataValidation_typeCastingErrors_dbf() {
    val file =
      File(this.javaClass.classLoader.getResource(TestConstants.SheetUtilTestFiles.bankDetailsInvalidFieldsDbf)!!.path)
    val errors = mutableListOf<SheetRowData>()
    val correctResults = mutableListOf<BankDetails>()
    val processedData = SheetUtils.readSheet(file, BankDetails::class.java, FileType.DBF,  delimiter = null, ::validateBankDetails) {
      errors.addAll(it.invalidData)
      correctResults.addAll(it.validData)
      true
    }
    Assertions.assertThat(errors.size).isEqualTo(16)
    Assertions.assertThat(correctResults.size).isEqualTo(985)
    Assertions.assertThat(processedData).isEqualTo(1001)
  }

  @Test
  fun readDbf_validateData() {
    val file = File(this.javaClass.classLoader.getResource(TestConstants.SheetUtilTestFiles.dbfSample)!!.path)
    val profiles = mutableListOf<CustomerInfo>()
    SheetUtils.readDbf(file, CustomerInfo::class.java) {
      if (it.validData.isNotEmpty()) {
        profiles.addAll(it.validData)
      }
      true
    }
    Assertions.assertThat(profiles.first().name).isEqualTo("Kauai Dive Shoppe")
  }

  @Test
  fun readDbf_validateLimit() {
    val file = File(this.javaClass.classLoader.getResource(TestConstants.SheetUtilTestFiles.dbfSample)!!.path)
    val result = SheetUtils.readDbf(file, CustomerInfo::class.java) {
      true
    }
    Assertions.assertThat(result).isEqualTo(48)
  }

	@Test
	fun readExcelRows_validateVariableHeaderIndex() {
		val file = File(this.javaClass.classLoader.getResource(TestConstants.SheetUtilTestFiles.excelTestWithVariableHeader)!!.path)
		val user = mutableListOf<User>()
		readSheet(file, clazz = User::class.java, fileType = FileType.EXCEL, validateData = null, headerRow = 6) {
			if (it.validData.isNotEmpty()) {
				user.addAll(it.validData)
			}
			true
		}
		Assertions.assertThat(user.first().name).isEqualTo("User1")
		Assertions.assertThat(user.size).isEqualTo(10)
	}
  @Test
  fun readExcelRows_validateData() {
    val file = File(this.javaClass.classLoader.getResource(TestConstants.SheetUtilTestFiles.excelTest)!!.path)
    val profiles = mutableListOf<Profile>()
    readSheet(file, clazz = Profile::class.java, fileType = FileType.EXCEL, validateData = null) {
      if (it.validData.isNotEmpty()) {
        profiles.addAll(it.validData)
      }
      true
    }
    Assertions.assertThat(profiles.first().firstName).isEqualTo("Dulce")
  }

  @Test
  fun readExcelRows_validateLimit() {
    val file = File(this.javaClass.classLoader.getResource(TestConstants.SheetUtilTestFiles.excelTest)!!.path)
    val result = readSheet(file, clazz = Profile::class.java, validateData = null, fileType = FileType.EXCEL) {
      true
    }
    Assertions.assertThat(result).isEqualTo(9)
  }

  @Test
  fun readSheetTest_testingExcelFormulatedSheet() {
    val file = File(this.javaClass.classLoader.getResource(TestConstants.SheetUtilTestFiles.formulatedExcel)!!.path)
    val parsedData = mutableListOf<Student>()
    val processedData = SheetUtils.readSheet(file, Student::class.java, FileType.EXCEL, delimiter = null, validateData = null) {
      parsedData.addAll(it.validData)
      true
    }
    val firstStudent = parsedData.first()
    Assertions.assertThat(firstStudent.maximumMarks).isEqualTo(600)
    Assertions.assertThat(firstStudent.obtainedMarks).isEqualTo(450)
    Assertions.assertThat(firstStudent.fees).isEqualTo(10245.0)
    Assertions.assertThat(firstStudent.analysis).isEqualTo(1900.0)
    Assertions.assertThat(firstStudent.analysisAsText).isEqualTo(190000.05)
    Assertions.assertThat(processedData).isEqualTo(5)
  }

  @Test
  fun readSheetTest_testingAnBigNumericCellWithStringMapper_expectedValueWithoutScientificAnnotation() {
    val file =  File(this.javaClass.classLoader.getResource(TestConstants.SheetUtilTestFiles.bigNumeriValue)!!.path)
    val parsedData = mutableListOf<MandateTest>()
    val processedData = SheetUtils.readSheet(file, MandateTest::class.java, FileType.EXCEL, delimiter = null, validateData = null) {
      parsedData.addAll(it.validData)
      true
    }

    val mandate = parsedData.find { it.clientCode == "TEST101" }!!
    Assertions.assertThat(mandate.mandateCode).isEqualTo("10104553")
    Assertions.assertThat(processedData).isEqualTo(3)
  }
}

class Student {
  @SerializedName("name")
  @Parsed(field=["name"])
  var name: String? = null

  @SerializedName("maximum marks")
  @Parsed(field=["maximum marks"])
  var maximumMarks: Int? = null

  @SerializedName("obtained marks")
  @Parsed(field=["obtained marks"])
  var obtainedMarks: Int? = null

  @SerializedName("percentage")
  @Parsed(field=["percentage"])
  var percentage: Double? = null

  @SerializedName("fees")
  @Parsed(field=["fees"])
  var fees: Double? = null

  @SerializedName("analysis")
  @Parsed(field=["analysis"])
  var analysis: Double? = null

  @SerializedName("analysis asText")
  @Parsed(field=["analysis asText"])
  var analysisAsText: Double? = null
}

class Profile() {
  @SerializedName("Id")
  @Parsed(field=["Id"])
  var id: String? = null

  @SerializedName("First Name")
  @Parsed(field=["First Name"])
  var firstName: String? = null

  @SerializedName("Last Name")
  @Parsed(field=["Last Name"])
  var lastName: String? = null

  @SerializedName("Gender")
  @Parsed(field=["Gender"])
  var gender: String? = null

  @SerializedName("Country")
  @Parsed(field=["Country"])
  var country: String? = null

  @SerializedName("Age")
  @Parsed(field=["Age"])
  var age: String? = null

  @SerializedName("Date")
  @Parsed(field=["Date"])
  var date: String? = null
}

class User() {
	@SerializedName("id")
	@Parsed(field=["id"])
	var id: String? = null

	@SerializedName("name")
	@Parsed(field=["name"])
	var name: String? = null
}

class BankDetails {

  @SerializedName("BANK")
  @Parsed(field=["BANK"])
  var name: String? = null

  @SerializedName("IFSC")
  @Parsed(field=["IFSC"])
  var ifsc: String? = null

  @SerializedName("BRANCH")
  @Parsed(field=["BRANCH"])
  var branch: String? = null

  @SerializedName("ADDRESS")
  @Parsed(field=["ADDRESS"])
  var address: String? = null

  @SerializedName("PHONE")
  @Parsed(field=["PHONE"])
  var phone: Long? = null
}

class CustomerInfo {

  @SerializedName("CUST_NO")
  @Parsed(field=["CUST_NO"])
  var id: Int? = null

  @SerializedName("NAME")
  @Parsed(field=["NAME"])
  var name: String? = null

  @SerializedName("STREET")
  @Parsed(field=["STREET"])
  var address: String? = null

  @SerializedName("CITY")
  @Parsed(field=["CITY"])
  var city: String? = null

  @SerializedName("STATE_PROV")
  @Parsed(field=["STATE_PROV"])
  var state: String? = null

  @SerializedName("COUNTRY")
  @Parsed(field=["COUNTRY"])
  var country: String? = null

  @SerializedName("ZIP_PST_CD")
  @Parsed(field=["ZIP_PST_CD"])
  var zipCode: Int? = null

  @SerializedName("PHONE")
  @Parsed(field=["PHONE"])
  var phone: String? = null
}

class MandateTest {
  @SerializedName("MANDATE CODE")
  @Parsed(field=["MANDATE CODE"])
  var mandateCode: String? = null

  @SerializedName("CLIENT CODE")
  @Parsed(field=["CLIENT CODE"])
  var clientCode: String? = null

  @SerializedName("CLIENT NAME")
  @Parsed(field=["CLIENT NAME"])
  var clientName: String? = null

  @SerializedName("MEMBER CODE")
  @Parsed(field=["MEMBER CODE"])
  var memberCode: String? = null

  @SerializedName("BANK NAME")
  @Parsed(field=["BANK NAME"])
  var bankCode: String? = null
}
