package com.igormaznitsa.jbbp.it;

import static com.igormaznitsa.jbbp.io.JBBPByteOrder.LITTLE_ENDIAN;
import static org.junit.jupiter.api.Assertions.assertEquals;


import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import org.junit.jupiter.api.Test;

public class RemarkableLinesParsingTest extends AbstractParserIntegrationTest {

  private static final String REMARKABLE_V5_LINES = "byte [43] header;" +
      "<int nlayers;" +
      "layers [nlayers] {" +
      "  <int nstrokes;" +
      "  strokes [nstrokes] {" +
      "    <int pen;" +
      "    <int color;" +
      "    <int unknown1;" +
      "    <floatj width;" +
      "    <int unknown2;" +
      "    <int nsegments;" +
      "    segments [nsegments] {" +
      "      <floatj x;" +
      "      <floatj y;" +
      "      <floatj pressure;" +
      "      <floatj tilt;" +
      "      <floatj unknown1;" +
      "      <floatj unknown2;" +
      "    }" +
      "  } " +
      "}";
  private static final JBBPParser PARSER = JBBPParser.prepare(REMARKABLE_V5_LINES);

  @Test
  public void testV5_Remarkable1() throws Exception {
    final RemarkableV5Body parsed;
    try (JBBPBitInputStream inputStream = new JBBPBitInputStream(
        getResourceAsInputStream("remarkable1.rm"))) {
      parsed =
          PARSER.parse(getResourceAsInputStream("remarkable1.rm")).mapTo(new RemarkableV5Body());
    }

    assertEquals("reMarkable .lines file, version=5", parsed.header.trim());
    final byte[] written = JBBPOut.BeginBin().Bin(parsed).End().toByteArray();
    assertResource("remarkable1.rm", written);
  }

  public static class RemarkableV5Body {
    @Bin(order = 1, arraySizeExpr = "43", type = BinType.BYTE_ARRAY)
    public String header;
    @Bin(order = 2, byteOrder = LITTLE_ENDIAN)
    public int nlayers;
    @Bin(order = 3, arraySizeExpr = "nlayers")
    public Layer[] layers;

    public static class Layer {
      @Bin(order = 1, byteOrder = LITTLE_ENDIAN)
      public int nstrokes;
      @Bin(order = 2, arraySizeExpr = "nstrokes")
      public Stroke[] strokes;

      public static class Stroke {
        @Bin(order = 1, byteOrder = LITTLE_ENDIAN)
        public int pen;
        @Bin(order = 2, byteOrder = LITTLE_ENDIAN)
        public int color;
        @Bin(order = 3, byteOrder = LITTLE_ENDIAN)
        public int unknown1;
        @Bin(order = 4, byteOrder = LITTLE_ENDIAN)
        public float width;
        @Bin(order = 5, byteOrder = LITTLE_ENDIAN)
        public int unknown2;
        @Bin(order = 6, byteOrder = LITTLE_ENDIAN)
        public int nsegments;
        @Bin(order = 7, arraySizeExpr = "nsegments")
        public Segment[] segments;

        public static class Segment {
          @Bin(order = 1, byteOrder = LITTLE_ENDIAN)
          public float x;
          @Bin(order = 2, byteOrder = LITTLE_ENDIAN)
          public float y;
          @Bin(order = 3, byteOrder = LITTLE_ENDIAN)
          public float pressure;
          @Bin(order = 4, byteOrder = LITTLE_ENDIAN)
          public float tilt;
          @Bin(order = 5, byteOrder = LITTLE_ENDIAN)
          public float unknown1;
          @Bin(order = 6, byteOrder = LITTLE_ENDIAN)
          public float unknown2;
        }
      }
    }
  }

}
