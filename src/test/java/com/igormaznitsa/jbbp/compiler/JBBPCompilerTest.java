/* 
 * Copyright 2014 Igor Maznitsa (http://www.igormaznitsa.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.jbbp.compiler;

import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPTokenType;
import com.igormaznitsa.jbbp.exceptions.JBBPCompilationException;
import com.igormaznitsa.jbbp.exceptions.JBBPTokenizerException;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
import org.junit.Test;

public class JBBPCompilerTest {

  @Test
  public void testCompile_ErrorForWrongChar() throws Exception {
    try {
      JBBPCompiler.compile("align;9").getCompiledData();
      fail("Must throw parser exception");
    }
    catch (JBBPTokenizerException ex) {
      assertEquals(6, ex.getPosition());
      assertTrue(ex.getMessage().indexOf("[9]") >= 0);
    }
  }

  @Test
  public void testCompile_StructForWholeStreamAsSecondField() throws Exception {
    final JBBPCompiledBlock block = JBBPCompiler.compile("byte;test [_] {byte;}");
    assertEquals(5, block.getCompiledData().length);
    assertEquals(JBBPCompiler.CODE_BYTE, block.getCompiledData()[0]);
    assertEquals(JBBPCompiler.CODE_STRUCT_START | JBBPCompiler.FLAG_EXPRESSION_OR_WHOLESTREAM | JBBPCompiler.FLAG_NAMED, block.getCompiledData()[1]);
    assertEquals(JBBPCompiler.CODE_BYTE, block.getCompiledData()[2]);
    assertEquals(JBBPCompiler.CODE_STRUCT_END, block.getCompiledData()[3]);
    assertEquals(1, block.getCompiledData()[4]);
  }

  @Test
  public void testCompile_ErrorForFieldAfterWholeStreamArray() throws Exception {
    try {
      JBBPCompiler.compile("byte;test [_] {byte;} int error;");
      fail("Must throw IAE");
    }
    catch (JBBPCompilationException ex) {
      assertTrue(ex.getToken().toString().indexOf("int error") >= 0);
    }
  }

  @Test
  public void testCompile_WholeStreamArrayInsideStructure() throws Exception {
    final JBBPCompiledBlock block = JBBPCompiler.compile("test {byte [_];}");
    assertEquals(4, block.getCompiledData().length);
    assertEquals(JBBPCompiler.CODE_STRUCT_START | JBBPCompiler.FLAG_NAMED, block.getCompiledData()[0]);
    assertEquals(JBBPCompiler.CODE_BYTE | JBBPCompiler.FLAG_EXPRESSION_OR_WHOLESTREAM, block.getCompiledData()[1]);
    assertEquals(JBBPCompiler.CODE_STRUCT_END, block.getCompiledData()[2]);
    assertEquals(0, block.getCompiledData()[3]);
  }

  @Test
  public void testCompile_WholeStreamStructureArrayInsideStructure() throws Exception {
    final JBBPCompiledBlock block = JBBPCompiler.compile("test { whole[_]{ byte;}}");
    assertEquals(7, block.getCompiledData().length);
    assertEquals(JBBPCompiler.CODE_STRUCT_START | JBBPCompiler.FLAG_NAMED, block.getCompiledData()[0]);
    assertEquals(JBBPCompiler.CODE_STRUCT_START | JBBPCompiler.FLAG_NAMED | JBBPCompiler.FLAG_EXPRESSION_OR_WHOLESTREAM, block.getCompiledData()[1]);
    assertEquals(JBBPCompiler.CODE_BYTE, block.getCompiledData()[2]);
    assertEquals(JBBPCompiler.CODE_STRUCT_END, block.getCompiledData()[3]);
    assertEquals(1, block.getCompiledData()[4]);
    assertEquals(JBBPCompiler.CODE_STRUCT_END, block.getCompiledData()[5]);
    assertEquals(0, block.getCompiledData()[6]);
  }

  @Test
  public void testCompile_StructureAndArrayWithLengthFromStructureField() throws Exception {
    final JBBPCompiledBlock block = JBBPCompiler.compile("str {byte size;} byte[str.size] some;");
    assertTrue(block.hasEvaluatedSizeArrays());
    assertNotNull(block.findFieldForPath("str.size"));
    assertNotNull(block.findFieldForPath("some"));
  }

  @Test
  public void testCompile_ErrorForUnknownType() throws Exception {
    try {
      JBBPCompiler.compile("somewrong;");
      fail("Must throw IAE");
    }
    catch (JBBPCompilationException ex) {
      assertTrue(ex.getToken().toString().indexOf("somewrong") >= 0);
    }
  }

  @Test(expected = JBBPCompilationException.class)
  public void testCompile_ErrorForNamedAlignField() throws Exception {
    JBBPCompiler.compile("align hello;");
  }

  @Test(expected = JBBPCompilationException.class)
  public void testCompile_ErrorForArrayAlignField() throws Exception {
    JBBPCompiler.compile("align [445] hello;");
  }

  @Test(expected = JBBPCompilationException.class)
  public void testCompile_ErrorForZeroAlignValue() throws Exception {
    JBBPCompiler.compile("align:0;");
  }

  @Test(expected = JBBPCompilationException.class)
  public void testCompile_ErrorForNegativeAlignValue() throws Exception {
    JBBPCompiler.compile("align:-1;");
  }

  @Test(expected = JBBPTokenizerException.class)
  public void testCompile_ErrorForNonNumericAlignValue() throws Exception {
    JBBPCompiler.compile("align:hhh;");
  }

  @Test(expected = JBBPCompilationException.class)
  public void testCompile_ErrorForNamedSkipField() throws Exception {
    JBBPCompiler.compile("skip hello;");
  }

  @Test(expected = JBBPCompilationException.class)
  public void testCompile_ErrorForArraySkipField() throws Exception {
    JBBPCompiler.compile("skip [445] hello;");
  }

  @Test
  public void testCompile_ZeroSkipValueIsAllowed() throws Exception {
    assertArrayEquals(new byte[]{JBBPCompiler.CODE_SKIP, 0},JBBPCompiler.compile("skip:0;").getCompiledData());
  }

  @Test(expected = JBBPCompilationException.class)
  public void testCompile_ErrorForNegativeSkipValue() throws Exception {
    JBBPCompiler.compile("skip:-1;");
  }

  @Test(expected = JBBPTokenizerException.class)
  public void testCompile_ErrorForNonNumericSkipValue() throws Exception {
    JBBPCompiler.compile("skip:hhh;");
  }

  @Test
  public void testCompile_ErrorForUnknownTypeAsSecondField() throws Exception {
    try {
      JBBPCompiler.compile("byte; somewrong; int;");
      fail("Must throw IAE");
    }
    catch (JBBPCompilationException ex) {
      assertTrue(ex.getToken().toString().indexOf("somewrong") >= 0);
    }
  }

  @Test
  public void testCompile_ErrorForNonOpenedStructure() throws Exception {
    try {
      JBBPCompiler.compile("byte; int; } ");
      fail("Must throw IAE");
    }
    catch (JBBPCompilationException ex) {
      assertEquals(JBBPTokenType.STRUCT_END, ex.getToken().getType());
    }
  }

  @Test
  public void testCompile_ErrorForNonClosedStructure() throws Exception {
    try {
      JBBPCompiler.compile("{byte; int; ");
      fail("Must throw IAE");
    }
    catch (JBBPCompilationException ex) {
      assertNull(ex.getToken());
    }
  }

  @Test
  public void testCompile_ErrorForIllegaFieldName() throws Exception {
    try {
      JBBPCompiler.compile("  byte int;");
      fail("Must throw Tokenizer Exception");
    }
    catch (JBBPTokenizerException ex) {
      assertEquals(2, ex.getPosition());
    }
  }

  @Test
  public void testCompile_ErrorForIllegalCharInFieldName() throws Exception {
    try {
      JBBPCompiler.compile("int;  byte in.td;");
      fail("Must throw Tokenizer Exception");
    }
    catch (JBBPTokenizerException ex) {
      assertEquals(6, ex.getPosition());
    }
  }

  @Test
  public void testCompile_ErrorForIllegalStartCharInFieldName() throws Exception {
    try {
      JBBPCompiler.compile("int;  byte 5intd;");
      fail("Must throw Tokenizer Exception");
    }
    catch (JBBPTokenizerException ex) {
      assertEquals(4, ex.getPosition());
    }
  }

  @Test
  public void testCompile_ErrorForIllegalCharInOnlyFieldName() throws Exception {
    try {
      JBBPCompiler.compile("  byte in.td;");
      fail("Must throw Tokenizer Exception");
    }
    catch (JBBPTokenizerException ex) {
      assertEquals(2, ex.getPosition());
    }
  }

  @Test
  public void testCompile_NonamedVarWithoutExtra() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("var;").getCompiledData();
    assertArrayEquals(new byte[]{JBBPCompiler.CODE_VAR,0}, compiled);
  }

  @Test
  public void testCompile_NonamedVarWithPositiveExtra() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("var:12;").getCompiledData();
    assertArrayEquals(new byte[]{JBBPCompiler.CODE_VAR,12}, compiled);
  }

  @Test
  public void testCompile_NonamedVarWithNegativeExtra() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("var:-1;").getCompiledData();
    assertArrayEquals(new byte[]{JBBPCompiler.CODE_VAR,(byte)0x81,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF}, compiled);
  }

  @Test
  public void testCompile_NamedVarWithoutExtra() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("var VVV;").getCompiledData();
    assertArrayEquals(new byte[]{JBBPCompiler.CODE_VAR | JBBPCompiler.FLAG_NAMED,0}, compiled);
  }

  @Test
  public void testCompile_NamedVarWithPositiveExtra() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("var:12 VVV;").getCompiledData();
    assertArrayEquals(new byte[]{JBBPCompiler.CODE_VAR | JBBPCompiler.FLAG_NAMED,12}, compiled);
  }

  @Test
  public void testCompile_NamedVarWithNegativeExtra() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("var:-1 VVV;").getCompiledData();
    assertArrayEquals(new byte[]{JBBPCompiler.CODE_VAR | JBBPCompiler.FLAG_NAMED,(byte)0x81,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF}, compiled);
  }

  @Test
  public void testCompile_NamedVarArrayWithoutExtra() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("var [98] VVV;").getCompiledData();
    assertArrayEquals(new byte[]{JBBPCompiler.CODE_VAR | JBBPCompiler.FLAG_NAMED | JBBPCompiler.FLAG_ARRAY,98,0}, compiled);
  }

  @Test
  public void testCompile_NamedVarArrayWithPositiveExtra() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("var:12 [98] VVV;").getCompiledData();
    assertArrayEquals(new byte[]{JBBPCompiler.CODE_VAR | JBBPCompiler.FLAG_ARRAY |JBBPCompiler.FLAG_NAMED, 98, 12}, compiled);
  }

  @Test
  public void testCompile_NamedVarArrayWithNegativeExtra() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("var:-1 [98] VVV;").getCompiledData();
    assertArrayEquals(new byte[]{JBBPCompiler.CODE_VAR | JBBPCompiler.FLAG_ARRAY|JBBPCompiler.FLAG_NAMED,98,(byte)0x81,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF}, compiled);
  }

  @Test
  public void testCompile_SingleAlignFieldWithoutExtra() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("align;").getCompiledData();
    assertEquals(2, compiled.length);
    assertEquals(JBBPCompiler.CODE_ALIGN, compiled[0]);
    assertEquals(1, compiled[1]);
  }

  @Test
  public void testCompile_SingleAlignFieldWithValue() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("align:9;").getCompiledData();
    assertEquals(2, compiled.length);
    assertEquals(JBBPCompiler.CODE_ALIGN, compiled[0]);
    assertEquals(9, compiled[1]);
  }

  @Test
  public void testCompile_SingleNonamedBitDefaultLenField() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("bit;").getCompiledData();
    assertEquals(2, compiled.length);
    assertEquals(JBBPCompiler.CODE_BIT, compiled[0]);
    assertEquals(1, compiled[1]);
  }

  @Test
  public void testCompile_SingleNonamedBitDefinedLenField() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("bit:5;").getCompiledData();
    assertEquals(2, compiled.length);
    assertEquals(JBBPCompiler.CODE_BIT, compiled[0]);
    assertEquals(5, compiled[1]);
  }

  @Test
  public void testCompile_SingleNonamedBooleanField() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("bool;").getCompiledData();
    assertEquals(1, compiled.length);
    assertEquals(JBBPCompiler.CODE_BOOL, compiled[0]);
  }

  @Test
  public void testCompile_SingleNonamedByteField() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("byte;").getCompiledData();
    assertEquals(1, compiled.length);
    assertEquals(JBBPCompiler.CODE_BYTE, compiled[0]);
  }

  @Test
  public void testCompile_SingleNonamedUByteField() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("ubyte;").getCompiledData();
    assertEquals(1, compiled.length);
    assertEquals(JBBPCompiler.CODE_UBYTE, compiled[0]);
  }

  @Test
  public void testCompile_SingleNonamedShortField() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("short;").getCompiledData();
    assertEquals(1, compiled.length);
    assertEquals(JBBPCompiler.CODE_SHORT, compiled[0]);
  }

  @Test
  public void testCompile_SingleNonamedUShortField() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("ushort;").getCompiledData();
    assertEquals(1, compiled.length);
    assertEquals(JBBPCompiler.CODE_USHORT, compiled[0]);
  }

  @Test
  public void testCompile_SingleNonamedIntField() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("int;").getCompiledData();
    assertEquals(1, compiled.length);
    assertEquals(JBBPCompiler.CODE_INT, compiled[0]);
  }

  @Test
  public void testCompile_SingleNonamedLongField() throws Exception {
    final byte[] compiled = JBBPCompiler.compile("long;").getCompiledData();
    assertEquals(1, compiled.length);
    assertEquals(JBBPCompiler.CODE_LONG, compiled[0]);
  }

  @Test
  public void testCompile_SingleNamedIntField() throws Exception {
    final JBBPCompiledBlock block = JBBPCompiler.compile("int HeLLo;");
    final byte[] compiled = block.getCompiledData();
    assertEquals(1, compiled.length);
    assertEquals(JBBPCompiler.CODE_INT | JBBPCompiler.FLAG_NAMED, compiled[0]);
    assertEquals(0, block.findFieldOffsetForPath("hello"));
  }

  @Test
  public void testCompile_SingleNamedIntBigEndianField() throws Exception {
    final JBBPCompiledBlock block = JBBPCompiler.compile(">int HeLLo;");
    final byte[] compiled = block.getCompiledData();
    assertEquals(1, compiled.length);
    assertEquals(JBBPCompiler.CODE_INT | JBBPCompiler.FLAG_NAMED, compiled[0]);
    assertEquals(0, block.findFieldOffsetForPath("hello"));
  }

  @Test
  public void testCompile_SingleNamedIntLittleEndianField() throws Exception {
    final JBBPCompiledBlock block = JBBPCompiler.compile("<int HeLLo;");
    final byte[] compiled = block.getCompiledData();
    assertEquals(1, compiled.length);
    assertEquals(JBBPCompiler.CODE_INT | JBBPCompiler.FLAG_NAMED | JBBPCompiler.FLAG_LITTLE_ENDIAN, compiled[0] & 0xFF);
    assertEquals(0, block.findFieldOffsetForPath("hello"));
  }

  @Test
  public void testCompile_CommentAsTheFirstAndThirthLines() throws Exception {
    final JBBPCompiledBlock block = JBBPCompiler.compile("// first line\nbyte;//second line");
    assertEquals(1, block.getCompiledData().length);
    assertEquals(JBBPCompiler.CODE_BYTE, block.getCompiledData()[0]);
  }

  @Test
  public void testCompile_ArrayNamedIntLittleEndianField() throws Exception {
    final JBBPCompiledBlock block = JBBPCompiler.compile("<int [768] HeLLo;");
    final byte[] compiled = block.getCompiledData();
    assertEquals(4, compiled.length);
    assertEquals(JBBPCompiler.CODE_INT | JBBPCompiler.FLAG_NAMED | JBBPCompiler.FLAG_LITTLE_ENDIAN | JBBPCompiler.FLAG_ARRAY, compiled[0] & 0xFF);
    assertEquals(0, block.findFieldOffsetForPath("hello"));
    assertEquals(768, JBBPUtils.unpackInt(compiled, new AtomicInteger(1)));
  }

  @Test
  public void testCompile_ByteFieldAndNamedStructureWithSingleNamedIntField() throws Exception {
    final JBBPCompiledBlock block = JBBPCompiler.compile("byte ; some {int HeLLo;} ");
    final byte[] compiled = block.getCompiledData();
    assertEquals(5, compiled.length);
    assertEquals(JBBPCompiler.CODE_BYTE, compiled[0] & 0xFF);
    assertEquals(JBBPCompiler.CODE_STRUCT_START | JBBPCompiler.FLAG_NAMED, compiled[1]);
    assertEquals(JBBPCompiler.CODE_INT | JBBPCompiler.FLAG_NAMED, compiled[2] & 0xFF);
    assertEquals(JBBPCompiler.CODE_STRUCT_END, compiled[3]);
    assertEquals(1, compiled[4]);
    assertEquals(2, block.findFieldOffsetForPath("some.hello"));
  }

  @Test
  public void testCompile_ByteFieldAndNamedStructureWithIncludedStructureWithSingleNamedIntField() throws Exception {
    final JBBPCompiledBlock block = JBBPCompiler.compile("byte ; out{ some {int HeLLo;} }");
    final byte[] compiled = block.getCompiledData();
    assertEquals(8, compiled.length);
    assertEquals(JBBPCompiler.CODE_BYTE, compiled[0] & 0xFF);
    assertEquals(JBBPCompiler.CODE_STRUCT_START | JBBPCompiler.FLAG_NAMED, compiled[1]);
    assertEquals(JBBPCompiler.CODE_STRUCT_START | JBBPCompiler.FLAG_NAMED, compiled[2]);
    assertEquals(JBBPCompiler.CODE_INT | JBBPCompiler.FLAG_NAMED, compiled[3] & 0xFF);
    assertEquals(JBBPCompiler.CODE_STRUCT_END, compiled[4]);
    assertEquals(2, compiled[5]);
    assertEquals(JBBPCompiler.CODE_STRUCT_END, compiled[6]);
    assertEquals(1, compiled[7]);
    assertEquals(3, block.findFieldOffsetForPath("out.some.hello"));
  }

  @Test
  public void testCompile_FixedLengthByteArrayInStructure() throws Exception {
    final JBBPCompiledBlock block = JBBPCompiler.compile("out { some {byte [3] HeLLo;} }");
    final byte[] compiled = block.getCompiledData();
    assertEquals(8, compiled.length);
    assertEquals(JBBPCompiler.CODE_STRUCT_START | JBBPCompiler.FLAG_NAMED, compiled[0]);
    assertEquals(JBBPCompiler.CODE_STRUCT_START | JBBPCompiler.FLAG_NAMED, compiled[1]);
    assertEquals(JBBPCompiler.CODE_BYTE | JBBPCompiler.FLAG_NAMED | JBBPCompiler.FLAG_ARRAY, compiled[2] & 0xFF);
    assertEquals(3, compiled[3]);
    assertEquals(JBBPCompiler.CODE_STRUCT_END, compiled[4]);
    assertEquals(1, compiled[5]);
    assertEquals(JBBPCompiler.CODE_STRUCT_END, compiled[6]);
    assertEquals(0, compiled[7]);
    assertEquals(3, block.getNamedFields().length);
    assertEquals("out", block.getNamedFields()[0].getFieldPath());
    assertEquals("out.some", block.getNamedFields()[1].getFieldPath());
    assertEquals("out.some.hello", block.getNamedFields()[2].getFieldPath());
  }

  @Test
  public void testCompile_VarLengthByteArrayInStructure() throws Exception {
    final JBBPCompiledBlock block = JBBPCompiler.compile("out { int len; some {byte [len] HeLLo;} }");
    final byte[] compiled = block.getCompiledData();
    assertEquals(8, compiled.length);
    assertEquals(JBBPCompiler.CODE_STRUCT_START | JBBPCompiler.FLAG_NAMED, compiled[0]);
    assertEquals(JBBPCompiler.CODE_INT | JBBPCompiler.FLAG_NAMED, compiled[1] & 0xFF);
    assertEquals(JBBPCompiler.CODE_STRUCT_START | JBBPCompiler.FLAG_NAMED, compiled[2]);
    assertEquals(JBBPCompiler.CODE_BYTE | JBBPCompiler.FLAG_NAMED | JBBPCompiler.FLAG_ARRAY | JBBPCompiler.FLAG_EXPRESSION_OR_WHOLESTREAM, compiled[3] & 0xFF);
    assertEquals(JBBPCompiler.CODE_STRUCT_END, compiled[4]);
    assertEquals(2, compiled[5]);
    assertEquals(JBBPCompiler.CODE_STRUCT_END, compiled[6]);
    assertEquals(0, compiled[7]);
    assertEquals(4, block.getNamedFields().length);
    assertEquals("out", block.getNamedFields()[0].getFieldPath());
    assertEquals("out.len", block.getNamedFields()[1].getFieldPath());
    assertEquals("out.some", block.getNamedFields()[2].getFieldPath());
    assertEquals("out.some.hello", block.getNamedFields()[3].getFieldPath());
  }

  @Test(expected = JBBPCompilationException.class)
  public void testCompile_ErrorForArrayAsVarLength() throws Exception {
    JBBPCompiler.compile("out { int [4] len; some {byte [len] HeLLo;} }");
  }

  @Test(expected = JBBPCompilationException.class)
  public void testCompile_ErrorForFieldInStructArrayAsVarLength() throws Exception {
    JBBPCompiler.compile("struct [10] {int [4] len;} some {byte [struct.len] HeLLo;} ");
  }

  @Test(expected = JBBPCompilationException.class)
  public void testCompile_ErrorForFieldInStructAsVarLength() throws Exception {
    JBBPCompiler.compile("struct [10] {int len;} some {byte [struct.len] HeLLo;} ");
  }

  @Test(expected = JBBPCompilationException.class)
  public void testCompile_ErrorForUnknownFieldAsArrayLength() throws Exception {
    JBBPCompiler.compile("some {byte [struct.len] HeLLo;} ");
  }

  @Test
  public void testCompile_ArrayWithUndefinedLength() throws Exception {
    final JBBPCompiledBlock compiled = JBBPCompiler.compile("byte [_] HeLLo;");
    final JBBPNamedFieldInfo field = compiled.findFieldForPath("hello");
    assertEquals(1, compiled.getCompiledData().length);
    assertNotNull(field);
    assertEquals(0, field.getFieldOffsetInCompiledBlock());
    assertEquals(JBBPCompiler.CODE_BYTE | JBBPCompiler.FLAG_NAMED | JBBPCompiler.FLAG_EXPRESSION_OR_WHOLESTREAM, compiled.getCompiledData()[0]);
  }

  @Test
  public void testCompile_StructFieldWithNameOfExternalField() throws Exception {
    final JBBPCompiledBlock compiled = JBBPCompiler.compile("int a; ins{ int a;}");
    final byte [] data = compiled.getCompiledData();
    assertEquals(5, data.length);
  }

}
