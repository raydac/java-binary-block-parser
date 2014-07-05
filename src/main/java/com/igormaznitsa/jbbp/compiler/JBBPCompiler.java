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

import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.compiler.parser.JBBPTokenizer;
import com.igormaznitsa.jbbp.compiler.parser.JBBPToken;
import com.igormaznitsa.jbbp.compiler.parser.JBBPTokenParameters;
import com.igormaznitsa.jbbp.compiler.utils.JBBPCompilerUtils;
import com.igormaznitsa.jbbp.compiler.varlen.JBBPLengthEvaluator;
import com.igormaznitsa.jbbp.compiler.varlen.JBBPEvaluatorFactory;
import com.igormaznitsa.jbbp.exceptions.JBBPCompilationException;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.*;
import java.util.*;

public final class JBBPCompiler {

  private static class StructStackItem {

    private final int startOffset;
    private final int code;
    private final JBBPToken token;

    private StructStackItem(int startOffset, int code, JBBPToken token) {
      this.startOffset = startOffset;
      this.code = code;
      this.token = token;
    }
  }

  public static final int CODE_ALIGN = 0x00;
  public static final int CODE_BIT = 0x01;
  public static final int CODE_BOOL = 0x02;
  public static final int CODE_UBYTE = 0x03;
  public static final int CODE_BYTE = 0x04;
  public static final int CODE_USHORT = 0x05;
  public static final int CODE_SHORT = 0x06;
  public static final int CODE_INT = 0x07;
  public static final int CODE_LONG = 0x08;
  public static final int CODE_STRUCT_START = 0x09;
  public static final int CODE_STRUCT_END = 0x0A;

  public static final int FLAG_NAMED = 0x10;
  public static final int FLAG_EXPRESSIONORWHOLE = 0x20;
  public static final int FLAG_ARRAY = 0x40;
  public static final int FLAG_LITTLE_ENDIAN = 0x80;

  public static JBBPCompiledBlock compile(final String script) throws IOException {
    final JBBPCompiledBlock.Builder builder = JBBPCompiledBlock.prepare().setSource(script);

    final List<JBBPNamedFieldInfo> namedFields = new ArrayList<JBBPNamedFieldInfo>();
    final List<JBBPLengthEvaluator> varLengthEvaluators = new ArrayList<JBBPLengthEvaluator>();

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    int offset = 0;

    final List<JBBPCompiler.StructStackItem> structureStack = new ArrayList<JBBPCompiler.StructStackItem>();
    final JBBPTokenizer parser = new JBBPTokenizer(script);

    int fieldUnrestrictedArrayOffset = -1;

    for (final JBBPToken token : parser) {
      if (token.isComment()) {
        continue;
      }

      final int code = prepareCodeForToken(token);
      final int startFieldOffset = offset;

      out.write(code);
      offset++;

      StructStackItem currentClosedStructure = null;
      int extraField = -1;

      if ((code & 0xF) != CODE_STRUCT_END && fieldUnrestrictedArrayOffset >= 0) {
        // check that the field is not int the current structure which is a whole stream one
        if (structureStack.isEmpty() || structureStack.get(structureStack.size() - 1).startOffset != fieldUnrestrictedArrayOffset) {
          throw new JBBPCompilationException("Attempt to read field or structure after a full stream field", token);
        }
      }

      switch (code & 0xF) {
        case CODE_BOOL:
        case CODE_BYTE:
        case CODE_UBYTE:
        case CODE_SHORT:
        case CODE_USHORT:
        case CODE_INT:
        case CODE_LONG:{
          // do nothing
        }break;
        case CODE_ALIGN: {
          if (token.getSizeAsString() != null) {
            throw new IllegalArgumentException("An Align field can't be array");
          }
          if (token.getFieldName() != null) {
            throw new IllegalArgumentException("An Align field can't be named [" + token.getFieldName() + ']');
          }

          final String parsedAlignBytesNumber = token.getFieldType().getExtraField();
          if (parsedAlignBytesNumber == null) {
            extraField = 1;
          }
          else {
            try {
              extraField = Integer.parseInt(parsedAlignBytesNumber);
            }
            catch (NumberFormatException ex) {
              extraField = -1;
            }
            if (extraField <= 0) {
              throw new JBBPCompilationException("Align byte number must be greater than zero [" + token.getFieldType().getExtraField() + ']', token);
            }
          }
        }
        break;
        case CODE_BIT: {
          final String parsedBitNumber = token.getFieldType().getExtraField();
          if (parsedBitNumber == null) {
            extraField = 1;
          }
          else {
            try {
              extraField = Integer.parseInt(parsedBitNumber);
            }
            catch (NumberFormatException ex) {
              extraField = -1;
            }
            if (extraField < 1 || extraField > 8) {
              throw new JBBPCompilationException("Wrong bit number, must be 1..8 [" + token.getFieldType().getExtraField() + ']', token);
            }
          }
        }
        break;
        case CODE_STRUCT_START: {
          structureStack.add(new StructStackItem(offset - 1, code, token));
        }
        break;
        case CODE_STRUCT_END: {
          if (structureStack.isEmpty()) {
            throw new JBBPCompilationException("Detected structure close tag without opening one", token);
          }
          else {
            currentClosedStructure = structureStack.remove(structureStack.size() - 1);
            offset += writePackedInt(out, currentClosedStructure.startOffset);
          }
        }
        break;
        default:
          throw new Error("Detected unsupported compiled code, notify the developer please [" + code + ']');
      }

      if ((code & FLAG_ARRAY) != 0) {
        if ((code & FLAG_EXPRESSIONORWHOLE) != 0) {
          if ("_".equals(token.getSizeAsString())) {
            if (fieldUnrestrictedArrayOffset >= 0) {
              throw new JBBPCompilationException("Detected two or more unlimited arrays [" + script + ']', token);
            }
            else {
              fieldUnrestrictedArrayOffset = offset - 1;
            }
          }
          else {
            varLengthEvaluators.add(JBBPEvaluatorFactory.getInstance().make(token.getSizeAsString(), namedFields, out.toByteArray()));
          }
        }
        else {
          offset += writePackedInt(out, token.getSizeAsInt());
        }
      }

      if (extraField >= 0) {
        out.write(extraField);
        offset++;
      }

      if ((code & FLAG_NAMED) != 0) {
        final String normalizedName = JBBPCompilerUtils.normalizeFieldName(token.getFieldName());
        assertName(normalizedName, token);
        registerNamedField(normalizedName, startFieldOffset, namedFields, token);
      }
      else {
        if (currentClosedStructure != null && (currentClosedStructure.code & FLAG_NAMED) != 0) {
          // it is structure, process field names
          final String normalizedName = JBBPCompilerUtils.normalizeFieldName(currentClosedStructure.token.getFieldName());
          for (int i = namedFields.size() - 1; i >= 0; i--) {
            final JBBPNamedFieldInfo f = namedFields.get(i);
            if (f.getFieldOffsetInCompiledBlock() <= currentClosedStructure.startOffset) {
              break;
            }
            final String newFullName = normalizedName + '.' + f.getFieldPath();
            namedFields.set(i, new JBBPNamedFieldInfo(newFullName, f.getFieldName(), f.getFieldOffsetInCompiledBlock()));
          }
        }
      }
    }

    if (!structureStack.isEmpty()) {
      throw new JBBPCompilationException("Detected nonclosed " + structureStack.size() + " structure(s)");
    }

    final byte[] compiledBlock = out.toByteArray();

    if (fieldUnrestrictedArrayOffset >= 0) {
      compiledBlock[fieldUnrestrictedArrayOffset] = (byte) (compiledBlock[fieldUnrestrictedArrayOffset] & ~FLAG_ARRAY);
    }

    return builder
            .setNamedFieldData(namedFields)
            .setVarLengthProcessors(varLengthEvaluators)
            .setCompiledData(compiledBlock)
            .build();
  }

