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
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class TGAParsingTest extends AbstractParserIntegrationTest {

    private static final JBBPParser TGAParser = JBBPParser.prepare(
            "Header {" +
                    "          ubyte IDLength;" +
                    "          ubyte ColorMapType;" +
                    "          ubyte ImageType;" +
                    "          <ushort CMapStart;" +
                    "          <ushort CMapLength;" +
                    "          ubyte CMapDepth;" +
                    "          <short XOffset;" +
                    "          <short YOffset;" +
                    "          <ushort Width;" +
                    "          <ushort Height;" +
                    "          ubyte PixelDepth;" +
                    "          ImageDesc {" +
                    "              bit:4 PixelAttrNumber;" +
                    "              bit:2 Pos;" +
                    "              bit:2 Reserved;" +
                    "          }" +
                    "      }" +
                    "byte [Header.IDLength] ImageID;" +
                    "ColorMap [ (Header.ColorMapType & 1) * Header.CMapLength ] {" +
                    "    byte [Header.CMapDepth >>> 3] ColorMapItem; " +
                    " }" +
                    "byte [_] ImageData;"
    );

    private void assertTgaFile(final JBBPFieldStruct parsedTga, final String imageId, final int width, final int height, final int pixelDepth, final int colorTableItems, final int imageDataSize) {
        final JBBPFieldArrayByte imageIdArray = parsedTga.findFieldForNameAndType("ImageID", JBBPFieldArrayByte.class);
        if (imageId == null || imageId.length() == 0) {
            assertEquals(0, imageIdArray.size());
        } else {
            assertEquals(imageId.length(), imageIdArray.size());
            for (int i = 0; i < imageId.length(); i++) {
                assertEquals(imageId.charAt(i) & 0xFF, imageIdArray.getArray()[i] & 0xFF);
            }
        }

        assertEquals(width, parsedTga.findFieldForPathAndType("header.Width", JBBPFieldUShort.class).getAsInt());
        assertEquals(height, parsedTga.findFieldForPathAndType("header.Height", JBBPFieldUShort.class).getAsInt());
        assertEquals(pixelDepth, parsedTga.findFieldForPathAndType("header.PixelDepth", JBBPFieldUByte.class).getAsInt());
        assertEquals(colorTableItems, parsedTga.findFieldForNameAndType("ColorMap", JBBPFieldArrayStruct.class).size());
        assertEquals(imageDataSize, parsedTga.findFieldForNameAndType("ImageData", JBBPFieldArrayByte.class).size());
    }

    @Test
    public void testTgaParsing_Cbw8() throws Exception {
        final InputStream tgaStream = getResourceAsInputStream("cbw8.tga");
        try {
            final JBBPFieldStruct result = TGAParser.parse(tgaStream);
            assertTgaFile(result, "Truevision(R) Sample Image", 128, 128, 8, 0, 8715);
        } finally {
            JBBPUtils.closeQuietly(tgaStream);
        }
    }

    @Test
    public void testTgaParsing_Xingt32() throws Exception {
        final InputStream tgaStream = getResourceAsInputStream("xing_t32.tga");
        try {
            final JBBPFieldStruct result = TGAParser.parse(tgaStream);
            assertTgaFile(result, "", 240, 164, 32, 0, 240 * 164 * 4);
        } finally {
            JBBPUtils.closeQuietly(tgaStream);
        }
    }

    @Test
    public void testTgaParsing_Logo() throws Exception {
        final InputStream tgaStream = getResourceAsInputStream("logo.tga");
        try {
            final JBBPFieldStruct result = TGAParser.parse(tgaStream);
            assertTgaFile(result, "", 319, 165, 32, 0, 116944);
            assertEquals(0, result.findFieldForPathAndType("Header.XOffset", JBBPFieldShort.class).getAsInt());
            assertEquals(165, result.findFieldForPathAndType("Header.YOffset", JBBPFieldShort.class).getAsInt());
        } finally {
            JBBPUtils.closeQuietly(tgaStream);
        }
    }

    @Test
    public void testTgaParsing_IndexedColorMap() throws Exception {
        final InputStream tgaStream = getResourceAsInputStream("indexedcolor.tga");
        try {
            final JBBPFieldStruct result = TGAParser.parse(tgaStream);
            assertTgaFile(result, "", 640, 480, 8, 256, 155403);
        } finally {
            JBBPUtils.closeQuietly(tgaStream);
        }
    }
}
