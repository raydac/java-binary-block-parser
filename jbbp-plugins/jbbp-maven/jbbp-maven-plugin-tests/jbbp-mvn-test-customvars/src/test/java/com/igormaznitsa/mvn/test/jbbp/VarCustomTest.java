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
package com.igormaznitsa.mvn.test.jbbp;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import com.igormaznitsa.mvn.tst.VarCustomImpl;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;

public class VarCustomTest {

    private static final Random RND = new Random(12345);

    @Test
    public void testReadWrite() throws Exception {
        final VarCustomImpl impl = new VarCustomImpl();

        final byte[] etalonArray = new byte[319040];
        RND.nextBytes(etalonArray);
        impl.read(new JBBPBitInputStream(new ByteArrayInputStream(etalonArray)));

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final JBBPBitOutputStream bios = new JBBPBitOutputStream(bos);
        impl.write(bios);
        bios.close();

        assertArrayEquals(etalonArray, bos.toByteArray());
    }
}
