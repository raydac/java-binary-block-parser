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

package com.igormaznitsa.mvn.tst;

import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.model.JBBPAbstractArrayField;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayLong;
import com.igormaznitsa.jbbp.model.JBBPFieldLong;
import com.igormaznitsa.jbbp.model.JBBPFieldShort;
import com.igormaznitsa.mvn.test.jbbp.VarCustom;

import java.io.IOException;

public class VarCustomImpl extends VarCustom {

  @Override
  public JBBPAbstractField readCustomFieldType(Object sourceStruct, JBBPBitInputStream inStream, JBBPFieldTypeParameterContainer typeParameterContainer, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue, boolean readWholeStream, int arraySize) throws IOException {
    return new JBBPFieldShort(nullableNamedFieldInfo, (short) inStream.readUnsignedShort(typeParameterContainer.getByteOrder()));
  }

  @Override
  public void writeCustomFieldType(Object sourceStruct, JBBPBitOutputStream outStream, JBBPAbstractField fieldValue, JBBPFieldTypeParameterContainer typeParameterContainer, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue, boolean wholeArray, int arraySize) throws IOException {
    outStream.writeShort(((JBBPFieldShort) fieldValue).getAsInt(), typeParameterContainer.getByteOrder());
  }

  @Override
  public JBBPAbstractField readVarField(Object sourceStruct, JBBPBitInputStream inStream, JBBPByteOrder byteOrder, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue) throws IOException {
    return new JBBPFieldLong(nullableNamedFieldInfo, inStream.readLong(byteOrder));
  }

  @Override
  public JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(Object sourceStruct, JBBPBitInputStream inStream, JBBPByteOrder byteOrder, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue, boolean readWholeStream, int arraySize) throws IOException {
    if (readWholeStream) {
      return new JBBPFieldArrayLong(nullableNamedFieldInfo, inStream.readLongArray(-1, byteOrder));
    } else {
      return new JBBPFieldArrayLong(nullableNamedFieldInfo, inStream.readLongArray(arraySize, byteOrder));
    }
  }

  @Override
  public void writeVarField(Object sourceStruct, JBBPAbstractField value, JBBPBitOutputStream outStream, JBBPByteOrder byteOrder, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue) throws IOException {
    outStream.writeLong(((JBBPFieldLong) value).getAsLong(), byteOrder);
  }

  @Override
  public void writeVarArray(Object sourceStruct, JBBPAbstractArrayField<? extends JBBPAbstractField> array, JBBPBitOutputStream outStream, JBBPByteOrder byteOrder, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue, int arraySizeToWrite) throws IOException {
    final JBBPFieldArrayLong a = (JBBPFieldArrayLong) array;
    if (arraySizeToWrite < 0) {
      for (final long l : a.getArray()) {
        outStream.writeLong(l, byteOrder);
      }
    } else {
      final long[] larr = a.getArray();
      for (int i = 0; i < arraySizeToWrite; i++) {
        outStream.writeLong(larr[i], byteOrder);
      }
    }
  }

}
