/*
 * Copyright 2017 Igor Maznitsa.
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

package com.igormaznitsa.jbbp.compiler.tokenizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.exceptions.JBBPTokenizerException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

public class JBBPTokenizerTest {

  @Test
  public void testNotAllowesNameError() {
    assertThrows(JBBPTokenizerException.class, () -> new JBBPTokenizer("int $$;").next());
    assertThrows(JBBPTokenizerException.class, () -> new JBBPTokenizer("int $a;").next());
    assertThrows(JBBPTokenizerException.class, () -> new JBBPTokenizer("int a%d;").next());
    assertThrows(JBBPTokenizerException.class, () -> new JBBPTokenizer("int 1a;").next());
    assertThrows(JBBPTokenizerException.class, () -> new JBBPTokenizer("int _;").next());
  }

  @Test
  public void testError_ForEmptyString() {
    final JBBPTokenizer parser = new JBBPTokenizer("");
    assertTrue(parser.hasNext());
    try {
      parser.next();
      fail("Must throw Tokenizer exception");
    } catch (JBBPTokenizerException ex) {
      assertEquals(0, ex.getPosition());
    }
  }

  @Test
  public void testError_ForArrayFieldWithoutType() {
    final JBBPTokenizer parser = new JBBPTokenizer(" [123] hello;");
    assertTrue(parser.hasNext());
    try {
      parser.next();
      fail("Must throw Tokenizer exception");
    } catch (JBBPTokenizerException ex) {
      assertEquals(1, ex.getPosition());
      assertEquals("->[<- 123] hello;", ex.getErrorPart());
    }
  }

  @Test
  public void testStructureEndAndErrorForArrayFieldWithoutType() {
    final JBBPTokenizer parser = new JBBPTokenizer("} [123] hello;");
    assertTrue(parser.hasNext());

    assertEquals(JBBPTokenType.STRUCT_END, parser.next().getType());

    try {
      parser.next();
      fail("Must throw Tokenizer exception");
    } catch (JBBPTokenizerException ex) {
      assertEquals(2, ex.getPosition());
      assertEquals("}  ->[<- 123] hello;", ex.getErrorPart());
    }
  }


  @Test
  public void testParseOnlyCommentLine_WithNextLine() {
    final JBBPTokenizer parser = new JBBPTokenizer("   // only comment line   \n");
    int commentCount = 0;
    int otherCount = 0;
    for (final JBBPToken item : parser) {
      if (item.getType() == JBBPTokenType.COMMENT) {
        assertEquals(3, item.getPosition());
        assertEquals("only comment line", item.getFieldName());
        assertNull(item.getArraySizeAsString());
        assertFalse(item.isArray());
        assertTrue(item.isComment());
        commentCount++;
      } else {
        otherCount++;
      }
    }

    assertEquals(0, otherCount);
    assertEquals(1, commentCount);
  }

  @Test
  public void testParseOnlyCommentLine_WithoutNextLine() {
    final JBBPTokenizer parser = new JBBPTokenizer("   // only comment line   ");
    int commentCount = 0;
    int otherCount = 0;
    for (final JBBPToken item : parser) {
      if (item.getType() == JBBPTokenType.COMMENT) {
        assertEquals(3, item.getPosition());
        assertEquals("only comment line", item.getFieldName());
        assertNull(item.getArraySizeAsString());
        assertFalse(item.isArray());
        assertTrue(item.isComment());
        commentCount++;
      } else {
        otherCount++;
      }
    }

    assertEquals(0, otherCount);
    assertEquals(1, commentCount);
  }

  @Test
  public void testSingleLine_ErrorForUnsupportedText() {
    final JBBPTokenizer parser = new JBBPTokenizer("   some unsupported line  ");
    try {
      parser.iterator().next();
      fail("Must throw parser exception");
    } catch (JBBPTokenizerException ex) {
      assertEquals(0, ex.getPosition());
      assertEquals("-> <-   some", ex.getErrorPart());
    }
  }

  @Test
  public void testStructure_EndWithSpaces() {
    final JBBPTokenizer parser = new JBBPTokenizer("   } ");

    final Iterator<JBBPToken> iterator = parser.iterator();

    final JBBPToken item = iterator.next();

    assertEquals(JBBPTokenType.STRUCT_END, item.getType());
    assertNull(item.getFieldName());
    assertNull(item.getFieldTypeParameters());
    assertNull(item.getArraySizeAsString());
    assertEquals(3, item.getPosition());

    try {
      iterator.next();
      fail("Must throw NSEE");
    } catch (NoSuchElementException ex) {

    }
  }

  @Test
  public void testStructure_End_WithoutSpaces() {
    final JBBPTokenizer parser = new JBBPTokenizer("}");

    final Iterator<JBBPToken> iterator = parser.iterator();

    final JBBPToken item = iterator.next();

    assertEquals(JBBPTokenType.STRUCT_END, item.getType());
    assertNull(item.getFieldName());
    assertNull(item.getFieldTypeParameters());
    assertNull(item.getArraySizeAsString());
    assertEquals(0, item.getPosition());

    try {
      iterator.next();
      fail("Must throw NSEE");
    } catch (NoSuchElementException ex) {

    }
  }

  @Test
  public void testStructure_End_WithoutSpacesAndWithNextField() {
    final JBBPTokenizer parser = new JBBPTokenizer("}byte;");

    final Iterator<JBBPToken> iterator = parser.iterator();

    final JBBPToken item = iterator.next();

    assertEquals(JBBPTokenType.STRUCT_END, item.getType());
    assertNull(item.getFieldName());
    assertNull(item.getFieldTypeParameters());
    assertNull(item.getArraySizeAsString());
    assertEquals(0, item.getPosition());
  }

  @Test
  public void testStructure_Named_Start_WithSpaces() {
    final JBBPTokenizer parser = new JBBPTokenizer("    struct {  ");

    final Iterator<JBBPToken> iterator = parser.iterator();

    final JBBPToken item = iterator.next();

    assertEquals(JBBPTokenType.STRUCT_START, item.getType());
    assertEquals("struct", item.getFieldName());
    assertNull(item.getFieldTypeParameters());
    assertNull(item.getArraySizeAsString());
    assertEquals(4, item.getPosition());

    try {
      iterator.next();
      fail("Must throw NSEE");
    } catch (NoSuchElementException ex) {

    }
  }

  @Test
  public void testError_ForWrongByteOrderFormatChar() {
    final JBBPTokenizer parser = new JBBPTokenizer("   !int hello; ");

    final Iterator<JBBPToken> iterator = parser.iterator();
    try {
      iterator.next();
      fail("Must throw parser exception for wrong symbol of byte order");
    } catch (JBBPTokenizerException ex) {
      assertEquals(3, ex.getPosition());
      assertEquals("->!<- int hello;", ex.getErrorPart());
    }
  }

  @Test
  public void testErrorForIteratorRemove() {
    assertThrows(UnsupportedOperationException.class, () -> new JBBPTokenizer("int a;").remove());
  }

  @Test
  public void testStructure_Named_Start_ErrorForDisabledCharAtName() {
    final JBBPTokenizer parser = new JBBPTokenizer("   struct.a { ");

    final Iterator<JBBPToken> iterator = parser.iterator();
    try {
      iterator.next();
      fail("Must throw parser exception for disabled dot char at name");
    } catch (JBBPTokenizerException ex) {
      assertEquals(3, ex.getPosition());
      assertEquals("->s<- truct.a {", ex.getErrorPart());
    }
  }

  @Test
  public void testRegularField_ErrorForDisabledCharAtName() {
    final JBBPTokenizer parser = new JBBPTokenizer("   int.a; ");

    final Iterator<JBBPToken> iterator = parser.iterator();
    try {
      iterator.next();
      fail("Must throw parser exception for disabled dot char at name");
    } catch (JBBPTokenizerException ex) {
      assertEquals(3, ex.getPosition());
      assertEquals("->i<- nt.a;", ex.getErrorPart());
    }
  }

  @Test
  public void testStructure_Named_Start_ErrorForDollarAsTheFirstChar() {
    final JBBPTokenizer parser = new JBBPTokenizer("   $struct { ");

    final Iterator<JBBPToken> iterator = parser.iterator();
    try {
      iterator.next();
      fail("Must throw parser exception for disabled dot char at name");
    } catch (JBBPTokenizerException ex) {
      assertEquals(3, ex.getPosition());
      assertEquals("->$<- struct {", ex.getErrorPart());
    }
  }

  @Test
  public void testRegularField_Name_ErrorForDollarAsTheFirstChar() {
    final JBBPTokenizer parser = new JBBPTokenizer("   $int; ");

    final Iterator<JBBPToken> iterator = parser.iterator();
    try {
      iterator.next();
      fail("Must throw parser exception for disabled dot char at name");
    } catch (JBBPTokenizerException ex) {
      assertEquals(3, ex.getPosition());
      assertEquals("->$<- int;", ex.getErrorPart());
    }
  }

  @Test
  public void testStructure_Named_WithoutSpacesToBracket() {
    final JBBPTokenizer parser = new JBBPTokenizer("  struct{ ");

    final Iterator<JBBPToken> iterator = parser.iterator();

    final JBBPToken item = iterator.next();

    assertEquals(JBBPTokenType.STRUCT_START, item.getType());
    assertEquals("struct", item.getFieldName());
    assertNull(item.getFieldTypeParameters());
    assertNull(item.getArraySizeAsString());
    assertEquals(2, item.getPosition());

    try {
      iterator.next();
      fail("Must throw NSEE");
    } catch (NoSuchElementException ex) {

    }
  }

  @Test
  public void testStructure_NonamedArray_Spaces() {
    final JBBPTokenizer parser = new JBBPTokenizer("    [ 333 ]  { ");

    final Iterator<JBBPToken> iterator = parser.iterator();

    final JBBPToken item = iterator.next();

    assertEquals(JBBPTokenType.STRUCT_START, item.getType());
    assertNull(item.getFieldName());
    assertNull(item.getFieldTypeParameters());
    assertTrue(item.isArray());
    assertEquals(333, item.getArraySizeAsInt().intValue());
    assertEquals(4, item.getPosition());

    try {
      iterator.next();
      fail("Must throw NSEE");
    } catch (NoSuchElementException ex) {

    }
  }

  @Test
  public void testStructure_NonamedArray_WithoutSpacesInSizeAndBeforeBracket() {
    final JBBPTokenizer parser = new JBBPTokenizer("    [333]{ ");

    final Iterator<JBBPToken> iterator = parser.iterator();

    final JBBPToken item = iterator.next();

    assertEquals(JBBPTokenType.STRUCT_START, item.getType());
    assertNull(item.getFieldName());
    assertNull(item.getFieldTypeParameters());
    assertTrue(item.isArray());
    assertEquals(333, item.getArraySizeAsInt().intValue());
    assertEquals(4, item.getPosition());

    try {
      iterator.next();
      fail("Must throw NSEE");
    } catch (NoSuchElementException ex) {

    }
  }

  @Test
  public void testStructure_Array_ErrorForWrongNameSizeOrder() {
    final JBBPTokenizer parser = new JBBPTokenizer("  [333] test { ");

    final Iterator<JBBPToken> iterator = parser.iterator();

    try {
      iterator.next();
      fail("Must throw parser exception");
    } catch (JBBPTokenizerException ex) {
      assertEquals(2, ex.getPosition());
      assertEquals("->[<- 333] test", ex.getErrorPart());
    }
  }

  @Test
  public void testStructure_Array_SpacesBetweenNameSizeBracket() {
    final JBBPTokenizer parser = new JBBPTokenizer("  test [333] { ");

    final Iterator<JBBPToken> iterator = parser.iterator();

    final JBBPToken item = iterator.next();

    assertEquals(JBBPTokenType.STRUCT_START, item.getType());
    assertEquals("test", item.getFieldName());
    assertNull(item.getFieldTypeParameters());
    assertTrue(item.isArray());
    assertEquals(333, item.getArraySizeAsInt().intValue());
    assertEquals(2, item.getPosition());

    try {
      iterator.next();
      fail("Must throw NSEE");
    } catch (NoSuchElementException ex) {

    }
  }

  @Test
  public void testStructure_Array_ErrorForTypeDefinition() {
    final JBBPTokenizer parser = new JBBPTokenizer("    int [] struct{ ");

    final Iterator<JBBPToken> iterator = parser.iterator();

    try {
      iterator.next();
      fail("Must throw parser exception for wrong struct end definition");
    } catch (JBBPTokenizerException ex) {
      assertEquals(0, ex.getPosition());
      assertEquals("-> <-    int", ex.getErrorPart());
    }
  }

  @Test
  public void testStructure_ErrorForTypeDefinition() {
    final JBBPTokenizer parser = new JBBPTokenizer("    int struct{ ");

    final Iterator<JBBPToken> iterator = parser.iterator();

    try {
      iterator.next();
      fail("Must throw parser exception for wrong struct end definition");
    } catch (JBBPTokenizerException ex) {
      assertEquals(4, ex.getPosition());
      assertEquals("->i<- nt struct{", ex.getErrorPart());
    }
  }

  @Test
  public void testErrorForNonRecognizedStringInMiddleOfText() {
    final JBBPTokenizer parser = new JBBPTokenizer("// test \n wrong line \n next wrong int ;");

    final Iterator<JBBPToken> iterator = parser.iterator();

    JBBPToken comment = iterator.next();
    assertTrue(comment.isComment());

    try {
      iterator.next();
      fail("Must throw exception");
    } catch (JBBPTokenizerException ex) {
      assertEquals(8, ex.getPosition());
      assertEquals("test  ->\\u000a<-  wrong", ex.getErrorPart());
    }
  }

  @Test
  public void testParseScript_WithoutStructures() {
    final JBBPTokenizer parser = new JBBPTokenizer(
        "\n// test without structures\n"
            + " boolean; // nonamed boolean field\n"
            + " int [123] items;"
            + " bit:3 bitField; // a bit field");

    final Iterator<JBBPToken> iterator = parser.iterator();

    JBBPToken item = iterator.next();
    assertTrue(item.isComment());
    assertEquals("test without structures", item.getFieldName());
    assertEquals(1, item.getPosition());

    item = iterator.next(); // boolean;
    assertFalse(item.isComment());
    assertFalse(item.isArray());
    assertEquals(JBBPTokenType.ATOM, item.getType());
    assertEquals("boolean", item.getFieldTypeParameters().getTypeName());
    assertNull(item.getFieldName());
    assertEquals(29, item.getPosition());

    item = iterator.next(); // comment
    assertTrue(item.isComment());
    assertEquals("nonamed boolean field", item.getFieldName());
    assertEquals(38, item.getPosition());

    item = iterator.next(); // int [123] items;
    assertFalse(item.isComment());
    assertTrue(item.isArray());
    assertEquals(JBBPTokenType.ATOM, item.getType());
    assertEquals("int", item.getFieldTypeParameters().getTypeName());
    assertEquals(123, item.getArraySizeAsInt().intValue());
    assertEquals("items", item.getFieldName());
    assertEquals(64, item.getPosition());

    item = iterator.next(); // bit:3 bitField;
    assertFalse(item.isComment());
    assertFalse(item.isArray());
    assertEquals(JBBPTokenType.ATOM, item.getType());
    assertEquals("bit:3", item.getFieldTypeParameters().toString());
    assertEquals("bitField", item.getFieldName());
    assertEquals(81, item.getPosition());

    item = iterator.next(); // comment
    assertTrue(item.isComment());
    assertEquals("a bit field", item.getFieldName());
    assertEquals(97, item.getPosition());

    assertFalse(iterator.hasNext());
    try {
      iterator.next();
      fail("Must throw NSEE");
    } catch (NoSuchElementException ex) {
    }
  }

  private void assertParsedItem(final JBBPToken item, final JBBPTokenType itemType,
                                final String fieldType, final String length,
                                final String fieldName) {
    assertNotNull(item);
    assertEquals(itemType, item.getType());
    if (fieldType == null) {
      assertNull(item.getFieldTypeParameters());
    } else {
      assertEquals(fieldType, item.getFieldTypeParameters().toString());
    }
    assertEquals(fieldName, item.getFieldName());
    assertEquals(length, item.getArraySizeAsString());
  }

  @Test
  public void testParse_Field_Nonamed_BitBit() {
    final JBBPTokenizer parser = new JBBPTokenizer("  bit:4; bit:5;  ");
    final Iterator<JBBPToken> iterator = parser.iterator();
    assertParsedItem(iterator.next(), JBBPTokenType.ATOM, "bit:4", null, null);
    assertParsedItem(iterator.next(), JBBPTokenType.ATOM, "bit:5", null, null);
    assertFalse(iterator.hasNext());
  }

  @Test
  public void testParse_Field_Nonamed_Align() {
    final JBBPTokenizer parser = new JBBPTokenizer("  align:8;  ");
    final Iterator<JBBPToken> iterator = parser.iterator();
    assertParsedItem(iterator.next(), JBBPTokenType.ATOM, "align:8", null, null);
    assertFalse(iterator.hasNext());
  }

  @Test
  public void testParse_StructureArrayStartWithComplexExpressionAsSize() {
    final JBBPTokenizer parser =
        new JBBPTokenizer("ColorMap [ (Header.ColorMapType & 1) * Header.CMapLength] {");
    final Iterator<JBBPToken> iterator = parser.iterator();
    assertParsedItem(iterator.next(), JBBPTokenType.STRUCT_START, null,
        "(Header.ColorMapType & 1) * Header.CMapLength", "ColorMap");
    assertFalse(iterator.hasNext());
  }

  @Test
  public void testParse_wrongEndOfStruct() {
    try {
      JBBPParser.prepare(
          "byte[5] blob5; "
              + "accountFlags {"
              + " byte[32] address; "
              + "}; "
              + "byte[7] blob7;"
      );
    } catch (JBBPTokenizerException ex) {
      assertEquals(49, ex.getPosition());
      assertEquals("address; } ->;<-  byte[7]", ex.getErrorPart());
    }
  }

  @Test
  public void testParse_VarFieldArrayWithNegativeExtra() {
    final JBBPTokenizer parser = new JBBPTokenizer("var:-123 [size] name;");
    final Iterator<JBBPToken> iterator = parser.iterator();
    assertParsedItem(iterator.next(), JBBPTokenType.ATOM, "var:-123", "size", "name");
    assertFalse(iterator.hasNext());
  }

  @Test
  public void testParseScript_WithStructure() {
    final JBBPTokenizer parser = new JBBPTokenizer(
        "  // test\n"
            + "  int; \n"
            + "  boolean a;\n"
            + "  byte [1024] array ; \n"
            + "  header {\n"
            + "	    long id;\n"
            + "   }\n"
            + "   byte [header.long] data;");

    final Iterator<JBBPToken> iterator = parser.iterator();
    assertParsedItem(iterator.next(), JBBPTokenType.COMMENT, null, null, "test");
    assertParsedItem(iterator.next(), JBBPTokenType.ATOM, "int", null, null);
    assertParsedItem(iterator.next(), JBBPTokenType.ATOM, "boolean", null, "a");
    assertParsedItem(iterator.next(), JBBPTokenType.ATOM, "byte", "1024", "array");
    assertParsedItem(iterator.next(), JBBPTokenType.STRUCT_START, null, null, "header");
    assertParsedItem(iterator.next(), JBBPTokenType.ATOM, "long", null, "id");
    assertParsedItem(iterator.next(), JBBPTokenType.STRUCT_END, null, null, null);
    assertParsedItem(iterator.next(), JBBPTokenType.ATOM, "byte", "header.long", "data");

    assertFalse(iterator.hasNext());

    try {
      iterator.next();
      fail("Must throw exception");
    } catch (NoSuchElementException ex) {

    }
  }

}
