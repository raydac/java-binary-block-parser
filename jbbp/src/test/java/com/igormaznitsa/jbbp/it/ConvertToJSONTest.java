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

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.model.*;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConvertToJSONTest extends AbstractParserIntegrationTest {

    public static JSONObject convertToJSon(final JSONObject jsn, final JBBPAbstractField field) {
        final JSONObject json = jsn == null ? new JSONObject() : jsn;

        final String fieldName = field.getFieldName() == null ? "nonamed" : field.getFieldName();
        if (field instanceof JBBPAbstractArrayField) {
            final JSONArray jsonArray = new JSONArray();
            if (field instanceof JBBPFieldArrayBit) {
                for (final byte b : ((JBBPFieldArrayBit) field).getArray()) {
                    jsonArray.add(b);
                }
            } else if (field instanceof JBBPFieldArrayBoolean) {
                for (final boolean b : ((JBBPFieldArrayBoolean) field).getArray()) {
                    jsonArray.add(b);
                }
            } else if (field instanceof JBBPFieldArrayByte) {
                for (final byte b : ((JBBPFieldArrayByte) field).getArray()) {
                    jsonArray.add(b);
                }
            } else if (field instanceof JBBPFieldArrayInt) {
                for (final int b : ((JBBPFieldArrayInt) field).getArray()) {
                    jsonArray.add(b);
                }
            } else if (field instanceof JBBPFieldArrayLong) {
                for (final long b : ((JBBPFieldArrayLong) field).getArray()) {
                    jsonArray.add(b);
                }
            } else if (field instanceof JBBPFieldArrayShort) {
                for (final short b : ((JBBPFieldArrayShort) field).getArray()) {
                    jsonArray.add(b);
                }
            } else if (field instanceof JBBPFieldArrayStruct) {
                final JBBPFieldArrayStruct array = (JBBPFieldArrayStruct) field;
                for (int i = 0; i < array.size(); i++) {
                    jsonArray.add(convertToJSon(new JSONObject(), array.getElementAt(i)));
                }
            } else if (field instanceof JBBPFieldArrayUByte) {
                for (final byte b : ((JBBPFieldArrayUByte) field).getArray()) {
                    jsonArray.add(b & 0xFF);
                }
            } else if (field instanceof JBBPFieldArrayUShort) {
                for (final short b : ((JBBPFieldArrayUShort) field).getArray()) {
                    jsonArray.add(b & 0xFFFF);
                }
            } else {
                throw new Error("Unexpected field type");
            }
            json.put(fieldName, jsonArray);
        } else {
            if (field instanceof JBBPFieldBit) {
                json.put(fieldName, ((JBBPFieldBit) field).getAsInt());
            } else if (field instanceof JBBPFieldBoolean) {
                json.put(fieldName, ((JBBPFieldBoolean) field).getAsBool());
            } else if (field instanceof JBBPFieldByte) {
                json.put(fieldName, ((JBBPFieldByte) field).getAsInt());
            } else if (field instanceof JBBPFieldInt) {
                json.put(fieldName, ((JBBPFieldInt) field).getAsInt());
            } else if (field instanceof JBBPFieldLong) {
                json.put(fieldName, ((JBBPFieldLong) field).getAsLong());
            } else if (field instanceof JBBPFieldShort) {
                json.put(fieldName, ((JBBPFieldShort) field).getAsInt());
            } else if (field instanceof JBBPFieldStruct) {
                final JBBPFieldStruct struct = (JBBPFieldStruct) field;
                final JSONObject obj = new JSONObject();
                for (final JBBPAbstractField f : struct.getArray()) {
                    convertToJSon(obj, f);
                }
                if (jsn == null) {
                    json.putAll(obj);
                } else {
                    json.put(fieldName, obj);
                }
            } else if (field instanceof JBBPFieldUByte) {
                json.put(fieldName, ((JBBPFieldUByte) field).getAsInt());
            } else if (field instanceof JBBPFieldUShort) {
                json.put(fieldName, ((JBBPFieldUShort) field).getAsInt());
            } else {
                throw new Error("Unexpected field");
            }
        }
        return json;
    }

    @Test
    public void testConvertToJSON() throws Exception {
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

            final JSONObject json = convertToJSon(null, pngParser.parse(pngStream));
            final String jsonText = json.toJSONString(JSONStyle.MAX_COMPRESS);
            assertTrue(jsonText.length() == 13917);
            assertTrue(jsonText.contains("header:"));
            assertTrue(jsonText.contains("chunk:{"));
            assertTrue(jsonText.contains("length:"));
            assertTrue(jsonText.contains("type:"));
            assertTrue(jsonText.contains("data:"));
            assertTrue(jsonText.contains("crc:"));
        } finally {
            pngStream.close();
        }
    }
}
