Introduction
=============
Sometime it is very important to parse in Java some binary block data, may be not in very fast way but structures can have complex format and byte and bit orders can be very different. In Python we have [the Struct package](https://docs.python.org/2/library/struct.html) for operations to parse binary blocks but in Java such operations look a bit verbose and take some time to be programmed, so that I decided during my vacation to develop a framework which would decrease verbosity for such operations in Java and will decrease my work in future because I am too lazy to write a lot of code.<br>
p.s.<br>
For instance I have been very actively using the framework in [the ZX-Poly emulator](https://github.com/raydac/zxpoly) to parse snapshot files and save results.   
![Use cases](https://github.com/raydac/java-binary-block-parser/blob/master/docs/jbbp_usecases.png)
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
  
Java support
=============
  The Framework developed to support Java platform since Java SE 1.5+, its inside mapping has been developed in such manner to support work under the Android platform (as of Android 2.0) so that the framework is an Android compatible one. It doesn't use any NIO and usage of the non-standard sun.misc.unsafe class is isolated with reflection. 
  
How to use with Maven
======================
The Framework is published in the Maven Central thus it can be added as a dependency into a project
```
<dependency>
  <groupId>com.igormaznitsa</groupId>
  <artifactId>jbbp</artifactId>
  <version>1.2.0</version>
</dependency>
```
also the precompiled jar, javadoc and sources can be downloaded manually from [the Maven central.](http://search.maven.org/#browse|808871750) 

Hello world
============
The Framework is very easy in use because it has only two main classes for its functionality com.igormaznitsa.jbbp.JBBPParser (for data parsing) and com.igormaznitsa.jbbp.io.JBBPOut (for binary block writing), both of them work over low-level IO classes com.igormaznitsa.jbbp.io.JBBPBitInputStream and com.igormaznitsa.jbbp.io.JBBPBitOutputStream which are the core for the framework. 
```Java
 class Mapped { @Bin(type = BinType.BYTE_ARRAY) String text;}
    Mapped mapped = JBBPParser.prepare("byte [_]  text;").parse(JBBPOut.BeginBin().Byte("Hello World").End().toByteArray()).mapTo(Mapped.class);
    assertEquals("Hello World",mapped.text);
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
Format of a field
==================
Each field can be just a field or an array of fields, also it can be anonymous one or named one.

    [<|>]field_type [[array_size|expression|_]] [field_name] ; 

The first char shows the byte order to be used for parsing of the field (if the field is a multi-byte one) and can be omitted (and in the case the default order for the parser will be used). The Parser allows to parse a field as Big-Endinan one (for '>' prefix, it is the default state so that it can be omitted) and Little-Endian one (for '<' prefix). [You can read about endianness in wikipedia.](http://en.wikipedia.org/wiki/Endianness)

**The Field name will be normalized to its lower-case representation, so that keep in your mind that names are case insensitive.**

Supported data types and commands
==================================
- **bit[:(number_of_bits|expression_in_parentheses)]** - a bit field of fixed size (1..7 bits), by default 1
- **byte** - a signed byte field (8 bits)
- **ubyte** - a unsigned byte field (8 bits)
- **bool** - a boolean field (1 byte)
- **short** - a signed short field (2 bytes)
- **ushort**- a unsigned short field (2 bytes)
- **int** - an integer field (4 bytes)
- **long** - a long field (8 bytes)
- **align[:(number_of_bytes|expression_in_parentheses)]** - align the counter for number of bytes, by default 1. NB: It works relative to the current read byte counter!
- **skip[:(number_of_bytes|expression_in_parentheses)]** - skip number of bytes, by default 1
- **var[:(numeric_value|expression_in_parentheses)]** - a var field which should be read through an external processor defined by the user
- **reset$$** - reset the input stream read byte counter, it is very useful for relative alignment operations   

__expression_in_parentheses - means (expression)__

Structures
===========
Fields can be collected in structures, the format of structure definition: 
```
[structure_name] [[array_size|expression|_]] { fields... } 
```
Structures can contain another structures and every field inside a structure has path in the format ***structure_name.field_name*** but if you want use field names inside the same structure then use their names without name of structure. 

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
