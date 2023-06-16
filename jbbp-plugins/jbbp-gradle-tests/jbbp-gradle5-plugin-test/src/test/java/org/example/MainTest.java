package org.example;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import com.igormaznitsa.mvn.tst.VarCustomImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import com.igormaznitsa.mvn.test.jbbp.*;

public class MainTest {

  @Test
  void testReadWrite_Annotations_Static() throws IOException {
    final byte[] testData = new byte[] {
        4,
        (byte)0xFF, (byte)0x1A, (byte)0x1B, (byte)0x1C,
        (byte)0xFF, (byte)0x2A, (byte)0x2B, (byte)0x2C,
        (byte) 0x12, (byte) 0x34, 3, 5, 6, 7
    };

    final GenAnnotations result =
        new GenAnnotations().read(new JBBPBitInputStream(new ByteArrayInputStream(testData)));
    assertEquals(4, result.getLEN());
    assertEquals(3, result.getSOME1().getSOME2().getFIELD().length);

    final String script = "ubyte len;"
        + " uint uintField;"
        + " uint [1] uintArr;"
        + "some1 {"
        + " bit:4 [len] someField;"
        + " ubyte len;"
        + " some2 {"
        + "   byte [len] field;"
        + " }"
        + "}";

    final GenAnnotations instance =
        JBBPParser.prepare(script).parse(testData).mapTo(new GenAnnotations());

    assertEquals(result.getUINTFIELD(), instance.getUINTFIELD());
    assertArrayEquals(result.getUINTARR(), instance.getUINTARR());
    assertEquals(result.getLEN(), instance.getLEN());
    assertEquals(result.getSOME1().getLEN(), instance.getSOME1().getLEN());
    assertArrayEquals(result.getSOME1().getSOMEFIELD(), instance.getSOME1().getSOMEFIELD());
    assertArrayEquals(result.getSOME1().getSOME2().getFIELD(),
        instance.getSOME1().getSOME2().getFIELD());
  }

  @Test
  void testReadWrite_Annotations_NonStatic() throws IOException {
    final byte[] testData = new byte[] {4, (byte) 0x12, (byte) 0x34, 3, 5, 6, 7};

    final GenAnnotationsNonStatic result = new GenAnnotationsNonStatic()
        .read(new JBBPBitInputStream(new ByteArrayInputStream(testData)));
    assertEquals(4, result.getLEN());
    assertEquals(3, result.getSOME1().getSOME2().getFIELD().length);

    final String script = "ubyte len;"
        + "some1 {"
        + " bit:4 [len] someField;"
        + " ubyte len;"
        + " some2 {"
        + "   byte [len] field;"
        + " }"
        + "}";

    final GenAnnotationsNonStatic instance =
        JBBPParser.prepare(script).parse(testData).mapTo(new GenAnnotationsNonStatic());
    assertEquals(result.getLEN(), instance.getLEN());
    assertEquals(result.getSOME1().getLEN(), instance.getSOME1().getLEN());
    assertArrayEquals(result.getSOME1().getSOMEFIELD(), instance.getSOME1().getSOMEFIELD());
    assertArrayEquals(result.getSOME1().getSOME2().getFIELD(),
        instance.getSOME1().getSOME2().getFIELD());
  }

  private static final Random RND = new Random(12345);

  @Test
  public void testReadWrite_VarCustom() throws Exception {
    final VarCustomImpl impl = new VarCustomImpl();

    final byte[] etalonArray = new byte[319044];
    RND.nextBytes(etalonArray);
    etalonArray[0] = 1;

    impl.read(new JBBPBitInputStream(new ByteArrayInputStream(etalonArray)));

    assertEquals(1, impl.getBYTEA());

    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final JBBPBitOutputStream bios = new JBBPBitOutputStream(bos);

    impl.write(bios);
    bios.close();

    assertArrayEquals(etalonArray, bos.toByteArray());
  }

  @Test
  public void testRead_DefaultBitOrder() throws IOException {
    final WholeStreamByteArray struct = new WholeStreamByteArray().read(new JBBPBitInputStream(
        new ByteArrayInputStream(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10})));
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, struct.getARRAY());
  }

  @Test
  public void testWrite_DefaultBitOrder() throws IOException {
    final WholeStreamByteArray struct = new WholeStreamByteArray();
    struct.setARRAY(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final JBBPBitOutputStream bitOut = new JBBPBitOutputStream(out);
    struct.write(bitOut);
    bitOut.close();
    assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, out.toByteArray());
  }
}