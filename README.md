# Kotlin Utility Classes for Spring Boot Applications

This repository is a collection of reusable Kotlin utility classes designed to streamline common tasks within Spring Boot applications. It's built with the love for open-source development and the belief in collaborative efforts. We encourage contributions from the community to improve the functionality and broaden the range of supported tasks.

## Functionalities Offered:

## 1. Image Processing (ImageUtils.kt)

- Offers functionalities for image compression and decompression while maintaining desired constraints.
- Optimizes image storage and delivery, balancing quality and file size.

## 2. File Processing (SheetUtils.kt)

- Enables processing CSV, Excel, and DBF files into structured objects mapped to your custom classes.
- Simplifies data ingestion and integration tasks within your applications.

## 3. JSON Parsing (JsonParser.kt)

- Provides efficient parsing of large JSON files leveraging producer-consumer concepts.
- Processes data in chunks to handle memory limitations for massive files.
- Focuses on extracting specific data based on a provided key.

## 4. PDF to PPTX Conversion (ConvertFromPDFtoPPTX.kt)

- Converts PDF files into presentation slides (PPTX format).
- Renders each PDF page as an image and inserts those images into a new presentation.


## Usage:

<details>
<summary><strong>  Installation: </strong></summary>

1. Clone the repository:
    ```
    git clone https://github.com/Shashwatsingh22/utility-methods
    ```
2. Integrate it as a dependency within your Spring Boot application using your build system (Maven or Gradle).
</details>


<details>
<summary><strong> Individual Class Usage:: </strong></summary>


- #### SheetUtils
- Import the `SheetUtils` object into your main file.
- Read a CSV file:
```kotlin
import com.yourpackage.utils.SheetUtils


val processedData = SheetUtils.readSheet(
    file = myCsvFile,
    clazz = MyDataClass::class.java,
    fileType = SheetUtils.FileType.CSV
).
```
- Read an Excel file with a custom header row:
```

val processedData = SheetUtils.readSheet(
    file = myExcelFile,
    clazz = MyDataClass::class.java,
    fileType = SheetUtils.FileType.EXCEL,
    headerRow = 2  // Header row is at index 2 (zero-based)
)
```
- Access parsed data:
```

val validData = processedData.validData
val invalidData = processedData.invalidData
```

- #### ImageUtils

- Import the `ImageUtils` object into your main file.
- Use the `resizeImage` function to create a resized image file, providing:
  - The original image file path.
  - The desired maximum file size in KB.
  - The minimum acceptable quality (0.0 to 1.0).
  - The maximum acceptable resolution (width or height).

```kotlin
val resizedImage = ImageUtils.resizeImage(originalImageFile, 500.0, 0.8f, 1024)
```
- #### JsonParser
 Please refer to JSONParser Readme for detailed usage
- #### ConvertFromPDFToPPTX
 Please refer to ConvertFromPDFToPPTX Readme for detailed usage
</details>

### Testing:

Each utility class (`ImageUtils.kt`, `SheetUtils.kt`, `JsonParser.kt`, `ConvertFromPDFtoPPTX.kt`) comes with accompanying test files to ensure their proper functionality.

- Execute these tests using your preferred testing framework (JUnit, Kotest) to verify the code's correctness.

