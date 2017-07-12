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

import com.igormaznitsa.jbbp.*;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.mapper.JBBPMapperCustomFieldProcessor;
import com.igormaznitsa.jbbp.model.*;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import static org.junit.Assert.*;
import org.junit.Test;
import com.igormaznitsa.jbbp.io.*;
import com.igormaznitsa.jbbp.utils.*;
import java.io.*;

/**
 * Test to parse RLE encoded snapshots in well-known Z80 format (v.1) for
 * ZX-Spectrum emulators.
 */
public class Z80_v1_ParsingTest extends AbstractParserIntegrationTest {

  class EmulFlags {
    @Bin(outOrder = 1, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2) byte interruptmode;
    @Bin(outOrder = 2, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1) byte issue2emulation;
    @Bin(outOrder = 3, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1) byte doubleintfreq;
    @Bin(outOrder = 4, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2) byte videosync;
    @Bin(outOrder = 5, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2) byte inputdevice;
  }

  class Flags {
    @Bin(outOrder = 1, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1) byte reg_r_bit7;
    @Bin(outOrder = 2, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_3) byte bordercolor;
    @Bin(outOrder = 3, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1) byte basic_samrom;
    @Bin(outOrder = 4, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1) byte compressed;
    @Bin(outOrder = 5, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2) byte nomeaning;
  }

  class Z80Snapshot {
    @Bin(outOrder = 1) byte reg_a;
    @Bin(outOrder = 2) byte reg_f;
    @Bin(outOrder = 3) short reg_bc;
    @Bin(outOrder = 4) short reg_hl;
    @Bin(outOrder = 5) short reg_pc;
    @Bin(outOrder = 6) short reg_sp;
    @Bin(outOrder = 7) byte reg_ir;
    @Bin(outOrder = 8) byte reg_r;

    @Bin(outOrder = 9) Flags flags;

    @Bin(outOrder = 10) short reg_de;
    @Bin(outOrder = 11) short reg_bc_alt;
    @Bin(outOrder = 12) short reg_de_alt;
    @Bin(outOrder = 13) short reg_hl_alt;
    @Bin(outOrder = 14) byte reg_a_alt;
    @Bin(outOrder = 15) byte reg_f_alt;
    @Bin(outOrder = 16) short reg_iy;
    @Bin(outOrder = 17) short reg_ix;
    @Bin(outOrder = 18) byte iff;
    @Bin(outOrder = 19) byte iff2;

    @Bin(outOrder = 20) EmulFlags emulFlags;

    @Bin(outOrder = 21, custom = true) byte[] data;
  }

  private static final JBBPParser z80Parser = JBBPParser.prepare(
          "byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
          + "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
          + "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
          + "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
          + "byte [_] data;"
  );

  private static class RLEDataEncoder implements JBBPOutVarProcessor {

    @Override
    public boolean processVarOut(final JBBPOut context, final JBBPBitOutputStream outStream, final Object... args) throws IOException {
      final byte[] unpackedData = (byte[]) args[1];
      if (((Number) args[0]).intValue() == 0) {
        context.Byte(unpackedData);
      }
      else {

        int value = -1;
        int counter = 0;

        for (final byte anUnpackedData : unpackedData) {
          final int cur = anUnpackedData & 0xFF;
          if (value < 0) {
            value = cur;
            counter = 1;
          } else {
            if (value == cur) {
              counter++;
              if (counter == 0xFF) {
                context.Byte(0xED, 0xED, counter, cur);
                value = -1;
                counter = 0;
              }
            } else if (counter >= 5 || (value == 0xED && counter > 1)) {
              context.Byte(0xED, 0xED, counter, value);
              counter = 1;
              value = cur;
            } else {
              while (counter != 0) {
                context.Byte(value);
                counter--;
              }
              if (value == 0xED) {
                context.Byte(cur);
                value = -1;
                counter = 0;
              } else {
                counter = 1;
                value = cur;
              }
            }
          }
        }

        if (counter < 5) {
          while (counter != 0) {
            context.Byte(value);
            counter--;
          }
        }
        else {
          context.Byte(0xED, 0xED, counter, value);
        }

        context.Byte(0x00, 0xED, 0xED, 0x00);
      }
      return true;
    }
  }

