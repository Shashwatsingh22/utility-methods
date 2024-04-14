# Utility Classes for Streamlining Development

This repository hosts a collection of reusable utility classes designed to simplify common tasks across various programming languages and technology stacks. Whether you're working with `Kotlin, Java, Python, JavaScript, or any other language`, these utility classes aim to enhance productivity and streamline development workflows.

Built with the love for open-source development and the belief in collaborative efforts, this project welcomes contributions from the community. By collaborating together, we can improve existing functionalities and expand the range of supported tasks to benefit developers across different domains and ecosystems.

Feel free to explore the existing utility classes, contribute your own, or suggest improvements to make this repository even more valuable for developers worldwide.

## Functionalities Offered:

#### 1. Image Processing (ImageUtils.kt)

- Offers functionalities for image compression and decompression while maintaining desired constraints.
- Optimizes image storage and delivery, balancing quality and file size.

#### 2. File Processing (SheetUtils.kt)

- Enables processing CSV, Excel, and DBF files into structured objects mapped to your custom classes.
- Simplifies data ingestion and integration tasks within your applications.

#### 3. JSON Parsing (JsonParser.kt)

- Provides efficient parsing of large JSON files leveraging producer-consumer concepts.
- Processes data in chunks to handle memory limitations for massive files.
- Focuses on extracting specific data based on a provided key.

#### 4. Power Point Generator (PPTGenerator.kt)

- Create Dynamic PPT by the help of json by keeping an sample PPT format (PPTX format).

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
- #### PPTGenerator
 Please refer to PPT generator Readme for details usage
</details>

## Testing:

Each utility class (`ImageUtils.kt`, `SheetUtils.kt`, `JsonParser.kt`, `PPTGenerator.kt`) comes with accompanying test files to ensure their proper functionality.

## Contributing

We welcome your contributions to this project! Here are some ways you can get involved:

- **Report issues:** If you encounter any bugs or unexpected behavior, please file an issue on the repository's issue tracker.
  
- **Suggest improvements:** If you have ideas for new features or enhancements, feel free to create an issue or pull request.
  
- **Contribute code:** If you're a Kotlin developer, you can submit pull requests with your code contributions. Make sure to follow the coding conventions and style guide of the project.
  
- **Spread the word:** Let other developers know about this collection of utility classes and encourage them to use and contribute.

