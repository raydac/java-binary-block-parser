[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.igormaznitsa/jbbp/badge.svg)](http://search.maven.org/#artifactdetails|com.igormaznitsa|jbbp|1.2.0|jar)   
[![Codacy Badge](https://api.codacy.com/project/badge/grade/50b0281430a94eb6abe417409f99ed58)](https://www.codacy.com/app/rrg4400/java-binary-block-parser)   

![JBBP Logo](https://github.com/raydac/java-binary-block-parser/blob/master/logo.png)

Introduction
=============
It is very often in my projects when I needs to parse some binary data in Java. Java has some embedded features for that (for instance ByteBuffer), but I wanted to work with bits and describe binary structure in some domain specific language. I was very impressed by the [the Python Struct package](https://docs.python.org/2/library/struct.html) package so that I decided to make something like that. So JBBP was born.<br>
p.s.<br>
For instance I have been very actively using the framework in [the ZX-Poly emulator](https://github.com/raydac/zxpoly) to parse snapshot files and save results.   
![Use cases](https://github.com/raydac/java-binary-block-parser/blob/master/docs/jbbp_mm.png)

License
========
The Framework is under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Change log
===========
- **1.2.0**
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
- **1.1.0**
  - Added support for mapped classes output into JBBPOut
  - Added JBBPTextWriter to log binary data as text with commentaries,tabs and separators
  - Fixed read byte counter, now it counts only fully processed bytes, if only several bits have been read from byte then the byte will not be counted until whole read
  - Fixed static fields including in mapping processes if class has marked by default Bin annotation
  - Added flag JBBPParser#FLAG_SKIP_REMAINING_FIELDS_IF_EOF to ignore remaining fields during parsing if EOF without exception
  - Added flag JBBPMapper#FLAG_IGNORE_MISSING_VALUES to ignore mapping for values which are not found in parsed source
  - Added new auxiliary methods in JBBPUtils 
- **1.0**
  - The Initial version 
  
Compatibility
=============
The Framework is compatible with:
* Java 1.5+
* Android 2.0+
For mapping sometime it uses reflection.
  
Maven dependency
======================
The Framework is published in the Maven Central and can be easily added as a dependency into a maven project
```
<dependency>
  <groupId>com.igormaznitsa</groupId>
  <artifactId>jbbp</artifactId>
  <version>1.2.0</version>
</dependency>
```
the precompiled library jar, javadoc and sources also can be downloaded directly from [the Maven central.](http://search.maven.org/#browse|808871750) 

Hello world
============
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

More compex example with features added as of 1.1.0
====================================================
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
Fields
==================
![JBBP field format, types and examples](https://github.com/raydac/java-binary-block-parser/blob/master/docs/jbbp_fields.png)

Expressions
============
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

Field names
============
You can use any chars for field names but you can't use '.' in names (because the special char is used as the separator in field paths) and a name must not be started with a number. Also you must not use the symbols '$' and '_' as the first char in the name because they are used by the parser for special purposes. 

Commentaries
=============
You can use commentaries inside a parser script, the parser supports the only comment format and recognizes as commentaries all text after '//' till the end of line. 
```
 int;
    // hello commentaries
    byte field;
```

Expression macroses
====================
Inside expression you can use field names and field paths, also you can use the special macros '$$' which represents the current input stream byte counter, all fields started with '$' will be recognized by the parser as special user defined variables and it will be requesting them from special user defined provider. If the array size contains the only '_' symbol then the field or structure will not have defined size and whole stream will be read.

How to get result of parsing
=============================
The Result of parsing is an instance of com.igormaznitsa.jbbp.model.JBBPFieldStruct class which represents the root invisible structure for the parsed data and you can use its inside methods to find desired fields for their names, paths or classes. All Fields are successors of com.igormaznitsa.jbbp.model.JBBPAbstractField class. To increase comfort, it is easier to use mapping to classes when the mapper automaticaly places values to fields of a Java class. 

Example
========
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

My Binary data format is too complex one to be decoded by a JBBP script
========================================================================
No problems! The Parser works over com.igormaznitsa.jbbp.io.BitInputStream class which can be used directly and allows read bits, bytes, count bytes and align data from a stream.

I want to make a bin block instead of parsing!
===============================================
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
Donation   
=========
If you like the software you can make some donation to the author   
[![https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)