  @Test
  public void testRLEEncoding() throws Exception {
    assertArrayEquals(new byte[]{(byte) 0xED, (byte) 0xED, 1, 2, 3}, JBBPOut.BeginBin().Var(new RLEDataEncoder(), 0, new byte[]{(byte) 0xED, (byte) 0xED, 1, 2, 3}).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0xED, (byte) 0xED, 2, (byte) 0xED, 1, 2, 3, 0x00, (byte) 0xED, (byte) 0xED, 0x00}, JBBPOut.BeginBin().Var(new RLEDataEncoder(), 1, new byte[]{(byte) 0xED, (byte) 0xED, 1, 2, 3}).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0xED, 0x00, (byte) 0xED, (byte) 0xED, 0x05, 0x00, 0x00, (byte) 0xED, (byte) 0xED, 0x00}, JBBPOut.BeginBin().Var(new RLEDataEncoder(), 1, new byte[]{(byte) 0xED, 0, 0, 0, 0, 0, 0}).End().toByteArray());
    assertArrayEquals(new byte[]{(byte) 0xED, (byte) 0xED, 8, 5, 1, 2, 3, 0x00, (byte) 0xED, (byte) 0xED, 0x00}, JBBPOut.BeginBin().Var(new RLEDataEncoder(), 1, new byte[]{5, 5, 5, 5, 5, 5, 5, 5, 1, 2, 3}).End().toByteArray());
  }

  private static class DataProcessor implements JBBPMapperCustomFieldProcessor, JBBPCustomFieldWriter {

    @Override
    public Object prepareObjectForMapping(JBBPFieldStruct parsedBlock, Bin annotation, Field field) {
      if (field.getName().equals("data")) {
        final byte[] data = parsedBlock.findFieldForNameAndType("data", JBBPFieldArrayByte.class).getArray();

        if (parsedBlock.findFieldForPathAndType("flags.compressed", JBBPFieldBit.class).getAsBool()) {
          // RLE compressed
          final ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length << 1);
          int i = 0;

          // check end marker
          assertEquals(0x00, data[data.length - 1] & 0xFF);
          assertEquals(0xED, data[data.length - 2] & 0xFF);
          assertEquals(0xED, data[data.length - 3] & 0xFF);
          assertEquals(0x00, data[data.length - 4] & 0xFF);

          final int len = data.length - 4;

          while (i < len) {
            final int a = data[i++] & 0xFF;
            if (a == 0xED) {
              final int b = data[i++] & 0xFF;
              if (b == 0xED) {
                int num = data[i++] & 0xFF;
                final int val = data[i++] & 0xFF;
                while (num > 0) {
                  baos.write(val);
                  num--;
                }
              }
              else {
                baos.write(a);
                baos.write(b);
              }
            }
            else {
              baos.write(a);
            }
          }
          return baos.toByteArray();
        }
        else {
          // uncompressed
          return data;
        }
      }
      else {
        fail("Unexpected field");
        return null;
      }
    }

    @Override
    public void writeCustomField(final JBBPOut context, final JBBPBitOutputStream out, final Object instanceForSaving, final Field instanceCustomField, final Bin fieldAnnotation, final Object value) throws IOException {
      try {
        final byte [] array = (byte[])instanceCustomField.get(instanceForSaving);
        new RLEDataEncoder().processVarOut(context, out, 1, array);
      }
      catch (Exception ex) {
        ex.printStackTrace();
        fail("Can't get field data");
      }
    }
  }

  private Z80Snapshot assertParseAndPackBack(final String name, final long etalonLen) throws Exception {
    final Z80Snapshot z80sn;

    final InputStream resource = getResourceAsInputStream(name);
    try {
      z80sn = z80Parser.parse(resource).mapTo(Z80Snapshot.class, new DataProcessor());
      assertEquals(etalonLen, z80Parser.getFinalStreamByteCounter());
    }
    finally {
      JBBPUtils.closeQuietly(resource);
    }

    // form the same from parsed
    final JBBPOut out =  JBBPOut.BeginBin(JBBPByteOrder.LITTLE_ENDIAN, JBBPBitOrder.LSB0);
    assertEquals(0,out.getByteCounter());
    final byte[] packed = out.
            Byte(z80sn.reg_a, z80sn.reg_f).
            Short(z80sn.reg_bc, z80sn.reg_hl, z80sn.reg_pc, z80sn.reg_sp).
            Byte(z80sn.reg_ir, z80sn.reg_r).
            Bit(z80sn.flags.reg_r_bit7).
            Bits(JBBPBitNumber.BITS_3, z80sn.flags.bordercolor).
            Bit(z80sn.flags.basic_samrom, z80sn.flags.compressed).
            Bits(JBBPBitNumber.BITS_2, z80sn.flags.nomeaning).
            Short(z80sn.reg_de, z80sn.reg_bc_alt, z80sn.reg_de_alt, z80sn.reg_hl_alt).
            Byte(z80sn.reg_a_alt, z80sn.reg_f_alt).
            Short(z80sn.reg_iy, z80sn.reg_ix).
            Byte(z80sn.iff, z80sn.iff2).
            Bits(JBBPBitNumber.BITS_2, z80sn.emulFlags.interruptmode).
            Bit(z80sn.emulFlags.issue2emulation, z80sn.emulFlags.doubleintfreq).
            Bits(JBBPBitNumber.BITS_2, z80sn.emulFlags.videosync, z80sn.emulFlags.inputdevice).
            Var(new RLEDataEncoder(), z80sn.flags.compressed, z80sn.data).
            End().toByteArray();

    assertEquals(etalonLen, out.getByteCounter());
    
    assertResource(name, packed);
    return z80sn;
  }

  @Test
  public void testParseAndWriteTestZ80WithoutCheckOfFields() throws Exception {
    assertParseAndPackBack("test1.z80",16059);
    assertParseAndPackBack("test2.z80",29330);
    assertParseAndPackBack("test3.z80",5711);
    assertParseAndPackBack("test4.z80",9946);
  }  
  
  @Test
  public void testParseAndWriteTestZ80WithCheckOfFields() throws Exception {
    final Z80Snapshot z80sn = assertParseAndPackBack("test.z80",12429);

    final String text = new JBBPTextWriter().ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).SetMaxValuesPerLine(32).AddExtras(new JBBPTextWriterExtraAdapter() {

      @Override
      public String doConvertCustomField(JBBPTextWriter context, Object obj, Field field, Bin annotation) throws IOException {
        final byte [] data = (byte[])extractFieldValue(obj, field);
        return "byte array length ["+data.length+']';
      }

    }).Bin(z80sn).Close().toString();
    
    assertTrue(text.contains("byte array length [49152]"));
    System.out.println(text);
    
    assertEquals(0x7E, z80sn.reg_a & 0xFF);
    assertEquals(0x86, z80sn.reg_f & 0xFF);
    assertEquals(0x7A74, z80sn.reg_bc & 0xFFFF);
    assertEquals(0x7430, z80sn.reg_hl & 0xFFFF);

    assertEquals(12198, z80sn.reg_pc & 0xFFFF);
    assertEquals(65330, z80sn.reg_sp & 0xFFFF);

    assertEquals(0x3F, z80sn.reg_ir & 0xFF);
    assertEquals(0x1A, z80sn.reg_r & 0xFF);

    assertEquals(0, z80sn.flags.reg_r_bit7);
    assertEquals(2, z80sn.flags.bordercolor);
    assertEquals(0, z80sn.flags.basic_samrom);
    assertEquals(1, z80sn.flags.compressed);
    assertEquals(0, z80sn.flags.nomeaning);

    assertEquals(0x742B, z80sn.reg_de & 0xFFFF);
    assertEquals(0x67C6, z80sn.reg_bc_alt & 0xFFFF);
    assertEquals(0x3014, z80sn.reg_de_alt & 0xFFFF);
    assertEquals(0x3461, z80sn.reg_hl_alt & 0xFFFF);
    assertEquals(0x00, z80sn.reg_a_alt & 0xFF);
    assertEquals(0x46, z80sn.reg_f_alt & 0xFF);
    assertEquals(0x5C3A, z80sn.reg_iy & 0xFFFF);
    assertEquals(0x03D4, z80sn.reg_ix & 0xFFFF);
    assertEquals(0xFF, z80sn.iff & 0xFF);
    assertEquals(0xFF, z80sn.iff2 & 0xFF);

    assertEquals(1, z80sn.emulFlags.interruptmode);
    assertEquals(0, z80sn.emulFlags.issue2emulation);
    assertEquals(0, z80sn.emulFlags.doubleintfreq);
    assertEquals(0, z80sn.emulFlags.videosync);
    assertEquals(0, z80sn.emulFlags.inputdevice);

    assertEquals(49152, z80sn.data.length);
    int summ = 0;
    for (final byte b : z80sn.data) {
      summ += b & 0xFF;
    }
    assertTrue(summ > 0);
  }

  @Test
  public void testParseAndPackThrowMapping() throws Exception {
    final InputStream in = getResourceAsInputStream("test.z80");
    Z80Snapshot parsed = null;
    try{
      parsed = z80Parser.parse(in).mapTo(Z80Snapshot.class, new DataProcessor());
    }finally{
      JBBPUtils.closeQuietly(in);
    }
    
    final byte [] saved = JBBPOut.BeginBin(JBBPByteOrder.LITTLE_ENDIAN).Bin(parsed, new DataProcessor()).End().toByteArray();
   
    assertResource("test.z80", saved);
  }

}
