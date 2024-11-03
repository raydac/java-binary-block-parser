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

package com.igormaznitsa.jbbp.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.io.JBBPArraySizeLimiter;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.jbbp.model.JBBPFieldByte;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class JBBPCustomFieldTypeProcessorAggregatorTest {

  @Test
  public void testConstructor_ErrorForDuplicatedType() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> {
      final JBBPCustomFieldTypeProcessor proc1 = new JBBPCustomFieldTypeProcessor() {

        @Override
        public String[] getCustomFieldTypes() {
          return new String[] {"type1", "type2", "type3"};
        }

        @Override
        public boolean isAllowed(JBBPFieldTypeParameterContainer fieldType, String fieldName,
                                 int extraData, boolean isArray) {
          throw new UnsupportedOperationException(
              "Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public JBBPAbstractField readCustomFieldType(JBBPBitInputStream in, JBBPBitOrder bitOrder,
                                                     int parserFlags,
                                                     JBBPFieldTypeParameterContainer customTypeFieldInfo,
                                                     JBBPNamedFieldInfo fieldName, int extraData,
                                                     boolean readWholeStream, int arrayLength,
                                                     JBBPArraySizeLimiter arraySizeLimiter)
            throws IOException {
          throw new UnsupportedOperationException(
              "Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
      };

      final JBBPCustomFieldTypeProcessor proc2 = new JBBPCustomFieldTypeProcessor() {

        @Override
        public String[] getCustomFieldTypes() {
          return new String[] {"type5", "type6", "type3"};
        }

        @Override
        public boolean isAllowed(JBBPFieldTypeParameterContainer fieldType, String fieldName,
                                 int extraData, boolean isArray) {
          throw new UnsupportedOperationException(
              "Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public JBBPAbstractField readCustomFieldType(JBBPBitInputStream in, JBBPBitOrder bitOrder,
                                                     int parserFlags,
                                                     JBBPFieldTypeParameterContainer customTypeFieldInfo,
                                                     JBBPNamedFieldInfo fieldName, int extraData,
                                                     boolean readWholeStream, int arrayLength,
                                                     JBBPArraySizeLimiter arraySizeLimiter)
            throws IOException {
          throw new UnsupportedOperationException(
              "Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
      };

      new JBBPCustomFieldTypeProcessorAggregator(proc1, proc2);
    });
  }

  @Test
  public void testJoiningTypes() throws Exception {
    final JBBPCustomFieldTypeProcessor proc1 = new JBBPCustomFieldTypeProcessor() {

      @Override
      public String[] getCustomFieldTypes() {
        return new String[] {"type1", "type2", "type3"};
      }

      @Override
      public boolean isAllowed(JBBPFieldTypeParameterContainer fieldType, String fieldName,
                               int extraData, boolean isArray) {
        throw new UnsupportedOperationException(
            "Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public JBBPAbstractField readCustomFieldType(JBBPBitInputStream in, JBBPBitOrder bitOrder,
                                                   int parserFlags,
                                                   JBBPFieldTypeParameterContainer customTypeFieldInfo,
                                                   JBBPNamedFieldInfo fieldName, int extraData,
                                                   boolean readWholeStream, int arrayLength,
                                                   JBBPArraySizeLimiter arraySizeLimiter)
          throws IOException {
        throw new UnsupportedOperationException(
            "Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
    };

    final JBBPCustomFieldTypeProcessor proc2 = new JBBPCustomFieldTypeProcessor() {

      @Override
      public String[] getCustomFieldTypes() {
        return new String[] {"type4", "type5", "type6"};
      }

      @Override
      public boolean isAllowed(JBBPFieldTypeParameterContainer fieldType, String fieldName,
                               int extraData, boolean isArray) {
        throw new UnsupportedOperationException(
            "Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }

      @Override
      public JBBPAbstractField readCustomFieldType(JBBPBitInputStream in, JBBPBitOrder bitOrder,
                                                   int parserFlags,
                                                   JBBPFieldTypeParameterContainer customTypeFieldInfo,
                                                   JBBPNamedFieldInfo fieldName, int extraData,
                                                   boolean readWholeStream, int arrayLength,
                                                   JBBPArraySizeLimiter arraySizeLimiter)
          throws IOException {
        throw new UnsupportedOperationException(
            "Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
    };

    final List<String> types = Arrays
        .asList(new JBBPCustomFieldTypeProcessorAggregator(proc1, proc2).getCustomFieldTypes());

    assertEquals(6, types.size());
    assertTrue(types.contains("type1"));
    assertTrue(types.contains("type2"));
    assertTrue(types.contains("type3"));
    assertTrue(types.contains("type4"));
    assertTrue(types.contains("type5"));
    assertTrue(types.contains("type6"));
  }

  @Test
  public void testAllowedAndRead() throws Exception {

    final List<Record> allowed = new ArrayList<>();
    final List<Record> read = new ArrayList<>();

    final JBBPCustomFieldTypeProcessor proc1 = new JBBPCustomFieldTypeProcessor() {

      @Override
      public String[] getCustomFieldTypes() {
        return new String[] {"type1", "type2", "type3"};
      }

      @Override
      public boolean isAllowed(JBBPFieldTypeParameterContainer fieldType, String fieldName,
                               int extraData, boolean isArray) {
        allowed.add(new Record(fieldType.getTypeName(), this));
        return true;
      }

      @Override
      public JBBPAbstractField readCustomFieldType(JBBPBitInputStream in, JBBPBitOrder bitOrder,
                                                   int parserFlags,
                                                   JBBPFieldTypeParameterContainer customTypeFieldInfo,
                                                   JBBPNamedFieldInfo fieldName, int extraData,
                                                   boolean readWholeStream, int arrayLength,
                                                   JBBPArraySizeLimiter arraySizeLimiter)
          throws IOException {
        read.add(new Record(customTypeFieldInfo.getTypeName(), this));
        return new JBBPFieldByte(fieldName, (byte) in.readByte());
      }
    };

    final JBBPCustomFieldTypeProcessor proc2 = new JBBPCustomFieldTypeProcessor() {

      @Override
      public String[] getCustomFieldTypes() {
        return new String[] {"type4", "type5", "type6"};
      }

      @Override
      public boolean isAllowed(JBBPFieldTypeParameterContainer fieldType, String fieldName,
                               int extraData, boolean isArray) {
        allowed.add(new Record(fieldType.getTypeName(), this));
        return true;
      }

      @Override
      public JBBPAbstractField readCustomFieldType(JBBPBitInputStream in, JBBPBitOrder bitOrder,
                                                   int parserFlags,
                                                   JBBPFieldTypeParameterContainer customTypeFieldInfo,
                                                   JBBPNamedFieldInfo fieldName, int extraData,
                                                   boolean readWholeStream, int arrayLength,
                                                   JBBPArraySizeLimiter arraySizeLimiter)
          throws IOException {
        read.add(new Record(customTypeFieldInfo.getTypeName(), this));
        return new JBBPFieldByte(fieldName, (byte) in.readByte());
      }
    };

    final JBBPParser parser = JBBPParser
        .prepare("type1; type2; type3; type4; type5; type6;", JBBPBitOrder.LSB0,
            new JBBPCustomFieldTypeProcessorAggregator(proc1, proc2), 0);

    assertEquals(6, allowed.size());

    assertEquals("type1", allowed.get(0).type);
    assertSame(proc1, allowed.get(0).proc);

    assertEquals("type2", allowed.get(1).type);
    assertSame(proc1, allowed.get(1).proc);

    assertEquals("type3", allowed.get(2).type);
    assertSame(proc1, allowed.get(2).proc);

    assertEquals("type4", allowed.get(3).type);
    assertSame(proc2, allowed.get(3).proc);

    assertEquals("type5", allowed.get(4).type);
    assertSame(proc2, allowed.get(4).proc);

    assertEquals("type6", allowed.get(5).type);
    assertSame(proc2, allowed.get(5).proc);

    parser.parse(new byte[] {0, 0, 0, 0, 0, 0});

    assertEquals(6, read.size());

    assertEquals("type1", read.get(0).type);
    assertSame(proc1, read.get(0).proc);

    assertEquals("type2", read.get(1).type);
    assertSame(proc1, read.get(1).proc);

    assertEquals("type3", read.get(2).type);
    assertSame(proc1, read.get(2).proc);

    assertEquals("type4", read.get(3).type);
    assertSame(proc2, read.get(3).proc);

    assertEquals("type5", read.get(4).type);
    assertSame(proc2, read.get(4).proc);

    assertEquals("type6", read.get(5).type);
    assertSame(proc2, read.get(5).proc);

  }

  static class Record {

    final String type;
    final JBBPCustomFieldTypeProcessor proc;

    public Record(final String type, final JBBPCustomFieldTypeProcessor proc) {
      this.type = type;
      this.proc = proc;
    }
  }

}
