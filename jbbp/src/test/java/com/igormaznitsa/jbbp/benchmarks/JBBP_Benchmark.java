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
package com.igormaznitsa.jbbp.benchmarks;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.utils.TargetSources;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;


/**
 * Test set to check productivity of different work modes with parsing data of the same data set.
 */
public class JBBP_Benchmark {

    private static final JBBPParser parser = JBBPParser.prepare("ubyte val; data [(val>>1)*(val+3)]{ bit:3 a; bit:3 b; bit:2 c; skip:1; }");

    private static final Random RND = new Random(12345);

    private static final byte[] DATA;

    static {
        final int val = 201;
        DATA = new byte[1 + ((val >> 1) * (val + 3)) * 2];
        RND.nextBytes(DATA);
        DATA[0] = (byte) val;
    }

    public static class InData {
        @Bin(name = "a", type = BinType.BIT)
        public byte a;
        @Bin(name = "b", type = BinType.BIT)
        public byte b;
        @Bin(name = "c", type = BinType.BIT)
        public byte c;
    }

    public static class Data {
        @Bin(name = "val", type = BinType.UBYTE)
        public int val;

        @Bin(name = "data")
        public InData [] data;
    }

    public static void main(String... args) {
        System.out.println("-------------");
        System.out.println(parser.convertToSrc(TargetSources.JAVA_1_6, "com.igormaznitsa.jbbp.benchmarks.JBBP_Benchmark_Parser").get(0).getResult().values().iterator().next());
        System.out.println("-------------");
    }


    @Benchmark
    public void measureParse_DynamicAndMapping() throws IOException {
        parser.parse(DATA).mapTo(Data.class);
    }

    @Benchmark
    public void measureParse_Dynamic() throws IOException {
        parser.parse(DATA);
    }

    @Benchmark
    public void measureParse_Static() throws IOException {
        new JBBP_Benchmark_Parser().read(new JBBPBitInputStream(new ByteArrayInputStream(DATA)));
    }
}
