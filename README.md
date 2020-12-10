![JBBP Logo](https://github.com/raydac/java-binary-block-parser/blob/master/logo.png)

[![License Apache 2.0](https://img.shields.io/badge/license-Apache%20License%202.0-green.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.igormaznitsa/jbbp/badge.svg)](http://search.maven.org/#artifactdetails|com.igormaznitsa|jbbp|2.0.2|jar)
[![Java 1.8+](https://img.shields.io/badge/java-1.8%2b-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![Android 3.0+](https://img.shields.io/badge/android-3.0%2b-green.svg)](http://developer.android.com/sdk/index.html)
[![PayPal donation](https://img.shields.io/badge/donation-PayPal-red.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)
[![Yandex.Money donation](https://img.shields.io/badge/donation-Я.деньги-yellow.svg)](http://yasobe.ru/na/iamoss)

# Introduction
Java has some embedded features to parse binary data (for instance ByteBuffer), but sometime it is needed to work on bit level and describe binary structures through some DSL(domain specific language). I was impressed by the [the Python Struct package](https://docs.python.org/2/library/struct.html) package and wanted to get something like that for Java. So I developed the JBBP library.<br>
![Use cases](https://github.com/raydac/java-binary-block-parser/blob/master/docs/jbbp_mm.png)

# Change log
- __2.0.3 (SNAPSHOT)__
  - added service methods `JBBPUtils.traceData` to print dump of an input stream into a PrintStream
  - improved JBBPTokenizerException to show marked error position [#30](https://github.com/raydac/java-binary-block-parser/issues/30)

- __2.0.2 (22-aug-2020)__
  - added `JBBPOut#Bin` variant to override `@Bin` annotation fields in written objects. 
  - [#28](https://github.com/raydac/java-binary-block-parser/issues/28) added `JBBPOut#BinForceByteOrder` to override byte order defined in `@Bin` annotations of written object.

- __2.0.1 (04-feb-2020)__
  - [#26](https://github.com/raydac/java-binary-block-parser/issues/26) fixed bug in array write with MSB0 

[Full changelog](https://github.com/raydac/java-binary-block-parser/blob/master/changelog.txt)   

# Maven dependency
The Framework has been published in the Maven Central and can be easily added as a dependency
```
<dependency>
  <groupId>com.igormaznitsa</groupId>
  <artifactId>jbbp</artifactId>
  <version>2.0.2</version>
</dependency>
```
the precompiled library jar, javadoc and sources also can be downloaded directly from [the Maven central.](https://search.maven.org/artifact/com.igormaznitsa/jbbp/2.0.2/jar)

# Hello world
The library is very easy in use because in many cases only two its classes are needed - com.igormaznitsa.jbbp.JBBPParser (for data parsing) and com.igormaznitsa.jbbp.io.JBBPOut (for binary block writing). Both these classes work over low-level IO classes - com.igormaznitsa.jbbp.io.JBBPBitInputStream and com.igormaznitsa.jbbp.io.JBBPBitOutputStream, those bit stream classes are the core of the library.   

The easiet use case shows parsing of whole byte array to bits.   
```Java
  byte [] parsedBits = JBBPParser.prepare("bit:1 [_];").parse(new byte[]{1,2,3,4,5}).
          findFieldForType(JBBPFieldArrayBit.class).getArray();
```
On start it was the only functionality but then I found that it is no so comfort way to get result, so that added some mapping of parsed result to pre-instantiated object. It works slower, because uses a lot of Java reflection but much easy in some cases.
```Java
class Parsed {@Bin(type = BinType.BIT_ARRAY)byte[] parsed;}
Parsed parsedBits = JBBPParser.prepare("bit:1 [_] parsed;").parse(new byte[]{1,2,3,4,5}).mapTo(new Parsed());
```

# Relative speed of different approaches in parsing
Mainly I developed the library to help in my development of ZX-Spectrum emulator where I needed to work with data snapshots containing data on bit level. It didn't need much productivity in work. But since 1.3.0 version I added way to generate Java classes from JBBP scripts, such classes work in about five times faster than dynamic parsing and mapping approaches.  
![JMH results](https://github.com/raydac/java-binary-block-parser/blob/master/docs/jmh_results.png)   
Chart below compares speed of three provided ways to parse data with JBBP:
* __Dynamic__ - the basic parsing through interpretation of prepared JBBP DSL script. It is no so fast, but provide way to generate parsers on fly from text description.
* __Dynamic + map to class__ - parsing through interpretation of parsed JBBP script and mapping of parsed data to pre-instantiated class instance. It provides compfortable way to work with data and get result but uses a lot of Java reflection features and so fast.
* __Static class__ - the fastest way of JBBP use, some JBBP script is translated into Java class. There is no any interpretation or reflection operators so that it is very fast. [You can take a look at auxiliary class which I use in tests](https://github.com/raydac/java-binary-block-parser/blob/master/jbbp/src/test/java/com/igormaznitsa/jbbp/testaux/AbstractJBBPToJavaConverterTest.java).


# Generate sources from JBBP scripts
Since 1.3.0 version, the library provides Java source generator for JBBP scripts, __(keep in mind that generated sources anyway depends on JBBP library and it is needed for their work)__.
For instance such snippet can be used to generate Java classes from a JBBP script. It also can generate multiple classes.
```Java
  JBBPParser parser = JBBPParser.prepare("byte a; byte b; byte c;");
  List<ResultSrcItem> generated = parser.convertToSrc(TargetSources.JAVA,"com.test.jbbp.gen.SomeClazz");
  for(ResultSrcItem i : generated) {
     for(Map.Entry<String,String> j :i.getResult().entrySet()) {
        System.out.println("Class file name "+j.getKey());                
        System.out.println("Class file content "+j.getValue());                
     }
  }
```
also there are developed plug-ins for both Maven and Gradle to generate sources from JBBP scripts during source generate phase.   
in Maven it can be used through snippet:
```xml
 <plugin>
   <groupId>com.igormaznitsa</groupId>
   <artifactId>jbbp-maven-plugin</artifactId>
   <version>2.0.2</version>
   <executions>
     <execution>
       <id>gen-jbbp-src</id>
       <goals>
         <goal>generate</goal>
       </goals>
     </execution>
   </executions>
</plugin>
```
By default the maven plug-in looks for files with `jbbp` extension in `src/jbbp` folder of the project (it can be changed through plug-in configuration) and produces resulting java classes into `target/generated-sources/jbbp` folder. [For instance, I use such approach in my ZX-Poly emulator](https://github.com/raydac/zxpoly/tree/master/zxpoly-emul/src/jbbp).

# More complex example with features added as of 1.1.0
Example below shows how to parse a byte stream written in non-standard MSB0 order (Java has LSB0 bit order) into bit fields, then print its values and pack fields back:
```Java
class Flags {
      @Bin(order = 1, name = "f1", type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_1, comment = "It's flag one") byte flag1;
      @Bin(order = 2, name = "f2", type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_2, comment = "It's second flag") byte flag2;
      @Bin(order = 3, name = "f3", type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_1, comment = "It's 3th flag") byte flag3;
      @Bin(order = 4, name = "f4", type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_4, comment = "It's 4th flag") byte flag4;
    }

    final int data = 0b10101010;
    Flags parsed = JBBPParser.prepare("bit:1 f1; bit:2 f2; bit:1 f3; bit:4 f4;", JBBPBitOrder.MSB0).parse(new byte[]{(byte)data}).mapTo(new Flags());
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
Each field can have case insensitive name which must not contain '.' (because dot is reserved for links to structure field values) and '#'(because it is also reserved for internal library use).
A field name must not be started with either number or chars '$' and '_'. *Keep in mind that field names are case insensitive!*
```
int someNamedField;
byte field1;
byte field2;
byte field3;
```
![JBBP field format, types and examples](https://github.com/raydac/java-binary-block-parser/blob/master/docs/jbbp_fields.png)

## Primitive types
JBBP supports full set of Java numeric primitives with some extra types like ubyte and bit.
![JBBP field format, types and examples](https://github.com/raydac/java-binary-block-parser/blob/master/docs/jbbp_primitives.png)
## Complex types
JBBP provides support both arrays and structures. __In expressions you can use links only to field values which already read!__
![JBBP field format, types and examples](https://github.com/raydac/java-binary-block-parser/blob/master/docs/jbbp_complex_types.png)

## Custom types
It is possible to define processors for custom data types. For instance you can take a look at [case processing three byte unsigned integer types](https://github.com/raydac/java-binary-block-parser/blob/master/jbbp/src/test/java/com/igormaznitsa/jbbp/it/CustomThreeByteIntegerTypeTest.java).   

### Support of float, double and String types
Since 1.4.0 in JBBP was added support of Java float, double and String values. Because they have specific format, they are named as `doublej`, `floatj` and `stringj`.

## Variable fields
If you have some data which internal structure is undefined and variable then you can use the `var` type to mark such field and provide custom processor to read data of such value. Processor should implement interface [JBBPVarFieldProcessor](https://github.com/raydac/java-binary-block-parser/blob/master/src/main/java/com/igormaznitsa/jbbp/JBBPVarFieldProcessor.java) instance.
```
    final JBBPParser parser = JBBPParser.prepare("short k; var; int;");
    final JBBPIntCounter counter = new JBBPIntCounter();
    final JBBPFieldStruct struct = parser.parse(new byte[]{9, 8, 33, 1, 2, 3, 4}, new JBBPVarFieldProcessor() {

      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(JBBPBitInputStream inStream, int arraySize, JBBPNamedFieldInfo fieldName, int extraValue, JBBPByteOrder byteOrder, JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }

      public JBBPAbstractField readVarField(JBBPBitInputStream inStream, JBBPNamedFieldInfo fieldName, int extraValue, JBBPByteOrder byteOrder, JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        final int value = inStream.readByte();
        return new JBBPFieldByte(fieldName, (byte) value);
      }
    }, null);
```
*NB! Some programmers trying to use only parser for complex data, it is a mistake. In the case it is much better to have several easy parsers working with the same [JBBPBitInputStream](https://github.com/raydac/java-binary-block-parser/blob/master/src/main/java/com/igormaznitsa/jbbp/io/JBBPBitInputStream.java) instance, it allows to keep decision points on Java level and make solution easier.*

## Special types
Special types makes some actions to skip data in input stream
![JBBP field format, types and examples](https://github.com/raydac/java-binary-block-parser/blob/master/docs/jbbp_special_fields.png)

## Byte order
Multi-byte types can be read with different byte order.
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
The Result of parsing is an instance of com.igormaznitsa.jbbp.model.JBBPFieldStruct class which represents the root invisible structure for the parsed data and you can use its inside methods to find desired fields for their names, paths or classes. All Fields are successors of com.igormaznitsa.jbbp.model.JBBPAbstractField class. To increase comfort, it is easier to use mapping to classes when the mapper automatically places values to fields of a Java class.

# Example
Example below shows how to parse a PNG file through JBBP parser:
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

      JBBPFieldStruct result = pngParser.parse(pngStream);

      assertEquals(0x89504E470D0A1A0AL,result.findFieldForNameAndType("header",JBBPFieldLong.class).getAsLong());

      JBBPFieldArrayStruct chunks = result.findFieldForNameAndType("chunk", JBBPFieldArrayStruct.class);

      String [] chunkNames = new String[]{"IHDR","gAMA","bKGD","pHYs","tIME","tEXt","IDAT","IEND"};
      int [] chunkSizes = new int[]{0x0D, 0x04, 0x06, 0x09, 0x07, 0x19, 0x0E5F, 0x00};

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

        public Object newInstance(Class<?> klazz){
          return klazz == Chunk.class ? new Chunk() : null;
        }
      }

      final Png png = pngParser.parse(pngStream).mapTo(new Png());
```

Example shows how to parse TCP frame:
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
`@Bin` annotations is used only for mapping and data writing, but there is special class [JBBPDslBuilder](/jbbp/src/main/java/com/igormaznitsa/jbbp/utils/JBBPDslBuilder.java) which can convert `@Bin` marked class into JBBP script, for instance:
```java
JBBPDslBuilder.Begin().AnnotatedClass(SomeBinAnnotatetClass.class).End(true);
```

## My Binary data format is too complex one to be decoded by a JBBP script
No problems! JBBP parser works over [com.igormaznitsa.jbbp.io.JBBPBitInputStream](/jbbp/src/main/java/com/igormaznitsa/jbbp/io/JBBPBitInputStream.java) class which can be used directly and allows read bits, bytes, count bytes and align data. For writing there is similar class [JBBPBitOutputStream](https://github.com/raydac/java-binary-block-parser/blob/master/jbbp/src/main/java/com/igormaznitsa/jbbp/io/JBBPBitOutputStream.java).

## I want to make a binary data block instead of parsing!
Library provides special helper [JBBPOut](/jbbp/src/main/java/com/igormaznitsa/jbbp/io/JBBPOut.java). The helper allows to generate binary blocks and provides some kind of DSL
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
