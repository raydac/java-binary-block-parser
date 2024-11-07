package com.igormaznitsa.jbbp.it;

import static com.igormaznitsa.jbbp.io.JBBPByteOrder.LITTLE_ENDIAN;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import java.io.PrintWriter;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Test of parsing binary RM (reMarkable lines) format for <a href="https://remarkable.com/">Remarkablepaper device</a>
 * Information about the format was found <a href="https://plasma.ninja/blog/devices/remarkable/binary/format/2017/12/26/reMarkable-lines-file-format.html">here</a>
 */
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

//    final StringWriter writer = new StringWriter();
//    parsed.printSvg(new PrintWriter(writer, true));
//    System.out.println(writer.toString());
  }

  public static class RemarkableV5Body {
    @Bin(order = 1, arraySizeExpr = "43", type = BinType.BYTE_ARRAY)
    public String header;
    @Bin(order = 2, byteOrder = LITTLE_ENDIAN)
    public int nlayers;
    @Bin(order = 3, arraySizeExpr = "nlayers")
    public Layer[] layers;

    public void printSvg(final PrintWriter writer) {
      float maxX = Float.MIN_VALUE;
      float maxY = Float.MIN_VALUE;
      for (final Layer l : this.layers) {
        maxX = Math.max(maxX, l.findMaxX());
        maxY = Math.max(maxY, l.findMaxY());
      }

      writer.print(format("<svg version=\"1.1\"" +
          " baseProfile=\"full\"" +
          " xmlns=\"http://www.w3.org/2000/svg\"" +
          " xmlns:xlink=\"http://www.w3.org/1999/xlink\"" +
          " xmlns:ev=\"http://www.w3.org/2001/xml-events\"" +
          " width=\"%f\" height=\"%f\">", maxX, maxY));
      writer.println();
      writer.print(format("<rect width=\"%f\" height=\"%f\" fill=\"darkgrey\"/>", maxX, maxY));
      writer.println();
      for (final Layer layer : layers) {
        layer.printSvg(writer);
      }
      writer.print("</svg>");
    }

    public static class Layer {
      @Bin(order = 1, byteOrder = LITTLE_ENDIAN)
      public int nstrokes;
      @Bin(order = 2, arraySizeExpr = "nstrokes")
      public Stroke[] strokes;

      public void printSvg(final PrintWriter writer) {
        for (final Stroke stroke : strokes) {
          stroke.printSvg(writer);
        }
        writer.println();
      }

      public float findMaxX() {
        float result = Float.MIN_VALUE;
        for (final Stroke s : strokes) {
          result = Math.max(result, s.findMaxX());
        }
        return result;
      }

      public float findMaxY() {
        float result = Float.MIN_VALUE;
        for (final Stroke s : strokes) {
          result = Math.max(result, s.findMaxY());
        }
        return result;
      }

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

        private static String color2svg(final int index) {
          switch (index) {
            case 1:
              return "grey";
            case 2:
              return "white";
            default:
              return "black";
          }
        }

        private static float pen2opacity(final int index) {
          switch (index) {
            case 3:
            case 7:
            case 13:
            case 16:
              return 0.9f;
            case 5:
            case 18:
              return 0.2f;
            case 8:
              return 0.0f;
            default:
              return 1.0f;
          }
        }

        public float findMaxX() {
          float result = Float.MIN_VALUE;
          for (final Segment s : segments) {
            result = Math.max(result, s.x);
          }
          return result;
        }

        public float findMaxY() {
          float result = Float.MIN_VALUE;
          for (final Segment s : segments) {
            result = Math.max(result, s.y);
          }
          return result;
        }

        public void printSvg(final PrintWriter writer) {
          writer.print(format("<g id=\"%s\" style=\"stroke: %s; fill:none; opacity: %f\">",
              UUID.randomUUID(), color2svg(this.color), pen2opacity(this.pen)));
          writer.println();
          writer.print(format("<polyline stroke-width=\"%f\" points=\"", this.width));
          String space = "";
          for (final Segment s : segments) {
            writer.print(space);
            s.printSvg(writer);
            space = " ";
          }
          writer.print("\"/>");
          writer.println();
          writer.print("</g>");
          writer.println();
        }

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

          public void printSvg(final PrintWriter writer) {
            writer.print(format("%f,%f", x, y));
          }
        }
      }
    }
  }

}
