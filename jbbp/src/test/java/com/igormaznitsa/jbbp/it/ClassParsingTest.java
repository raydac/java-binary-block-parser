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

package com.igormaznitsa.jbbp.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.igormaznitsa.jbbp.JBBPNamedNumericFieldMap;
import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.JBBPVarFieldProcessor;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.io.JBBPArraySizeLimiter;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.model.JBBPAbstractArrayField;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class ClassParsingTest extends AbstractParserIntegrationTest {

  //    public static final int FORMAT_J2SE8 = 0x34;
  public static final int FORMAT_J2SE7 = 0x33;
  //    public static final int FORMAT_J2SE6 = 0x32;
  public static final int FORMAT_J2SE5 = 0x31;
//    public static final int FORMAT_JDK14 = 0x30;
//    public static final int FORMAT_JDK13 = 0x2F;
//    public static final int FORMAT_JDK12 = 0x2E;
//    public static final int FORMAT_JDK11 = 0x2D;

  private static final int CONSTANT_Class = 7;
  private static final int CONSTANT_Fieldref = 9;
  private static final int CONSTANT_Methodref = 10;
  private static final int CONSTANT_InterfaceMethodref = 11;
  private static final int CONSTANT_String = 8;
  private static final int CONSTANT_Integer = 3;
  private static final int CONSTANT_Float = 4;
  private static final int CONSTANT_Long = 5;
  private static final int CONSTANT_Double = 6;
  private static final int CONSTANT_NameAndType = 12;
  private static final int CONSTANT_Utf8 = 1;
  private static final int CONSTANT_MethodHandle = 15;
  private static final int CONSTANT_MethodType = 16;
  private static final int CONSTANT_InvokeDynamic = 18;

  private static final String[] AllowedAttributes = new String[] {
      "ConstantValue",
      "Code",
      "StackMapTable",
      "Exceptions",
      "InnerClasses",
      "EnclosingMethod",
      "Synthetic",
      "Signature",
      "SourceFile",
      "SourceDebugExtension",
      "LineNumberTable",
      "LocalVariableTable",
      "LocalVariableTypeTable",
      "Deprecated",
      "RuntimeVisibleAnnotations",
      "RuntimeInvisibleAnnotations",
      "RuntimeVisibleParameterAnnotations",
      "RuntimeInvisibleParameterAnnotations",
      "AnnotationDefault",
      "BootstrapMethods"
  };
  private static final JBBPParser classParser = JBBPParser.prepare(
      "  int magic;"
          + "ushort minor_version;"
          + "ushort major_version;"
          + "ushort constant_pool_count;"
          +
          "constant_pool_item [constant_pool_count - 1] { var [1] cp_item; //we can make any array size because the field will be processed by a custom processor\n }"
          + "ushort access_flags;"
          + "ushort this_class;"
          + "ushort super_class;"
          + "ushort interfaces_count;"
          + "interfaces[interfaces_count]{"
          + "     ushort index;"
          + "}"
          + "ushort fields_count;"
          + "fields[fields_count]{"
          + "    ushort access_flags;"
          + "    ushort name_index;"
          + "    ushort descriptor_index;"
          + "    ushort attributes_count;"
          + "    attribute_info [attributes_count] {"
          + "             ushort name_index;"
          + "             int length;"
          + "             byte [length] info;"
          + "    }"
          + "}"
          + "ushort methods_count;"
          + "methods[methods_count]{"
          + "    ushort access_flags;"
          + "    ushort name_index;"
          + "    ushort descriptor_index;"
          + "    ushort attributes_count;"
          + "    attribute_info[attributes_count]{"
          + "             ushort name_index;"
          + "             int length;"
          + "             byte [length] info;"
          + "  }"
          + "}"
          + "ushort attributes_count;"
          + "attribute_info[attributes_count]{"
          + "    ushort name_index;"
          + "    int length;"
          + "    byte [length] info;"
          + "}");

  private JBBPVarFieldProcessor getVarFieldProcessor() {
    return new JBBPVarFieldProcessor() {

      @Override
      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(
          final JBBPBitInputStream inStream, final int arraySize,
          final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder,
          final JBBPNamedNumericFieldMap numericFieldMap,
          final JBBPArraySizeLimiter arraySizeLimiter) throws IOException {
        if ("cp_item".equals(fieldName.getFieldName())) {
          final int tagItem = inStream.readByte();
          final JBBPFieldArrayByte result;
          switch (tagItem) {
            case CONSTANT_Class:
            case CONSTANT_String:
            case CONSTANT_MethodType: {
              result = new JBBPFieldArrayByte(fieldName,
                  new byte[] {(byte) tagItem, (byte) inStream.readByte(),
                      (byte) inStream.readByte()});
            }
            break;
            case CONSTANT_InterfaceMethodref:
            case CONSTANT_Methodref:
            case CONSTANT_Fieldref:
            case CONSTANT_Float:
            case CONSTANT_Integer:
            case CONSTANT_NameAndType: {
              result = new JBBPFieldArrayByte(fieldName,
                  new byte[] {(byte) tagItem, (byte) inStream.readByte(),
                      (byte) inStream.readByte(), (byte) inStream.readByte(),
                      (byte) inStream.readByte()});
            }
            break;
            case CONSTANT_Double:
            case CONSTANT_Long: {
              result = new JBBPFieldArrayByte(fieldName,
                  new byte[] {(byte) tagItem, (byte) inStream.readByte(),
                      (byte) inStream.readByte(), (byte) inStream.readByte(),
                      (byte) inStream.readByte(), (byte) inStream.readByte(),
                      (byte) inStream.readByte(), (byte) inStream.readByte(),
                      (byte) inStream.readByte()});
            }
            break;
            case CONSTANT_Utf8: {
              final int len = inStream.readUnsignedShort(byteOrder);
              final byte[] array = inStream.readByteArray(len);

              final byte[] res = new byte[array.length + 3];
              res[0] = (byte) tagItem;
              res[1] = (byte) (len >>> 8);
              res[2] = (byte) len;
              System.arraycopy(array, 0, res, 3, array.length);

              result = new JBBPFieldArrayByte(fieldName, res);
            }
            break;
            case CONSTANT_MethodHandle:
            case CONSTANT_InvokeDynamic: {
              result = new JBBPFieldArrayByte(fieldName,
                  new byte[] {(byte) tagItem, (byte) inStream.readByte(),
                      (byte) inStream.readByte(), (byte) inStream.readByte()});
            }
            break;
            default: {
              fail("Can't process constant pool tag [" + tagItem + ']');
              throw new Error();
            }
          }
          return result;
        }
        fail("Unsupported var field [" + fieldName + ']');
        return null;
      }

      @Override
      public JBBPAbstractField readVarField(final JBBPBitInputStream inStream,
                                            final JBBPNamedFieldInfo fieldName,
                                            final int extraValue, final JBBPByteOrder byteOrder,
                                            final JBBPNamedNumericFieldMap numericFieldMap)
          throws IOException {
        fail("Must not be called");
        return null;
      }
    };
  }

  private String extractClassNameFromConstantPool(final ClassFile klazz, final int classInfoIndex)
      throws Exception {
    final byte[] constantClassInfo = klazz.constant_pool_item[classInfoIndex - 1].cp_item;
    final int utf8Index = (constantClassInfo[1] << 8) | (constantClassInfo[2] & 0xFF);
    return extractUtf8FromConstantPool(klazz, utf8Index);
  }

  private String extractUtf8FromConstantPool(final ClassFile klazz, final int utf8Index)
      throws Exception {
    final byte[] utf8data = klazz.constant_pool_item[utf8Index - 1].cp_item;
    return new String(utf8data, 3, utf8data.length - 3, StandardCharsets.UTF_8);
  }

  private void assertAttribute(final ClassFile klass, final AttributeInfo attr) throws Exception {
    final String attrName = extractUtf8FromConstantPool(klass, attr.name_index);
    for (final String s : AllowedAttributes) {
      if (s.equals(attrName)) {
        return;
      }
    }
    fail("Disallowed attribute '" + attrName + '\'');
  }

  private void assertClass(final ClassFile klazz, final int majorVersion, final String className,
                           final String superclass, final int interfaces, final int fields,
                           final int methods) throws Exception {
    assertEquals(0xCAFEBABE, klazz.magic);
    assertEquals(0, klazz.minor_version);
    assertEquals(majorVersion, klazz.major_version);
    assertEquals(className, extractClassNameFromConstantPool(klazz, klazz.this_class));
    assertEquals(superclass, extractClassNameFromConstantPool(klazz, klazz.super_class));
    assertEquals(interfaces, klazz.interfaces.length);
    assertEquals(fields, klazz.fields.length);
    assertEquals(methods, klazz.methods.length);

    if (fields > 0) {
      for (final FieldMethodInfo info : klazz.fields) {
        assertTrue(klazz.attribute_info.length > 0);
        for (final AttributeInfo ainfo : info.attribute_info) {
          assertAttribute(klazz, ainfo);
        }
      }
    }

    if (methods > 0) {
      for (final FieldMethodInfo info : klazz.methods) {
        assertTrue(klazz.attribute_info.length > 0);
        for (final AttributeInfo ainfo : info.attribute_info) {
          assertAttribute(klazz, ainfo);
        }
      }
    }

    assertTrue(klazz.attribute_info.length > 0);
    for (final AttributeInfo ainfo : klazz.attribute_info) {
      assertAttribute(klazz, ainfo);
    }
  }

  @Test
  public void testParseClassFile_TestClass() throws Exception {
    final InputStream in = getResourceAsInputStream("test.clazz");
    try {
      final ClassFile klazz =
          classParser.parse(in, getVarFieldProcessor(), null).mapTo(new ClassFile());
      assertClass(klazz, FORMAT_J2SE7, "Test", "java/lang/Object", 0, 2, 4);
      assertEquals(831, classParser.getFinalStreamByteCounter());
    } finally {
      JBBPUtils.closeQuietly(in);
    }
  }

  @Test
  public void testParseClassFile_HexEngineClass() throws Exception {
    final InputStream in = getResourceAsInputStream("hexengine.clazz");
    try {
      final ClassFile klazz =
          classParser.parse(in, getVarFieldProcessor(), null).mapTo(new ClassFile());
      assertClass(klazz, FORMAT_J2SE5, "com/igormaznitsa/jhexed/engine/HexEngine",
          "java/lang/Object", 0, 22, 44);
      assertEquals(21364, classParser.getFinalStreamByteCounter());
    } finally {
      JBBPUtils.closeQuietly(in);
    }
  }

  @Bin
  public static class ConstantPoolItem {
    byte[] cp_item;
  }

  @Bin
  public static class Interface {
    char index;
  }

  @Bin
  public static class FieldMethodInfo {
    char access_flags;
    char name_index;
    char descriptor_index;
    char attributes_count;
    AttributeInfo[] attribute_info;
  }

  @Bin
  public static class AttributeInfo {
    char name_index;
    int length;
    byte[] info;
  }

  @Bin
  public static class ClassFile {
    int magic;
    char minor_version;
    char major_version;
    char constant_pool_count;
    ConstantPoolItem[] constant_pool_item;
    char access_flags;
    char this_class;
    char super_class;
    char interfaces_count;
    Interface[] interfaces;
    char fields_count;
    FieldMethodInfo[] fields;
    char methods_count;
    FieldMethodInfo[] methods;
    char attributes_count;
    AttributeInfo[] attribute_info;
  }
}
