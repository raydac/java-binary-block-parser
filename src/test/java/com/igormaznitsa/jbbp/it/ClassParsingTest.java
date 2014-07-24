/*
 * Copyright 2014 Igor Maznitsa.
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

import com.igormaznitsa.jbbp.*;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.model.*;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.IOException;
import java.io.InputStream;
import static org.junit.Assert.*;
import org.junit.Test;

public class ClassParsingTest extends AbstractParserIntegrationTest {

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

  
  private class ConstantPoolItem {
    @Bin byte [] cp_item; 
  }

  private class Interface {
    @Bin char index; 
  }
  
  private class FieldMethodInfo {
    @Bin char access_flags; 
    @Bin char name_index;
    @Bin char descriptor_index;
    @Bin char attributes_count;
    @Bin AttributeInfo [] attribute_info;
  }

  private class AttributeInfo {
    @Bin char name_index;
    @Bin int length;
    @Bin byte [] info;
  }
  
  private class ClassFile {
    @Bin int magic;
    @Bin char minor_version; 
    @Bin char major_version; 
    @Bin char constant_pool_count; 
    @Bin ConstantPoolItem [] constant_pool_item;
    @Bin char access_flags;
    @Bin char this_class;
    @Bin char super_class;
    @Bin char interfaces_count;
    @Bin Interface [] interfaces;
    @Bin char fields_count;
    @Bin FieldMethodInfo [] fields;
    @Bin char methods_count;
    @Bin FieldMethodInfo [] methods;
    @Bin char attributes_count;
    @Bin AttributeInfo [] attribute_info;
  }
  
  private static final JBBPParser classParser = JBBPParser.prepare(
          "  int magic;"
          + "ushort minor_version;"
          + "ushort major_version;"
          + "ushort constant_pool_count;"
          + "constant_pool_item [constant_pool_count - 1] { var [111] cp_item; //we can make any array size because it depends on data\n }"
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

      public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(final JBBPBitInputStream inStream, final int arraySize, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        if ("cp_item".equals(fieldName.getFieldName())) {
          final int tagItem = inStream.readByte();
          final JBBPFieldArrayByte result;
          switch (tagItem) {
            case CONSTANT_Class: {
              result = new JBBPFieldArrayByte(fieldName, new byte[]{(byte) tagItem, (byte) inStream.readByte(), (byte) inStream.readByte()});
            }
            break;
            case CONSTANT_InterfaceMethodref:
            case CONSTANT_Methodref:
            case CONSTANT_Fieldref: {
              result = new JBBPFieldArrayByte(fieldName, new byte[]{(byte) tagItem, (byte) inStream.readByte(), (byte) inStream.readByte(), (byte) inStream.readByte(), (byte) inStream.readByte()});
            }
            break;
            case CONSTANT_String: {
              result = new JBBPFieldArrayByte(fieldName, new byte[]{(byte) tagItem, (byte) inStream.readByte(), (byte) inStream.readByte()});
            }
            break;
            case CONSTANT_Float:
            case CONSTANT_Integer: {
              result = new JBBPFieldArrayByte(fieldName, new byte[]{(byte) tagItem, (byte) inStream.readByte(), (byte) inStream.readByte(), (byte) inStream.readByte(), (byte) inStream.readByte()});
            }
            break;
            case CONSTANT_Double:
            case CONSTANT_Long: {
              result = new JBBPFieldArrayByte(fieldName, new byte[]{(byte) tagItem, (byte) inStream.readByte(), (byte) inStream.readByte(), (byte) inStream.readByte(), (byte) inStream.readByte(), (byte) inStream.readByte(), (byte) inStream.readByte(), (byte) inStream.readByte(), (byte) inStream.readByte()});
            }
            break;
            case CONSTANT_NameAndType: {
              result = new JBBPFieldArrayByte(fieldName, new byte[]{(byte) tagItem, (byte) inStream.readByte(), (byte) inStream.readByte(), (byte) inStream.readByte(), (byte) inStream.readByte()});
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
            case CONSTANT_MethodHandle: {
              result = new JBBPFieldArrayByte(fieldName, new byte[]{(byte) tagItem, (byte) inStream.readByte(), (byte) inStream.readByte(), (byte) inStream.readByte()});
            }
            break;
            case CONSTANT_MethodType: {
              result = new JBBPFieldArrayByte(fieldName, new byte[]{(byte) tagItem, (byte) inStream.readByte(), (byte) inStream.readByte()});
            }
            break;
            case CONSTANT_InvokeDynamic: {
              result = new JBBPFieldArrayByte(fieldName, new byte[]{(byte) tagItem, (byte) inStream.readByte(), (byte) inStream.readByte(), (byte) inStream.readByte()});
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

      public JBBPAbstractField readVarField(final JBBPBitInputStream inStream, final JBBPNamedFieldInfo fieldName, final int extraValue, final JBBPByteOrder byteOrder, final JBBPNamedNumericFieldMap numericFieldMap) throws IOException {
        fail("Must not be called");
        return null;
      }
    };
  }

  private String extractClassNameFromConstantPool(final ClassFile klazz, final int classInfoIndex) throws Exception {
    final byte [] constantClassInfo = klazz.constant_pool_item[classInfoIndex-1].cp_item;
    final int utf8Index = (constantClassInfo[1]<<8) | (constantClassInfo[2] & 0xFF);
    final byte [] utf8data = klazz.constant_pool_item[utf8Index - 1].cp_item;
    return new String(utf8data, 3, utf8data.length - 3, "UTF-8");
  }
  
  private void assertClass(final ClassFile klazz, final String className, final String superclass, final int interfaces, final int fields, final int methods) throws Exception {
    assertEquals(0xCAFEBABE, klazz.magic);
    assertEquals(className, extractClassNameFromConstantPool(klazz, klazz.this_class));
    assertEquals(superclass, extractClassNameFromConstantPool(klazz, klazz.super_class));
    assertEquals(interfaces, klazz.interfaces.length);
    assertEquals(fields, klazz.fields.length);
    assertEquals(methods, klazz.methods.length);
  }
  
  @Test
  public void testParseClassFile() throws Exception {
    final InputStream in = getResourceAsInputStream("test.clazz");
    try {
      final ClassFile klazz = classParser.parse(in, getVarFieldProcessor(), null).mapTo(ClassFile.class);
      assertClass(klazz, "Test", "java/lang/Object", 0, 2, 4);
    }
    finally {
      JBBPUtils.closeQuietly(in);
    }
  }
}
