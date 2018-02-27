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

package com.igormaznitsa.jbbp.model;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.exceptions.JBBPFinderException;
import com.igormaznitsa.jbbp.exceptions.JBBPTooManyFieldsFoundException;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JBBPFieldStructTest {

  @Test
  public void testConstructor_Fields() {
    final JBBPFieldStruct struct = new JBBPFieldStruct(null, new JBBPAbstractField[] {new JBBPFieldByte(null, (byte) 123), new JBBPFieldByte(null, (byte) -123)});
    assertNull(struct.getNameInfo());
    assertEquals(2, struct.getArray().length);
  }

  @Test
  public void testConstructor_Name_Fields() {
    final JBBPFieldStruct struct = new JBBPFieldStruct(new JBBPNamedFieldInfo("test.struct", "struct", 999), new JBBPAbstractField[] {new JBBPFieldByte(null, (byte) 123), new JBBPFieldByte(null, (byte) -123)});
    assertNotNull(struct.getNameInfo());
    assertEquals("test.struct", struct.getNameInfo().getFieldPath());
    assertEquals("struct", struct.getNameInfo().getFieldName());
    assertEquals(999, struct.getNameInfo().getFieldOffsetInCompiledBlock());
    assertEquals(2, struct.getArray().length);
  }

  @Test
  public void testConstructor_Name_ListOfFields() {
    final JBBPFieldStruct struct = new JBBPFieldStruct(new JBBPNamedFieldInfo("test.struct", "struct", 999), JBBPUtils.fieldsAsList(new JBBPFieldByte(null, (byte) 123), new JBBPFieldByte(null, (byte) -123)));
    assertNotNull(struct.getNameInfo());
    assertEquals("test.struct", struct.getNameInfo().getFieldPath());
    assertEquals("struct", struct.getNameInfo().getFieldName());
    assertEquals(999, struct.getNameInfo().getFieldOffsetInCompiledBlock());
    assertEquals(2, struct.getArray().length);
  }

  @Test
  public void testFindFieldForName() {
    final JBBPFieldByte field1 = new JBBPFieldByte(new JBBPNamedFieldInfo("struct.field1", "field1", 1024), (byte) 23);
    final JBBPFieldInt field2 = new JBBPFieldInt(new JBBPNamedFieldInfo("struct.field2", "field2", 1960), 23432);
    final JBBPFieldByte field3 = new JBBPFieldByte(new JBBPNamedFieldInfo("struct.field3", "field3", 2048), (byte) 78);

    final JBBPFieldStruct struct = new JBBPFieldStruct(new JBBPNamedFieldInfo("test.struct", "struct", 999), new JBBPAbstractField[] {field1, field2, field3});
    assertSame(field1, struct.findFieldForName("field1"));
    assertSame(field2, struct.findFieldForName("field2"));
    assertSame(field3, struct.findFieldForName("field3"));
    assertNull(struct.findFieldForName("field4"));
  }

  @Test
  public void testFindFieldForNameAndType() {
    final JBBPFieldByte field1 = new JBBPFieldByte(new JBBPNamedFieldInfo("struct.field1", "field1", 1024), (byte) 23);
    final JBBPFieldInt field2 = new JBBPFieldInt(new JBBPNamedFieldInfo("struct.field2", "field2", 1960), 23432);
    final JBBPFieldByte field3 = new JBBPFieldByte(new JBBPNamedFieldInfo("struct.field3", "field3", 2048), (byte) 78);

    final JBBPFieldStruct struct = new JBBPFieldStruct(new JBBPNamedFieldInfo("test.struct", "struct", 999), new JBBPAbstractField[] {field1, field2, field3});
    assertSame(field2, struct.findFieldForNameAndType("field2", JBBPFieldInt.class));
    assertNull(struct.findFieldForNameAndType("field2", JBBPFieldByte.class));
    assertNull(struct.findFieldForNameAndType("field1", JBBPFieldInt.class));
  }

  @Test
  public void testFindFieldForPathAndType() {
    final JBBPFieldByte field1 = new JBBPFieldByte(new JBBPNamedFieldInfo("struct.field1", "field1", 1024), (byte) 23);
    final JBBPFieldInt field2 = new JBBPFieldInt(new JBBPNamedFieldInfo("struct.field2", "field2", 1960), 23432);
    final JBBPFieldByte field3 = new JBBPFieldByte(new JBBPNamedFieldInfo("struct.field3", "field3", 2048), (byte) 78);

    final JBBPFieldStruct struct = new JBBPFieldStruct(new JBBPNamedFieldInfo("test.struct", "struct", 999), new JBBPAbstractField[] {field1, field2, field3});
    assertSame(field2, struct.findFieldForPathAndType("struct.field2", JBBPFieldInt.class));
    assertNull(struct.findFieldForPathAndType("field2", JBBPFieldByte.class));
    assertNull(struct.findFieldForPathAndType("field1", JBBPFieldInt.class));
  }

  @Test
  public void testFindFieldForPath() {
    final JBBPFieldByte field = new JBBPFieldByte(new JBBPNamedFieldInfo("struct1.struct2.field3", "field3", 2048), (byte) 78);
    final JBBPFieldStruct struct2 = new JBBPFieldStruct(new JBBPNamedFieldInfo("struct1.struct2", "struct2", 1024), new JBBPAbstractField[] {field});
    final JBBPFieldStruct struct1 = new JBBPFieldStruct(new JBBPNamedFieldInfo("struct1", "struct1", 1024), new JBBPAbstractField[] {struct2});

    try {
      struct1.findFieldForPath(null);
      fail("Must throw NPE");
    } catch (NullPointerException ex) {

    }

    try {
      struct1.findFieldForPath("struct1.struct2.field3.unknown");
      fail("Must throw finder exception for attempt to find inside a field");
    } catch (JBBPFinderException ex) {

    }

    assertNull(struct1.findFieldForPath("struct1.struct2.field0"));
    assertNull(struct1.findFieldForPath("struct"));
    assertSame(field, struct1.findFieldForPath("struct1.struct2.field3"));
  }

  @Test
  public void testFindFieldForType() {
    final JBBPFieldByte field1 = new JBBPFieldByte(new JBBPNamedFieldInfo("struct.field1", "field1", 1024), (byte) 23);
    final JBBPFieldInt field2 = new JBBPFieldInt(new JBBPNamedFieldInfo("struct.field2", "field2", 1960), 23432);
    final JBBPFieldByte field3 = new JBBPFieldByte(new JBBPNamedFieldInfo("struct.field3", "field3", 2048), (byte) 78);

    final JBBPFieldStruct struct = new JBBPFieldStruct(new JBBPNamedFieldInfo("test.struct", "struct", 999), new JBBPAbstractField[] {field1, field2, field3});
    try {
      struct.findFieldForType(JBBPFieldByte.class);
      fail("Must throw JBBPTooManyFieldsFoundException");
    } catch (JBBPTooManyFieldsFoundException ex) {
      assertEquals(2, ex.getNumberOfFoundInstances());
    }
    assertSame(field2, struct.findFieldForType(JBBPFieldInt.class));
    assertNull(struct.findFieldForType(JBBPFieldArrayByte.class));
  }

  @Test
  public void testFindFirstFieldForType() {
    final JBBPFieldByte field1 = new JBBPFieldByte(new JBBPNamedFieldInfo("struct.field1", "field1", 1024), (byte) 23);
    final JBBPFieldInt field2 = new JBBPFieldInt(new JBBPNamedFieldInfo("struct.field2", "field2", 1960), 23432);
    final JBBPFieldByte field3 = new JBBPFieldByte(new JBBPNamedFieldInfo("struct.field3", "field3", 2048), (byte) 78);

    final JBBPFieldStruct struct = new JBBPFieldStruct(new JBBPNamedFieldInfo("test.struct", "struct", 999), new JBBPAbstractField[] {field1, field2, field3});

    assertSame(field1, struct.findFirstFieldForType(JBBPFieldByte.class));
    assertSame(field2, struct.findFirstFieldForType(JBBPFieldInt.class));
    assertNull(struct.findFirstFieldForType(JBBPFieldArrayByte.class));
  }

  @Test
  public void testFindLastFieldForType() {
    final JBBPFieldByte field1 = new JBBPFieldByte(new JBBPNamedFieldInfo("struct.field1", "field1", 1024), (byte) 23);
    final JBBPFieldInt field2 = new JBBPFieldInt(new JBBPNamedFieldInfo("struct.field2", "field2", 1960), 23432);
    final JBBPFieldByte field3 = new JBBPFieldByte(new JBBPNamedFieldInfo("struct.field3", "field3", 2048), (byte) 78);

    final JBBPFieldStruct struct = new JBBPFieldStruct(new JBBPNamedFieldInfo("test.struct", "struct", 999), new JBBPAbstractField[] {field1, field2, field3});

    assertSame(field3, struct.findLastFieldForType(JBBPFieldByte.class));
    assertSame(field2, struct.findLastFieldForType(JBBPFieldInt.class));
    assertNull(struct.findLastFieldForType(JBBPFieldArrayByte.class));
  }

  @Test
  public void testPathExists() {
    final JBBPFieldByte field1 = new JBBPFieldByte(new JBBPNamedFieldInfo("struct.field1", "field1", 1024), (byte) 23);
    final JBBPFieldInt field2 = new JBBPFieldInt(new JBBPNamedFieldInfo("struct.field2", "field2", 1960), 23432);
    final JBBPFieldByte field3 = new JBBPFieldByte(new JBBPNamedFieldInfo("struct.field3", "field3", 2048), (byte) 78);

    final JBBPFieldStruct struct = new JBBPFieldStruct(new JBBPNamedFieldInfo("test.struct", "struct", 999), new JBBPAbstractField[] {field1, field2, field3});

    assertTrue(struct.pathExists("struct.field1"));
    assertFalse(struct.pathExists("field1"));
    assertFalse(struct.pathExists("struct.field4"));
  }

  @Test
  public void testNameExists() {
    final JBBPFieldByte field1 = new JBBPFieldByte(new JBBPNamedFieldInfo("struct.field1", "field1", 1024), (byte) 23);
    final JBBPFieldInt field2 = new JBBPFieldInt(new JBBPNamedFieldInfo("struct.field2", "field2", 1960), 23432);
    final JBBPFieldByte field3 = new JBBPFieldByte(new JBBPNamedFieldInfo("struct.field3", "field3", 2048), (byte) 78);

    final JBBPFieldStruct struct = new JBBPFieldStruct(new JBBPNamedFieldInfo("test.struct", "struct", 999), new JBBPAbstractField[] {field1, field2, field3});

    assertFalse(struct.nameExists("struct.field1"));
    assertTrue(struct.nameExists("field1"));
    assertFalse(struct.nameExists("struct.field4"));
    assertTrue(struct.nameExists("field3"));
  }

  @Test
  public void testMapTo_Class() throws Exception {
    final ClassTestMapToClass mapped = JBBPParser.prepare("byte a; byte b; byte c;").parse(new byte[] {1, 2, 3}).mapTo(ClassTestMapToClass.class);

    assertEquals(1, mapped.a);
    assertEquals(2, mapped.b);
    assertEquals(3, mapped.c);
  }

  @Test
  public void testMapTo_Object() throws Exception {
    final ClassTestMapToClass mapped = new ClassTestMapToClass();
    assertSame(mapped, JBBPParser.prepare("byte a; byte b; byte c;").parse(new byte[] {1, 2, 3}).mapTo(mapped));

    assertEquals(1, mapped.a);
    assertEquals(2, mapped.b);
    assertEquals(3, mapped.c);
  }

  @Test
  public void testInterStructFieldReferences() throws Exception {
    final JBBPParser parser = JBBPParser.prepare("header {ubyte sections; ubyte datalen;} sections [header.sections]{byte[header.datalen] data;}");

    final JBBPFieldArrayStruct sections = parser.parse(new byte[] {3, 2, 1, 2, 3, 4, 5, 6}).findFieldForNameAndType("sections", JBBPFieldArrayStruct.class);
    assertEquals(3, sections.size());
    for (int i = 0; i < 3; i++) {
      JBBPFieldArrayByte data = sections.getElementAt(i).findFieldForNameAndType("data", JBBPFieldArrayByte.class);
      final int base = i * 2;
      assertArrayEquals(new byte[] {(byte) (base + 1), (byte) (base + 2)}, data.getArray());
    }
  }

  private class ClassTestMapToClass {
    @Bin
    byte b;
    @Bin
    byte a;
    @Bin
    byte c;
  }

}
