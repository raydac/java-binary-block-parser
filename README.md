[![License Apache 2.0](https://img.shields.io/badge/license-Apache%20License%202.0-green.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.igormaznitsa/jbbp/badge.svg)](http://search.maven.org/#artifactdetails|com.igormaznitsa|jbbp|1.2.1|jar)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/50b0281430a94eb6abe417409f99ed58)](https://www.codacy.com/app/rrg4400/java-binary-block-parser)
[![Java 5.0+](https://img.shields.io/badge/java-5.0%2b-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![Android 2.0+](https://img.shields.io/badge/android-2.0%2b-green.svg)](http://developer.android.com/sdk/index.html)
[![PayPal donation](https://img.shields.io/badge/donation-PayPal-red.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)
[![Yandex.Money donation](https://img.shields.io/badge/donation-Я.деньги-yellow.svg)](https://money.yandex.ru/embed/small.xml?account=41001158080699&quickpay=small&yamoney-payment-type=on&button-text=01&button-size=l&button-color=orange&targets=%D0%9F%D0%BE%D0%B6%D0%B5%D1%80%D1%82%D0%B2%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BD%D0%B0+%D0%BF%D1%80%D0%BE%D0%B5%D0%BA%D1%82%D1%8B+%D1%81+%D0%BE%D1%82%D0%BA%D1%80%D1%8B%D1%82%D1%8B%D0%BC+%D0%B8%D1%81%D1%85%D0%BE%D0%B4%D0%BD%D1%8B%D0%BC+%D0%BA%D0%BE%D0%B4%D0%BE%D0%BC&default-sum=100&successURL=)

![JBBP Logo](https://github.com/raydac/java-binary-block-parser/blob/master/logo.png)

Introduction
============
Java has some embedded features to parse binary data (for instance ByteBuffer), but I wanted to work with separated bits and describe binary structure in some strong DSL(domain specific language). I was very impressed by the [the Python Struct package](https://docs.python.org/2/library/struct.html) package so that I decided to make something like that. So JBBP was born.<br>
p.s.<br>
For instance I have been very actively using the framework in [the ZX-Poly emulator](https://github.com/raydac/zxpoly) to parse snapshot files and save results.   
![Use cases](https://github.com/raydac/java-binary-block-parser/blob/master/docs/jbbp_mm.png)

Change log
===========
- **1.3.0-SNAPSHOT**
  - Class version target has been changed to Java 1.6
  - Minor refactoring
  
- **1.2.1 (28-JUL-2016)**
  - __Fixed issue [#10 "assertArrayLength throws exception in multi-thread"](https://github.com/raydac/java-binary-block-parser/issues/10), many thanks to @sky4star for the bug report.__
  - minor refactoring

- **1.2.0 (10-JUN-2015)**
  - Refactoring
  - Improved tree of JBBP exceptions
  - Fixed NPE in JBBPTextWriter for String field mapped to byte array 
  - Added support of custom field types through JBBPCustomFieldTypeProcessor
  - Added JBBPCustomFieldTypeProcessorAggregator, auxiliary class to join several JBBPCustomFieldTypeProcessors
  - Fixed JBBPTextWriter, added support of logging for JBBPAbstractField objects
  - Added support of payload objects in JBBPAbstractField
  - Improved inside script compiler and interpreter to support future extensions.
  - Fixed expression evaluator to support single char field names in expressions.
  - Added support of expressions in extra field numeric data part (example bit:(field*2))

- **1.1.0 (03-FEB-2015)**
  - Added support for mapped classes output into JBBPOut
  - Added JBBPTextWriter to log binary data as text with commentaries,tabs and separators
  - Fixed read byte counter, now it counts only fully processed bytes, if only several bits have been read from byte then the byte will not be counted until whole read
  - Fixed static fields including in mapping processes if class has marked by default Bin annotation
  - Added flag JBBPParser#FLAG_SKIP_REMAINING_FIELDS_IF_EOF to ignore remaining fields during parsing if EOF without exception
  - Added flag JBBPMapper#FLAG_IGNORE_MISSING_VALUES to ignore mapping for values which are not found in parsed source
  - Added new auxiliary methods in JBBPUtils 

- **1.0 (08-AUG-2014)**
  - The Initial version 
  
# Maven dependency
The Framework has been published in the Maven Central and can be easily added as a dependency
```
<dependency>
  <groupId>com.igormaznitsa</groupId>
  <artifactId>jbbp</artifactId>
  <version>1.2.1</version>
</dependency>
```
the precompiled library jar, javadoc and sources also can be downloaded directly from [the Maven central.](http://search.maven.org/#browse|808871750) 

# Hello world
The Framework is very easy in use because it has only two main classes for its functionality com.igormaznitsa.jbbp.JBBPParser (for data parsing) and com.igormaznitsa.jbbp.io.JBBPOut (for binary block writing), both of them work over low-level IO classes com.igormaznitsa.jbbp.io.JBBPBitInputStream and com.igormaznitsa.jbbp.io.JBBPBitOutputStream which are the core for the framework.   

The Easiest case below shows how to parse byte array to bits.   
```Java
  byte [] parsedBits = JBBPParser.prepare("bit:1 [_];").parse(new byte[]{1,2,3,4,5}).
          findFieldForType(JBBPFieldArrayBit.class).getArray();
```
Of course sometime it is not a comfortable way to look for parsed fields in the result, so you can use mapping of parsed data to class fields.
```Java
class Parsed {@Bin(type = BinType.BIT_ARRAY)byte[] parsed;}
Parsed parsedBits = JBBPParser.prepare("bit:1 [_] parsed;").parse(new byte[]{1,2,3,4,5}).mapTo(Parsed.class);
```

# More compex example with features added as of 1.1.0
The Example shows how to parse a byte written in non-standard MSB0 order (Java has LSB0 bit order) to bit fields, print its values and pack fields back 
```Java
class Flags {
      @Bin(outOrder = 1, name = "f1", type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1, comment = "It's flag one") byte flag1;
      @Bin(outOrder = 2, name = "f2", type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2, comment = "It's second flag") byte flag2;
      @Bin(outOrder = 3, name = "f3", type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1, comment = "It's 3th flag") byte flag3;
      @Bin(outOrder = 4, name = "f4", type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_4, comment = "It's 4th flag") byte flag4;
    }
    
    final int data = 0b10101010;
    Flags parsed = JBBPParser.prepare("bit:1 f1; bit:2 f2; bit:1 f3; bit:4 f4;", JBBPBitOrder.MSB0).parse(new byte[]{(byte)data}).mapTo(Flags.class);
    assertEquals(1,parsed.flag1);
    assertEquals(2,parsed.flag2);
    assertEquals(0,parsed.flag3);
    assertEquals(5,parsed.flag4);
    
    System.out.println(new JBBPTextWriter().Bin(parsed).Close().toString());
    
    assertEquals(data, JBBPOut.BeginBin(JBBPBitOrder.MSB0).Bin(parsed).End().toByteArray()[0] & 0xFF);
```
The Example will print in console the text below 
```
;--------------------------------------------------------------------------------
; START : Flags
;--------------------------------------------------------------------------------
    01; f1, It's flag one
    02; f2, It's second flag
    00; f3, It's 3th flag
    05; f4, It's 4th flag
;--------------------------------------------------------------------------------
; END : Flags
;--------------------------------------------------------------------------------
```
# Fields
Every field can have case insensitive name which should not contain '.' (because it is reserved for links to structure field values) and '#'(because it is also reserver for inside usage).
Field name must not be started by a number or chars '$' and '_'. *Field names are case insensitive!*
```
int someNamedField;
byte field1;
byte field2;
byte field3;
```
![JBBP field format, types and examples](https://github.com/raydac/java-binary-block-parser/blob/master/docs/jbbp_fields.png)

## Primitive types
The Framework supports full set of Java numeric primitives with extra types like ubyte and bit.
![JBBP field format, types and examples](https://github.com/raydac/java-binary-block-parser/blob/master/docs/jbbp_primitives.png)
## Complex types
The Framework provides support for arrays and structures. Just keep in mind that in expressions you can make links to field values only defined before expression.
![JBBP field format, types and examples](https://github.com/raydac/java-binary-block-parser/blob/master/docs/jbbp_complex_types.png)

## Custom types
it is possible to define processors for own custom data types, for instance you can take a look at [case processing three byte unsigned integer types](https://github.com/raydac/java-binary-block-parser/blob/master/src/test/java/com/igormaznitsa/jbbp/it/CustomThreeByteIntegerTypeTest.java).   

### Float and Double types
The Parser doesn't support Java float and double types out of the box. But it can be implemented through custom type processor. [there is written example and test and the code can be copy pasted](https://github.com/raydac/java-binary-block-parser/blob/master/src/test/java/com/igormaznitsa/jbbp/it/FloatAndDoubleTypesTest.java). 

## Variable fields
If you have some data which structure is variable then you can use the `var` type for defined field and process reading of the data manually with custom [JBBPVarFieldProcessor](https://github.com/raydac/java-binary-block-parser/blob/master/src/main/java/com/igormaznitsa/jbbp/JBBPVarFieldProcessor.java) instance.
```
    final JBBPParser parser = JBBPParser.prepare("short k; var; int;");
    final JBBPIntCounter counter = new JBBPIntCounter();
    final JBBPFieldStruct struct = parser.parse(new byte[]{9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(final JBBPBitInputStream inStream, final int arraySize, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }

      public JBBPAbstractField readVarField(final JBBPBitInputStream inStream, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        final int value = inStream.readByte();
        return new JBBPFieldByte(fieldName, (byte) value);
      }
    }, null);
```
*NB! Some programmers trying to use only parser for complex data, it is mistake. In the case it is much better to have several easy parsers working with the same [JBBPBitInputStream](https://github.com/raydac/java-binary-block-parser/blob/master/src/main/java/com/igormaznitsa/jbbp/io/JBBPBitInputStream.java) instance, it allows to keep decision points on Java level and make solution easier.*

## Special types
Special types makes some actions to skip data in input stream
![JBBP field format, types and examples](https://github.com/raydac/java-binary-block-parser/blob/master/docs/jbbp_special_fields.png)
## Byte order
Every multi-byte type can be read with different byte order.
![JBBP field format, types and examples](https://github.com/raydac/java-binary-block-parser/blob/master/docs/jbbp_byteorder.png)

# Expressions
Expressions are used for calculation of length of arrays and allow brackets and integer operators which work similar to Java operators:
- Arithmetic operators: +,-,%,*,/,%
- Bit operators: &,|,^,~
- Shift operators: <<,>>,>>>
- Brackets: (, ) 

Inside expression you can use integer numbers and named field values through their names (if you use fields from the same structure) or paths. Keep in your mind that you can't use array fields or fields placed inside structure arrays.
```
int field1;
   struct1 {
      int field2;
   }
   byte [field1+struct1.field2] data;
```

# Commentaries
You can use commentaries inside a parser script, the parser supports the only comment format and recognizes as commentaries all text after '//' till the end of line. 
```
 int;
    // hello commentaries
    byte field;
```

# Expression macroses
Inside expression you can use field names and field paths, also you can use the special macros '$$' which represents the current input stream byte counter, all fields started with '$' will be recognized by the parser as special user defined variables and it will be requesting them from special user defined provider. If the array size contains the only '_' symbol then the field or structure will not have defined size and whole stream will be read.

# How to get result of parsing
The Result of parsing is an instance of com.igormaznitsa.jbbp.model.JBBPFieldStruct class which represents the root invisible structure for the parsed data and you can use its inside methods to find desired fields for their names, paths or classes. All Fields are successors of com.igormaznitsa.jbbp.model.JBBPAbstractField class. To increase comfort, it is easier to use mapping to classes when the mapper automaticaly places values to fields of a Java class. 

# Example
The Example below shows how to parse a PNG file with the JBBP parser (the example taken from tests)
```Java
final InputStream pngStream = getResourceAsInputStream("picture.png");
    try {

      final JBBPParser pngParser = JBBPParser.prepare(
              "long header;"
              + "// chunks\n"
              + "chunk [_]{"
              + "   int length; "
              + "   int type; "
              + "   byte[length] data; "
              + "   int crc;"
              + "}"
      );

      final JBBPFieldStruct result = pngParser.parse(pngStream);
      
      assertEquals(0x89504E470D0A1A0AL,result.findFieldForNameAndType("header",JBBPFieldLong.class).getAsLong());
      
      final JBBPFieldArrayStruct chunks = result.findFieldForNameAndType("chunk", JBBPFieldArrayStruct.class);
      
      
      final String [] chunkNames = new String[]{"IHDR","gAMA","bKGD","pHYs","tIME","tEXt","IDAT","IEND"};
      final int [] chunkSizes = new int[]{0x0D, 0x04, 0x06, 0x09, 0x07, 0x19, 0x0E5F, 0x00};
      
      assertEquals(chunkNames.length,chunks.size());
      
      for(int i=0;i<chunks.size();i++){
        assertChunk(chunkNames[i], chunkSizes[i], (JBBPFieldStruct)chunks.getElementAt(i));
      }
    }
    finally {
      closeResource(pngStream);
    }
```
Also it is possible to map parsed packet to class fields 
```Java
final JBBPParser pngParser = JBBPParser.prepare(
              "long header;"
              + "chunk [_]{"
              + "   int length; "
              + "   int type; "
              + "   byte[length] data; "
              + "   int crc;"
              + "}"
      );

      class Chunk {
        @Bin int length;
        @Bin int type;
        @Bin byte [] data;
        @Bin int crc;
      }

      @Bin  
      class Png {
        long header;
        Chunk [] chunk;
      }
      
      final Png png = pngParser.parse(pngStream).mapTo(Png.class);
```
The Example from tests shows how to parse a tcp frame wrapped in a network frame 
```Java
final JBBPParser tcpParser = JBBPParser.prepare(
              "skip:34; // skip bytes till the frame\n"
              + "ushort SourcePort;"
              + "ushort DestinationPort;"
              + "int SequenceNumber;"
              + "int AcknowledgementNumber;"
                      
              + "bit:1 NONCE;"
              + "bit:3 RESERVED;"
              + "bit:4 HLEN;"
                      
              + "bit:1 FIN;"
              + "bit:1 SYN;"
              + "bit:1 RST;"
              + "bit:1 PSH;"
              + "bit:1 ACK;"
              + "bit:1 URG;"
              + "bit:1 ECNECHO;"
              + "bit:1 CWR;"
                      
              + "ushort WindowSize;"
              + "ushort TCPCheckSum;"
              + "ushort UrgentPointer;"
              + "byte [$$-34-HLEN*4] Option;"
              + "byte [_] Data;"
      );

      final JBBPFieldStruct result = pngParser.parse(tcpFrameStream);
```
# F.A.Q.
## Is it possible to use `@Bin` annotations for parsing and not only mapping?
No, `@Bin` annotations in classes are used only for mapping and data writing, but there is [the code snippet](https://gist.github.com/raydac/28d770307bd33683aa17ea3c39d5e2c4) allows to generate JBBP DSL based on detected @Bin annotations in class.

## My Binary data format is too complex one to be decoded by a JBBP script
No problems! The Parser works over com.igormaznitsa.jbbp.io.BitInputStream class which can be used directly and allows read bits, bytes, count bytes and align data from a stream.

## I want to make a bin block instead of parsing!
The Framework contains a special helper as the class com.igormaznitsa.jbbp.io.JBBPOut which allows to build bin blocks with some kind of DSL 
```Java
import static com.igormaznitsa.jbbp.io.JBBPOut.*;
...
final byte [] array = 
          BeginBin().
            Bit(1, 2, 3, 0).
            Bit(true, false, true).
            Align().
            Byte(5).
            Short(1, 2, 3, 4, 5).
            Bool(true, false, true, true).
            Int(0xABCDEF23, 0xCAFEBABE).
            Long(0x123456789ABCDEF1L, 0x212356239091AB32L).
          End().toByteArray();
```
# Public snapshot repository for the library
To make accessible the snapshot version of the library during development, I have tuned public maven snapshot repository which can be added into project with snippet
```xml
<repositories>
 <repository>
  <id>coldcore.ru-snapshots</id>
  <name>ColdCore.RU Mvn Snapshots</name>
  <url>http://coldcore.ru/m2</url>
  <snapshots>
   <enabled>true</enabled>
  </snapshots>
  <releases>
   <enabled>false</enabled>
  </releases>
 </repository>
</repositories>
```
