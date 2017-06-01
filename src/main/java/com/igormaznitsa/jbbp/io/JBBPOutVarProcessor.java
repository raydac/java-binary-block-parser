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

package com.igormaznitsa.jbbp.io;

import java.io.IOException;

/**
 * The Interface describes a class which can write some custom values into the context output stream (DSL command Var()).
 *
 * @since 1.0
 */
public interface JBBPOutVarProcessor {
    /**
     * Process a DSL Var() command.
     *
     * @param context   the DSL context, must not be null
     * @param outStream the output stream for the context, must not be null
     * @param args      optional arguments, can be null
     * @return true is to continue processing of DSL commands, false skip all commands till the End()
     * @throws IOException it should be thrown for transport errors
     */
    boolean processVarOut(JBBPOut context, JBBPBitOutputStream outStream, Object... args) throws IOException;
}