  private static void assertName(final String name, final JBBPToken token) {
    if (name.indexOf('.') >= 0) {
      throw new JBBPCompilationException("Detected disallowed char '.' in name [" + name + ']', token);
    }
  }

  private static void registerNamedField(final String normalizedName, final int offset, final List<JBBPNamedFieldInfo> namedFields, final JBBPToken token) {
    if (JBBPCompilerUtils.findForName(normalizedName, namedFields) == null) {
      namedFields.add(new JBBPNamedFieldInfo(normalizedName, normalizedName, offset));
    }
    else {
      throw new JBBPCompilationException("Duplicated named field detected [" + normalizedName + ']', token);
    }
  }

  private static int writePackedInt(final OutputStream out, final int value) throws IOException {
    final byte[] packedInt = JBBPUtils.packInt(value);
    out.write(packedInt);
    return packedInt.length;
  }

  private static int prepareCodeForToken(final JBBPToken token) {
    int result = -1;
    switch (token.getType()) {
      case ATOM: {
        final JBBPTokenParameters descriptor = token.getFieldType();

        result = descriptor.getByteOrder() == JBBPByteOrder.LITTLE_ENDIAN ? FLAG_LITTLE_ENDIAN : 0;
        result |= token.getSizeAsString() == null ? 0 : (token.isVarArrayLength() ? FLAG_ARRAY | FLAG_EXPRESSIONORWHOLE : FLAG_ARRAY);
        result |= token.getFieldName() == null ? 0 : FLAG_NAMED;

        final String name = descriptor.getName().toLowerCase(Locale.ENGLISH);
        if ("align".equals(name)) {
          result = 0;
        }
        else if ("bit".equals(name)) {
          result |= CODE_BIT;
        }
        else if ("bool".equals(name)) {
          result |= CODE_BOOL;
        }
        else if ("ubyte".equals(name)) {
          result |= CODE_UBYTE;
        }
        else if ("byte".equals(name)) {
          result |= CODE_BYTE;
        }
        else if ("ushort".equals(name)) {
          result |= CODE_USHORT;
        }
        else if ("short".equals(name)) {
          result |= CODE_SHORT;
        }
        else if ("int".equals(name)) {
          result |= CODE_INT;
        }
        else if ("long".equals(name)) {
          result |= CODE_LONG;
        }
        else {
          throw new JBBPCompilationException("Unsupported type [" + descriptor.getName() + ']', token);
        }
      }
      break;
      case COMMENT: {
        // doesn't contain code
      }
      break;
      case STRUCT_START: {
        result = token.getSizeAsString() == null ? 0 : (token.isVarArrayLength() ? FLAG_ARRAY | FLAG_EXPRESSIONORWHOLE : FLAG_ARRAY);
        result |= token.getFieldName() == null ? 0 : FLAG_NAMED;
        result |= CODE_STRUCT_START;
      }
      break;
      case STRUCT_END: {
        result = CODE_STRUCT_END;
      }
      break;
      default:
        throw new Error("Unsupported type detected, contact developer! [" + token.getType() + ']');
    }
    return result;
  }
}
