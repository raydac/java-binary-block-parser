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
package com.igormaznitsa.jbbp.compiler.conversion;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.compiler.varlen.JBBPIntegerValueEvaluator;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.utils.JavaSrcTextBuffer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.igormaznitsa.jbbp.compiler.JBBPCompiler.*;

/**
 * Converter to produce Java class sources (1.6+) from JBBPParser. If a parser contains variable field, custom fields or external
 * values in expressions then the result class will be abstract one and its
 * abstract methods must be implemented in successor.
 *
 * @since 1.3.0
 */
public final class JBBPToJava6Converter extends CompiledBlockVisitor {

    private static final int FLAG_DETECTED_CUSTOM_FIELDS = 1;
    private static final int FLAG_DETECTED_EXTERNAL_FIELDS = 2;
    private static final int FLAG_DETECTED_VAR_FIELDS = 4;

    /**
     * Name of the field to be used as link to the root structure instance in
     * child structures.
     */
    private static final String NAME_ROOT_STRUCT = "_Root_";
    /**
     * Name of the field to keep information about parser flags.
     */
    private static final String NAME_PARSER_FLAGS = "_ParserFlags_";
    /**
     * Name of the input stream argument.
     */
    private static final String NAME_INPUT_STREAM = "In";
    /**
     * Name of the output stream argument.
     */
    private static final String NAME_OUTPUT_STREAM = "Out";
    /**
     * Detected flags.
     */
    private final AtomicInteger flagSet = new AtomicInteger();

    /**
     * Map of detected named fields to their name field info object.
     */
    private final Map<JBBPNamedFieldInfo, NamedFieldInfo> foundNamedFields = new HashMap<JBBPNamedFieldInfo, NamedFieldInfo>();
    /**
     * Counter of anonymous fields to generate unique names.
     */
    private final AtomicInteger anonymousFieldCounter = new AtomicInteger();
    /**
     * Counter of special fields to generate their unique names.
     */
    private final AtomicInteger specialFieldsCounter = new AtomicInteger();
    /**
     * The List implements stack of current processing structures. The 0 contains
     * the root.
     */
    private final List<Struct> structStack = new ArrayList<Struct>();
    /**
     * Text buffer for the special section.
     */
    private final JavaSrcTextBuffer specialSection = new JavaSrcTextBuffer();
    /**
     * Text buffer for the special methods.
     */
    private final JavaSrcTextBuffer specialMethods = new JavaSrcTextBuffer();
    /**
     * The Builder instance to be used as the data source for the parser. It must
     * not be null.
     */
    private final Builder builder;
    /**
     * The Field contains conversion result after process end.
     */
    private String result;

    /**
     * The Only Constructor based on a builder instance.
     *
     * @param builder a builder instance, must not be null
     */
    private JBBPToJava6Converter(final Builder builder) {
        super(builder.parserFlags, builder.parser.getCompiledBlock());
        this.builder = builder;
    }

    /**
     * Make new builder.
     *
     * @param parser parser to be used as the base for translation, must not be
     *               null
     * @return the new builder instance, must not be null.
     */
    public static Builder makeBuilder(final JBBPParser parser) {
        return new Builder(parser);
    }

    /**
     * Do conversion.
     *
     * @return generated class with needed parameters as text, must not be null.
     */
    public String convert() {
        return JBBPToJava6Converter.class.cast(this.visit()).getResult();
    }

    private NamedFieldInfo registerNamedField(final JBBPNamedFieldInfo fieldInfo, final FieldType fieldType) {
        NamedFieldInfo resultInfo = null;
        if (fieldInfo != null) {
            if (this.foundNamedFields.containsKey(fieldInfo)) {
                throw new Error("Detected duplication of named field : " + fieldInfo);
            }
            resultInfo = new NamedFieldInfo(fieldInfo, this.getCurrentStruct(), fieldType);
            this.foundNamedFields.put(fieldInfo, resultInfo);
        }
        return resultInfo;
    }

    private Struct getCurrentStruct() {
        return this.structStack.get(0);
    }

    @Override
    public void visitStart() {
        this.flagSet.set(0);
        this.foundNamedFields.clear();
        this.anonymousFieldCounter.set(1234);
        this.specialFieldsCounter.set(1);
        this.specialSection.clean();
        this.structStack.clear();
        this.specialMethods.clean();

        this.structStack.add(new Struct(null, this.builder.className, "public"));
    }

    /**
     * Get result of the conversion process.
     *
     * @return the result, it will not be null if the process completed without
     * errors.
     */
    public String getResult() {
        return this.result;
    }

    @Override
    public void visitEnd() {
        final JavaSrcTextBuffer buffer = new JavaSrcTextBuffer();

        if (this.builder.classHeadComment != null) {
            buffer.printCommentMultiLinesWithIndent(this.builder.classHeadComment);
        }

        if (this.builder.classPackage != null && this.builder.classPackage.length() != 0) {
            buffer.print("package ").print(this.builder.classPackage).println(";");
        }

        buffer.println();

        buffer.println("import com.igormaznitsa.jbbp.model.*;");
        buffer.println("import com.igormaznitsa.jbbp.io.*;");
        buffer.println("import com.igormaznitsa.jbbp.compiler.*;");
        buffer.println("import com.igormaznitsa.jbbp.compiler.tokenizer.*;");
        buffer.println("import java.io.IOException;");
        buffer.println("import java.util.*;");

        buffer.println();

        this.specialSection.println();
        this.specialSection.printJavaDocLinesWithIndent("The Constant contains parser flags\n@see JBBPParser#FLAG_SKIP_REMAINING_FIELDS_IF_EOF");
        this.specialSection.indent().printf("protected static final int %s = %d;", NAME_PARSER_FLAGS, this.parserFlags);

        final int detected = this.flagSet.get();

        if ((detected & FLAG_DETECTED_CUSTOM_FIELDS) != 0) {
            this.specialMethods.printJavaDocLinesWithIndent("Reading of custom fields\n@param sourceStruct source structure holding the field, must not be null\n@param inStream the input stream, must not be null\n@param typeParameterContainer info about field type, must not be null\n@param nullableNamedFieldInfo info abut field name, it can be null\n@param extraValue value from extra field part, default value 0\n@param readWholeStream flag to read the stream as array till the stream end if true\n@param arraySize if array then it is zero or great\n@exception IOException if data can't be read\n@return read value as abstract field, must not be null");
            this.specialMethods.println("public abstract JBBPAbstractField readCustomFieldType(Object sourceStruct, JBBPBitInputStream inStream, JBBPFieldTypeParameterContainer typeParameterContainer, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue, boolean readWholeStream, int arraySize) throws IOException;");
            this.specialMethods.println();
            this.specialMethods.printJavaDocLinesWithIndent("Writing custom fields\n@param sourceStruct source structure holding the field, must not be null\n@param outStream the output stream, must not be null\n@param fieldValue value to be written\n@param typeParameterContainer info about field type, must not be null\n@param nullableNamedFieldInfo info abut field name, it can be null\n@param extraValue value from extra field part, default value is 0\n@param wholeArray true if to write whole array\n@param arraySize if array then it is zero or great\n@exception IOException if data can't be written");
            this.specialMethods.println("public abstract void writeCustomFieldType(Object sourceStruct, JBBPBitOutputStream outStream, JBBPAbstractField fieldValue, JBBPFieldTypeParameterContainer typeParameterContainer, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue, boolean wholeArray, int arraySize) throws IOException;");
        }

        if ((detected & FLAG_DETECTED_EXTERNAL_FIELDS) != 0) {
            if (!this.specialMethods.isEmpty()) {
                this.specialMethods.println();
            }
            this.specialMethods.printJavaDocLinesWithIndent("Method is called from expressions to provide value\n@param sourceStruct source structure holding the field, must not be null\n@param valueName name of value to be provided, must not be null\n@return integer value for the named parameter");
            this.specialMethods.println("public abstract int getNamedValue(Object sourceStruct, String valueName);");
        }

        if ((detected & FLAG_DETECTED_VAR_FIELDS) != 0) {
            if (!this.specialMethods.isEmpty()) {
                this.specialMethods.println();
            }
            this.specialMethods.printJavaDocLinesWithIndent("Read variable field\n@param sourceStruct source structure holding the field, must not be null\n@param inStream the input stream, must not be null\n@param byteOrder\n@param nullableNamedFieldInfo info abut field name, it can be null\n@param extraValue value from extra field part, -1 if not defined\n@return\n@exception IOException");
            this.specialMethods.println("public abstract JBBPAbstractField readVarField(Object sourceStruct, JBBPBitInputStream inStream, JBBPByteOrder byteOrder, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue) throws IOException;");
            this.specialMethods.println();
            this.specialMethods.printJavaDocLinesWithIndent("Read variable array field\n@param sourceStruct source structure holding the field, must not be null\n@param inStream the input stream, must not be null\n@param byteOrder byte order to be used for reading, must not be null\n@param nullableNamedFieldInfo info abut field name, it can be null\n@param extraValue value from extra field part, -1 if not defined\n@param readWholeStream if true then whole stream should be read\n@param arraySize size of array to read (if whole stream flag is false)\n@return array object contains read data, must not be null\n@exception IOException if error during data reading");
            this.specialMethods.println("public abstract JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(Object sourceStruct, JBBPBitInputStream inStream, JBBPByteOrder byteOrder, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue, boolean readWholeStream, int arraySize) throws IOException;");
            this.specialMethods.println();
            this.specialMethods.printJavaDocLinesWithIndent("Read variable field\n@param sourceStruct source structure holding the field, must not be null\n@param value field value, must not be null\n@param outStream the output stream, must not be null,\n@param byteOrder byte order to be used for reading, must not be null\n@param nullableNamedFieldInfo info abut field name, it can be null\n@param extraValue value from extra field part, -1 if not defined\n@exception IOException  it is thrown if any transport error during operation");
            this.specialMethods.println("public abstract void writeVarField(Object sourceStruct, JBBPAbstractField value, JBBPBitOutputStream outStream, JBBPByteOrder byteOrder, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue) throws IOException;");
            this.specialMethods.println();
            this.specialMethods.printJavaDocLinesWithIndent("Write variable array\n@param sourceStruct source structure holding the field, must not be null\n@param array array value to be written, must not be null\n@param outStream the output stream, must not be null\n@param byteOrder byte order to be used for reading, must not be null\n@param nullableNamedFieldInfo info abut field name, it can be null\n@param extraValue value from extra field part, -1 if not defined\n@param arraySizeToWrite\n@exception IOException it is thrown if any transport error during operation");
            this.specialMethods.println("public abstract void writeVarArray(Object sourceStruct, JBBPAbstractArrayField<? extends JBBPAbstractField> array, JBBPBitOutputStream outStream, JBBPByteOrder byteOrder, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue, int arraySizeToWrite) throws IOException;");
        }

        final String specialMethodsText = this.specialMethods.toString();

        final boolean hasAbstractMethods = (this.flagSet.get() & (FLAG_DETECTED_CUSTOM_FIELDS | FLAG_DETECTED_VAR_FIELDS | FLAG_DETECTED_EXTERNAL_FIELDS)) != 0 || this.builder.forceAbstract;

        buffer.printJavaDocLinesWithIndent("Generated from JBBP script by internal JBBP Class Source Generator");

        this.structStack.get(0).write(buffer,
                hasAbstractMethods ? "abstract" : null,
                this.builder.extendsClass,
                this.builder.interfaces,
                this.specialSection.toString(),
                specialMethodsText.length() == 0 ? null : specialMethodsText,
                this.builder.customText
        );

        this.result = buffer.toString();
    }

    @Override
    public void visitStructureStart(final int offsetInCompiledBlock, final JBBPNamedFieldInfo nullableNameFieldInfo, final JBBPIntegerValueEvaluator nullableArraySize) {
        final String structName = (nullableNameFieldInfo == null ? makeAnonymousStructName() : nullableNameFieldInfo.getFieldName()).toLowerCase(Locale.ENGLISH);
        final String structBaseTypeName = structName.toUpperCase(Locale.ENGLISH);
        final String arraySizeIn = nullableArraySize == null ? null : evaluatorToString(NAME_INPUT_STREAM, offsetInCompiledBlock, nullableArraySize, this.flagSet);
        final String arraySizeOut = nullableArraySize == null ? null : evaluatorToString(NAME_OUTPUT_STREAM, offsetInCompiledBlock, nullableArraySize, this.flagSet);
        final Struct newStruct = new Struct(this.getCurrentStruct(), structBaseTypeName, "public static");

        final String fieldModifier = makeModifier(nullableNameFieldInfo);

        final String structType;
        if (nullableArraySize == null) {
            structType = structBaseTypeName;
            this.getCurrentStruct().getFields().indent().print(fieldModifier).printf(" %s %s;", structType, structName).println();
            processSkipRemainingFlag();
            this.getCurrentStruct().getReadFunc().indent()
                    .printf("if ( this.%1$s == null) { this.%1$s = new %2$s(%3$s);}", structName, structType, this.structStack.size() == 1 ? "this" : "this." + NAME_ROOT_STRUCT)
                    .printf(" this.%s.read(%s);%n", structName, NAME_INPUT_STREAM);
            this.getCurrentStruct().getWriteFunc().indent().print(structName).println(".write(Out);");
        } else {
            structType = structBaseTypeName + " []";
            this.getCurrentStruct().getFields().indent().print(fieldModifier).printf(" %s %s;", structType, structName).println();
            processSkipRemainingFlag();
            if ("-1".equals(arraySizeIn)) {
                this.getCurrentStruct().getReadFunc().indent()
                        .printf("List<%3$s> __%1$s_tmplst__ = new ArrayList<%3$s>(); while (%5$s.hasAvailableData()){ __%1$s_tmplst__.add(new %3$s(%4$s).read(%5$s));} this.%1$s = __%1$s_tmplst__.toArray(new %3$s[__%1$s_tmplst__.size()]);__%1$s_tmplst__ = null;%n", structName, arraySizeIn, structBaseTypeName, (this.structStack.size() == 1 ? "this" : NAME_ROOT_STRUCT), NAME_INPUT_STREAM);
                this.getCurrentStruct().getWriteFunc().indent().printf("for (int I=0;I<this.%1$s.length;I++){ this.%1$s[I].write(%2$s); }%n", structName, NAME_OUTPUT_STREAM);
            } else {
                this.getCurrentStruct().getReadFunc().indent()
                        .printf("if (this.%1$s == null || this.%1$s.length != %2$s){ this.%1$s = new %3$s[%2$s]; for(int I=0;I<%2$s;I++){ this.%1$s[I] = new %3$s(%4$s);}}", structName, arraySizeIn, structBaseTypeName, (this.structStack.size() == 1 ? "this" : "this." + NAME_ROOT_STRUCT))
                        .printf("for (int I=0;I<%2$s;I++){ this.%1$s[I].read(%3$s); }%n", structName, arraySizeIn, NAME_INPUT_STREAM);
                this.getCurrentStruct().getWriteFunc().indent().printf("for (int I=0;I<%2$s;I++){ this.%1$s[I].write(%3$s); }", structName, arraySizeOut, NAME_OUTPUT_STREAM);
            }
        }

        if (nullableNameFieldInfo != null && this.builder.doGettersSetters) {
            registerGetterSetter(structType, structName, false);
        }

        this.structStack.add(0, newStruct);
    }

    private void processSkipRemainingFlag() {
        if (this.isFlagSkipRemainingFieldsIfEOF()) {
            this.getCurrentStruct().getReadFunc().indent().println(String.format("if (!%s.hasAvailableData()) return this;", NAME_INPUT_STREAM));
        }
    }

    @Override
    public void visitStructureEnd(final int offsetInCompiledBlock, final JBBPNamedFieldInfo nullableNameFieldInfo) {
        this.structStack.remove(0);
    }

    @Override
    public void visitPrimitiveField(final int offsetInCompiledBlock, final int primitiveType, final JBBPNamedFieldInfo nullableNameFieldInfo, final JBBPByteOrder byteOrder, final boolean readWholeStreamAsArray, final JBBPIntegerValueEvaluator nullableArraySize) {
        final String fieldName = nullableNameFieldInfo == null ? makeAnonymousFieldName() : nullableNameFieldInfo.getFieldName();
        final FieldType type = FieldType.findForCode(primitiveType);

        registerNamedField(nullableNameFieldInfo, type);

        final String arraySizeIn = nullableArraySize == null ? null : evaluatorToString(NAME_INPUT_STREAM, offsetInCompiledBlock, nullableArraySize, this.flagSet);
        final String arraySizeOut = nullableArraySize == null ? null : evaluatorToString(NAME_OUTPUT_STREAM, offsetInCompiledBlock, nullableArraySize, this.flagSet);

        final String fieldModifier = makeModifier(nullableNameFieldInfo);
        processSkipRemainingFlag();

        final String textFieldType;

        if (nullableArraySize == null) {
            textFieldType = type.asJavaSingleFieldType();
            getCurrentStruct().getFields().printf("%s %s %s;%n", fieldModifier, textFieldType, fieldName);
            getCurrentStruct().getReadFunc().println(String.format("this.%s = %s;", fieldName, type.makeReaderForSingleField(NAME_INPUT_STREAM, byteOrder)));
            getCurrentStruct().getWriteFunc().print(type.makeWriterForSingleField(NAME_OUTPUT_STREAM, "this." + fieldName, byteOrder)).println(";");
        } else {
            textFieldType = type.asJavaArrayFieldType() + " []";
            getCurrentStruct().getFields().printf("%s %s %s;%n", fieldModifier, textFieldType, fieldName);
            getCurrentStruct().getReadFunc().printf("this.%s = %s;%n", fieldName, type.makeReaderForArray(NAME_INPUT_STREAM, arraySizeIn, byteOrder));
            if (readWholeStreamAsArray) {
                getCurrentStruct().getWriteFunc().print(type.makeWriterForArrayWithUnknownSize(NAME_OUTPUT_STREAM, "this." + fieldName, byteOrder)).println(";");
            } else {
                getCurrentStruct().getWriteFunc().print(type.makeWriterForArray(NAME_OUTPUT_STREAM, "this." + fieldName, arraySizeOut, byteOrder)).println(";");
            }
        }

        if (nullableNameFieldInfo != null && this.builder.doGettersSetters) {
            registerGetterSetter(textFieldType, fieldName, true);
        }
    }

    private void registerGetterSetter(final String fieldType, final String fieldName, final boolean makeSetter) {
        if (!this.getCurrentStruct().getGettersSetters().isEmpty()) {
            this.getCurrentStruct().getGettersSetters().println();
        }

        if (makeSetter) {
            this.getCurrentStruct().getGettersSetters().indent().printf("public void set%s(%s value) { this.%s = value;}%n", fieldName.toUpperCase(Locale.ENGLISH), fieldType, fieldName);
        }

        this.getCurrentStruct().getGettersSetters().indent().printf("public %s get%s() { return this.%s;}%n", fieldType, fieldName.toUpperCase(Locale.ENGLISH), fieldName);
    }

    @Override
    public void visitBitField(final int offsetInCompiledBlock, final JBBPNamedFieldInfo nullableNameFieldInfo, final JBBPIntegerValueEvaluator notNullFieldSize, final JBBPIntegerValueEvaluator nullableArraySize) {
        final String fieldName = nullableNameFieldInfo == null ? makeAnonymousFieldName() : nullableNameFieldInfo.getFieldName();

        registerNamedField(nullableNameFieldInfo, FieldType.BIT);

        String sizeOfFieldIn = evaluatorToString(NAME_INPUT_STREAM, offsetInCompiledBlock, notNullFieldSize, this.flagSet);
        String sizeOfFieldOut = evaluatorToString(NAME_OUTPUT_STREAM, offsetInCompiledBlock, notNullFieldSize, this.flagSet);
        try {
            sizeOfFieldIn = "JBBPBitNumber." + JBBPBitNumber.decode(Integer.parseInt(sizeOfFieldIn)).name();
        } catch (NumberFormatException ex) {
            sizeOfFieldIn = "JBBPBitNumber.decode(" + sizeOfFieldIn + ')';
        }

        try {
            sizeOfFieldOut = "JBBPBitNumber." + JBBPBitNumber.decode(Integer.parseInt(sizeOfFieldOut)).name();
        } catch (NumberFormatException ex) {
            sizeOfFieldOut = "JBBPBitNumber.decode(" + sizeOfFieldOut + ')';
        }

        final String arraySizeIn = nullableArraySize == null ? null : evaluatorToString(NAME_INPUT_STREAM, offsetInCompiledBlock, nullableArraySize, this.flagSet);
        final String arraySizeOut = nullableArraySize == null ? null : evaluatorToString(NAME_OUTPUT_STREAM, offsetInCompiledBlock, nullableArraySize, this.flagSet);

        final String fieldModifier = makeModifier(nullableNameFieldInfo);

        processSkipRemainingFlag();

        if (arraySizeIn == null) {
            getCurrentStruct().getReadFunc().indent().printf("this.%s = In.readBitField(%s);%n", fieldName, sizeOfFieldIn);
        } else {
            getCurrentStruct().getReadFunc().indent().print(fieldName).print(" = In.readBitsArray(").print(arraySizeIn).print(",").print(sizeOfFieldIn).println(");");
        }

        if (arraySizeOut == null) {
            getCurrentStruct().getWriteFunc().indent().printf("%s.writeBits(this.%s,%s);%n", NAME_OUTPUT_STREAM, fieldName, sizeOfFieldOut);
        } else {
            if ("-1".equals(arraySizeIn)) {
                getCurrentStruct().getWriteFunc().indent().printf("for(int I=0; I<%s.length; I++)", fieldName).printf(" Out.writeBits(this.%s[I],%s);%n", fieldName, sizeOfFieldOut);
            } else {
                getCurrentStruct().getWriteFunc().indent().printf("for(int I=0; I<%s; I++)", arraySizeOut).printf(" Out.writeBits(this.%s[I],%s);%n", fieldName, sizeOfFieldOut);
            }
        }

        final String fieldType = nullableArraySize == null ? "byte" : "byte []";
        getCurrentStruct().getFields().indent().printf("%s %s %s;%n", fieldModifier, fieldType, fieldName);

        if (nullableNameFieldInfo != null && this.builder.doGettersSetters) {
            registerGetterSetter(fieldType, fieldName, true);
        }
    }

    private String makeAnonymousFieldName() {
        return "_AField" + this.anonymousFieldCounter.getAndIncrement();
    }

    private String makeSpecialFieldName() {
        return "_SField" + this.specialFieldsCounter.getAndIncrement();
    }

    private String makeAnonymousStructName() {
        return "_AStruct" + this.anonymousFieldCounter.getAndIncrement();
    }

    private String makeModifier(final JBBPNamedFieldInfo nullableNameFieldInfo) {
        if (this.builder.doGettersSetters) return "private";
        return nullableNameFieldInfo == null ? "protected" : "public";
    }

    @Override
    public void visitCustomField(final int offsetInCompiledBlock, final JBBPFieldTypeParameterContainer notNullfieldType, final JBBPNamedFieldInfo nullableNameFieldInfo, final JBBPByteOrder byteOrder, final boolean readWholeStream, final JBBPIntegerValueEvaluator nullableArraySizeEvaluator, final JBBPIntegerValueEvaluator extraDataValueEvaluator) {
        this.flagSet.set(this.flagSet.get() | FLAG_DETECTED_CUSTOM_FIELDS);

        registerNamedField(nullableNameFieldInfo, FieldType.CUSTOM);

        final String fieldName = nullableNameFieldInfo == null ? makeAnonymousFieldName() : nullableNameFieldInfo.getFieldName();
        final String fieldModifier = makeModifier(nullableNameFieldInfo);

        final String specialFieldName = makeSpecialFieldName();
        final String specialFieldName_fieldNameInfo = specialFieldName + "FieldInfo";
        final String specialFieldName_typeParameterContainer = specialFieldName + "TypeParameter";

        this.getCurrentStruct().getFields().printf("%s JBBPAbstractField %s;%n", fieldModifier, fieldName);

        if (nullableNameFieldInfo != null) {
            this.specialSection.printf("private static final JBBPNamedFieldInfo %s = %s;%n",
                    specialFieldName_fieldNameInfo,
                    "new JBBPNamedFieldInfo(\"" + nullableNameFieldInfo.getFieldName() + "\",\"" + nullableNameFieldInfo.getFieldPath() + "\"," + nullableNameFieldInfo.getFieldOffsetInCompiledBlock() + ")"
            );
        }

        this.specialSection.printf("private static final JBBPFieldTypeParameterContainer %s = %s;%n",
                specialFieldName_typeParameterContainer,
                "new JBBPFieldTypeParameterContainer(JBBPByteOrder." + notNullfieldType.getByteOrder().name() + ",\"" + notNullfieldType.getTypeName() + "\"," + (notNullfieldType.getExtraData() == null ? "null" : "\"" + notNullfieldType.getExtraData() + "\"") + ")"
        );

        processSkipRemainingFlag();
        this.getCurrentStruct().getReadFunc().printf("%s = %s;%n",
                fieldName,
                String.format("%s.readCustomFieldType(this, In, %s, %s, %s, %b, %s)",
                        this.getCurrentStruct().isRoot() ? "this" : "this." + NAME_ROOT_STRUCT,
                        specialFieldName_typeParameterContainer,
                        nullableNameFieldInfo == null ? "null" : specialFieldName_fieldNameInfo,
                        extraDataValueEvaluator == null ? "0" : evaluatorToString(NAME_INPUT_STREAM, offsetInCompiledBlock, extraDataValueEvaluator, this.flagSet),
                        readWholeStream,
                        nullableArraySizeEvaluator == null ? "-1" : evaluatorToString(NAME_INPUT_STREAM, offsetInCompiledBlock, nullableArraySizeEvaluator, this.flagSet)
                )
        );

        this.getCurrentStruct().getWriteFunc().printf("%s;%n",
                String.format("%s.writeCustomFieldType(this, Out, %s, %s, %s, %s, %b, %s)",
                        this.getCurrentStruct().isRoot() ? "this" : "this." + NAME_ROOT_STRUCT,
                        "this." + fieldName,
                        specialFieldName_typeParameterContainer,
                        nullableNameFieldInfo == null ? "null" : specialFieldName_fieldNameInfo,
                        extraDataValueEvaluator == null ? "0" : evaluatorToString(NAME_OUTPUT_STREAM, offsetInCompiledBlock, extraDataValueEvaluator, this.flagSet),
                        readWholeStream,
                        nullableArraySizeEvaluator == null ? "-1" : evaluatorToString(NAME_OUTPUT_STREAM, offsetInCompiledBlock, nullableArraySizeEvaluator, this.flagSet)
                )
        );

        if (nullableNameFieldInfo != null && this.builder.doGettersSetters) {
            registerGetterSetter("JBBPAbstractField", fieldName, true);
        }
    }

    @Override
    public void visitVarField(final int offsetInCompiledBlock, final JBBPNamedFieldInfo nullableNameFieldInfo, final JBBPByteOrder byteOrder, final boolean readWholeStreamIntoArray, final JBBPIntegerValueEvaluator nullableArraySizeEvaluator, final JBBPIntegerValueEvaluator extraDataValueEvaluator) {
        this.flagSet.set(this.flagSet.get() | FLAG_DETECTED_VAR_FIELDS);

        registerNamedField(nullableNameFieldInfo, FieldType.VAR);

        final String fieldName = nullableNameFieldInfo == null ? makeAnonymousFieldName() : nullableNameFieldInfo.getFieldName();
        final String fieldModifier = makeModifier(nullableNameFieldInfo);

        final String specialFieldName = makeSpecialFieldName();
        final String specialFieldName_fieldNameInfo = specialFieldName + "FieldInfo";

        if (nullableNameFieldInfo != null) {
            this.specialSection.printf("private static final JBBPNamedFieldInfo %s = %s;%n",
                    specialFieldName_fieldNameInfo,
                    "new JBBPNamedFieldInfo(\"" + nullableNameFieldInfo.getFieldName() + "\",\"" + nullableNameFieldInfo.getFieldPath() + "\"," + nullableNameFieldInfo.getFieldOffsetInCompiledBlock() + ")"
            );
        }

        processSkipRemainingFlag();
        final String fieldType;
        if (readWholeStreamIntoArray || nullableArraySizeEvaluator != null) {
            fieldType = "JBBPAbstractArrayField<? extends JBBPAbstractField>";
            this.getCurrentStruct().getFields().printf("%s %s %s;%n", fieldModifier, fieldType, fieldName);

            this.getCurrentStruct().getReadFunc().printf("%s = %s;%n",
                    fieldName,
                    String.format("%s.readVarArray(this, In, %s, %s, %s, %b, %s)",
                            this.getCurrentStruct().isRoot() ? "this" : "this." + NAME_ROOT_STRUCT,
                            "JBBPByteOrder." + byteOrder.name(),
                            nullableNameFieldInfo == null ? "null" : specialFieldName_fieldNameInfo,
                            extraDataValueEvaluator == null ? "-1" : evaluatorToString(NAME_INPUT_STREAM, offsetInCompiledBlock, extraDataValueEvaluator, this.flagSet),
                            readWholeStreamIntoArray,
                            nullableArraySizeEvaluator == null ? "-1" : evaluatorToString(NAME_INPUT_STREAM, offsetInCompiledBlock, nullableArraySizeEvaluator, this.flagSet)
                    )
            );

            this.getCurrentStruct().getWriteFunc().printf("%s.writeVarArray(this, this.%s, Out, %s, %s, %s, %s);%n",
                    this.getCurrentStruct().isRoot() ? "this" : "this." + NAME_ROOT_STRUCT,
                    fieldName,
                    "JBBPByteOrder." + byteOrder.name(),
                    nullableNameFieldInfo == null ? "null" : specialFieldName_fieldNameInfo,
                    extraDataValueEvaluator == null ? "-1" : evaluatorToString(NAME_INPUT_STREAM, offsetInCompiledBlock, extraDataValueEvaluator, this.flagSet),
                    nullableArraySizeEvaluator == null ? "-1" : evaluatorToString(NAME_OUTPUT_STREAM, offsetInCompiledBlock, nullableArraySizeEvaluator, this.flagSet)
            );

        } else {
            fieldType = "JBBPAbstractField";
            this.getCurrentStruct().getFields().printf("%s %s %s;%n", fieldModifier, fieldType, fieldName);

            this.getCurrentStruct().getReadFunc().printf("%s = %s;%n",
                    fieldName,
                    String.format("%s.readVarField(this, In, %s, %s, %s)",
                            this.getCurrentStruct().isRoot() ? "this" : "this." + NAME_ROOT_STRUCT,
                            "JBBPByteOrder." + byteOrder.name(),
                            nullableNameFieldInfo == null ? "null" : specialFieldName_fieldNameInfo,
                            extraDataValueEvaluator == null ? "-1" : evaluatorToString(NAME_INPUT_STREAM, offsetInCompiledBlock, extraDataValueEvaluator, this.flagSet))
            );

            this.getCurrentStruct().getWriteFunc().printf("%s.writeVarField(this, this.%s, Out, %s, %s, %s);%n",
                    this.getCurrentStruct().isRoot() ? "this" : "this." + NAME_ROOT_STRUCT,
                    fieldName,
                    "JBBPByteOrder." + byteOrder.name(),
                    nullableNameFieldInfo == null ? "null" : specialFieldName_fieldNameInfo,
                    extraDataValueEvaluator == null ? "-1" : evaluatorToString(NAME_OUTPUT_STREAM, offsetInCompiledBlock, extraDataValueEvaluator, this.flagSet)
            );
        }

        if (nullableNameFieldInfo != null && this.builder.doGettersSetters) {
            registerGetterSetter(fieldType, fieldName, true);
        }
    }

    /**
     * Convert an evaluator into string representation
     *
     * @param streamName       name of the stream in the case, must not be null
     * @param offsetInBlock    offset of the data in the compiled block
     * @param evaluator        the evaluator to be converted, must not be null
     * @param detectedFlagsSet container of detected flags, must not be null
     * @return the evaluator string representation, must not be null
     */
    private String evaluatorToString(final String streamName, final int offsetInBlock, final JBBPIntegerValueEvaluator evaluator, final AtomicInteger detectedFlagsSet) {
        final StringBuilder buffer = new StringBuilder();

        final ExpressionEvaluatorVisitor visitor = new ExpressionEvaluatorVisitor() {
            private final List<Object> stack = new ArrayList<Object>();

            @Override
            public ExpressionEvaluatorVisitor visitStart() {
                this.stack.clear();
                return this;
            }

            @Override
            public ExpressionEvaluatorVisitor visitSpecial(final Special specialField) {
                this.stack.add(specialField);
                return this;
            }

            @Override
            public ExpressionEvaluatorVisitor visitField(final JBBPNamedFieldInfo nullableNameFieldInfo, final String nullableExternalFieldName) {
                if (nullableNameFieldInfo != null) {
                    this.stack.add(nullableNameFieldInfo);
                } else if (nullableExternalFieldName != null) {
                    detectedFlagsSet.set(detectedFlagsSet.get() | FLAG_DETECTED_EXTERNAL_FIELDS);
                    this.stack.add(nullableExternalFieldName);
                }
                return this;
            }

            @Override
            public ExpressionEvaluatorVisitor visitOperator(final Operator operator) {
                this.stack.add(operator);
                return this;
            }

            @Override
            public ExpressionEvaluatorVisitor visitConstant(final int value) {
                this.stack.add(value);
                return this;
            }

            private String arg2str(final Object obj) {
                if (obj instanceof ExprTreeItem) {
                    return obj.toString();
                } else if (obj instanceof Special) {
                    switch ((Special) obj) {
                        case STREAM_COUNTER:
                            return "(int)" + streamName + ".getCounter()";
                        default:
                            throw new Error("Unexpected special");
                    }
                } else if (obj instanceof Integer) {
                    return obj.toString();
                } else if (obj instanceof String) {
                    return String.format("%s.getNamedValue(this, \"%s\")", (getCurrentStruct().isRoot() ? "this" : "this." + NAME_ROOT_STRUCT), obj.toString());
                } else if (obj instanceof JBBPNamedFieldInfo) {
                    final NamedFieldInfo namedFieldInfo = foundNamedFields.get(obj);
                    final String fieldPath = namedFieldInfo.makeSrcPath(getCurrentStruct());

                    String result;
                    switch (namedFieldInfo.fieldType) {
                        case BOOL: {
                            result = '(' + fieldPath + "?1:0)";
                        }
                        break;
                        case CUSTOM:
                        case VAR: {
                            result = "((JBBPNumericField)" + fieldPath + ").getAsInt()";
                        }
                        break;
                        default: {
                            result = "(int)" + fieldPath;
                        }
                        break;
                    }

                    return result;
                } else {
                    return null;
                }
            }

            @Override
            public ExpressionEvaluatorVisitor visitEnd() {
                buffer.setLength(0);

                for (int i = 0; i < this.stack.size(); i++) {
                    if (this.stack.get(i) instanceof Operator) {
                        final Operator op = (Operator) this.stack.remove(i);
                        i--;
                        ExprTreeItem newItem = new ExprTreeItem(op);
                        for (int j = 0; j < op.getArgsNumber(); j++) {
                            final Object val = this.stack.remove(i);
                            i--;
                            if (newItem.right == null) {
                                newItem.right = val;
                            } else {
                                newItem.left = val;
                            }
                        }
                        i++;
                        this.stack.add(i, newItem);
                    }
                }

                if (this.stack.size() != 1) {
                    throw new IllegalStateException("Stack must have only element");
                }

                final Object result = this.stack.remove(0);
                if (result instanceof ExprTreeItem) {
                    buffer.append('(').append(result.toString()).append(')');
                } else {
                    buffer.append(arg2str(result));
                }

                return this;
            }

            class ExprTreeItem {

                Operator op;
                Object left;
                Object right;

                ExprTreeItem(Operator op) {
                    this.op = op;
                }

                boolean doesNeedBrackets(Object obj) {
                    if (obj == null || !(obj instanceof ExprTreeItem)) {
                        return false;
                    }
                    final ExprTreeItem that = (ExprTreeItem) obj;

                    return that.op.getPriority() < this.op.getPriority() || ((that.op == Operator.LSHIFT || that.op == Operator.RSHIFT || that.op == Operator.URSHIFT) && (this.op == Operator.LSHIFT || this.op == Operator.RSHIFT || this.op == Operator.URSHIFT));
                }

                @Override
                public String toString() {
                    String leftStr = arg2str(this.left);
                    String rightStr = arg2str(this.right);
                    if (doesNeedBrackets(this.left)) {
                        leftStr = '(' + leftStr + ')';
                    }
                    if (doesNeedBrackets(this.right)) {
                        rightStr = '(' + rightStr + ')';
                    }
                    return (leftStr == null ? "" : leftStr) + this.op.getText() + (rightStr == null ? "" : rightStr);
                }
            }
        };

        evaluator.visitItems(this.compiledBlock, offsetInBlock, visitor);

        return buffer.toString();
    }

    @Override
    public void visitActionItem(final int offsetInCompiledBlock, final int actionType, final JBBPIntegerValueEvaluator nullableArgument) {
        final String valueTxtIn = nullableArgument == null ? "1" : evaluatorToString(NAME_INPUT_STREAM, offsetInCompiledBlock, nullableArgument, this.flagSet);
        final String valueTxtOut = nullableArgument == null ? "1" : evaluatorToString(NAME_OUTPUT_STREAM, offsetInCompiledBlock, nullableArgument, this.flagSet);

        switch (actionType) {
            case CODE_RESET_COUNTER: {
                getCurrentStruct().getReadFunc().println(NAME_INPUT_STREAM + ".resetCounter();");
                getCurrentStruct().getWriteFunc().println(NAME_OUTPUT_STREAM + ".resetCounter();");
            }
            break;
            case CODE_ALIGN: {
                getCurrentStruct().getReadFunc().indent().print(NAME_INPUT_STREAM + ".align(").print(valueTxtIn).println(");");
                getCurrentStruct().getWriteFunc().indent().print(NAME_OUTPUT_STREAM + ".align(").print(valueTxtOut).println(");");
            }
            break;
            case CODE_SKIP: {
                getCurrentStruct().getReadFunc().indent().print(NAME_INPUT_STREAM + ".skip(").print(valueTxtIn).println(");");
                getCurrentStruct().getWriteFunc().indent().printf("for(int I=0; I<%s; I++) %s.write(0);%n", valueTxtOut, NAME_OUTPUT_STREAM);
            }
            break;
            default: {
                throw new Error("Detected unknown action, contact developer!");
            }
        }
    }

    private enum FieldType {
        BOOL(CODE_BOOL, false, "boolean", "boolean", "%s.readBoolean()", "%s.readBoolArray(%s)", "%s.write(%s ? 1 : 0)", "for(int I=0;I<%3$s;I++){%1$s.write(%2$s[I] ? 1 : 0);}", "for(int I=0;I<%2$s.length;I++){%1$s.write(%2$s[I] ? 1 : 0);}"),
        BYTE(CODE_BYTE, false, "byte", "byte", "(byte)%s.readByte()", "%s.readByteArray(%s, %s)", "%s.write(%s)", "%1$s.writeBytes(%2$s, %3$s, %4$s)", "%1$s.writeBytes(%2$s, %2$s.length, %3$s)"),
        UBYTE(CODE_UBYTE, false, "char", "byte", "(char)(%s.readByte() & 0xFF)", "%s.readByteArray(%s, %s)", "%s.write(%s)", "%1$s.writeBytes(%2$s, %3$s, %4$s)", "%1$s.writeBytes(%2$s, %2$s.length, %3$s)"),
        SHORT(CODE_SHORT, true, "short", "short", "(short)%s.readUnsignedShort(%s)", "%s.readShortArray(%s,%s)", "%s.writeShort(%s,%s)", "for(int I=0;I<%3$s;I++){%1$s.writeShort(%2$s[I],%4$s);}", "for(int I=0;I<%2$s.length;I++){%1$s.writeShort(%2$s[I],%3$s);}"),
        USHORT(CODE_USHORT, true, "char", "char", "(char)%s.readUnsignedShort(%s)", "%s.readUShortArray(%s,%s)", "%s.writeShort(%s,%s)", "for(int I=0;I<%3$s;I++){%1$s.writeShort(%2$s[I],%4$s);}", "for(int I=0;I<%2$s.length;I++){%1$s.writeShort(%2$s[I],%3$s);}"),
        INT(CODE_INT, true, "int", "int", "%s.readInt(%s)", "%s.readIntArray(%s,%s)", "%s.writeInt(%s,%s)", "for(int I=0;I<%3$s;I++){%1$s.writeInt(%2$s[I],%4$s);}", "for(int I=0;I<%2$s.length;I++){%1$s.writeInt(%2$s[I],%3$s);}"),
        LONG(CODE_LONG, true, "long", "long", "%s.readLong(%s)", "%s.readLongArray(%s,%s)", "%s.writeLong(%s,%s)", "for(int I=0;I<%3$s;I++){%1$s.writeLong(%2$s[I],%4$s);}", "for(int I=0;I<%2$s.length;I++){%1$s.writeLong(%2$s[I],%3$s);}"),
        CUSTOM(-1, false, "", "", "", "", "", "", ""),
        VAR(-2, false, "", "", "", "", "", "", ""),
        BIT(-3, false, "", "", "", "", "", "", ""),
        UNKNOWN(Integer.MIN_VALUE, false, "", "", "", "", "", "", "");

        private final int code;
        private final String javaSingleType;
        private final String javaArrayType;
        private final String methodReadOne;
        private final String methodReadArray;
        private final String methodWriteOne;
        private final String methodWriteArray;
        private final String methodWriteArrayWithUnknownSize;
        private final boolean multiByte;

        FieldType(final int code, final boolean multiByte, final String javaSingleType, final String javaArrayType, final String readOne, final String readArray, final String writeOne, final String writeArray, final String writeArrayWithUnknownSize) {
            this.code = code;
            this.methodWriteArrayWithUnknownSize = writeArrayWithUnknownSize;
            this.multiByte = multiByte;
            this.javaSingleType = javaSingleType;
            this.javaArrayType = javaArrayType;
            this.methodReadArray = readArray;
            this.methodReadOne = readOne;
            this.methodWriteArray = writeArray;
            this.methodWriteOne = writeOne;
        }

        public static FieldType findForCode(final int code) {
            for (final FieldType t : values()) {
                if (t.code == code) {
                    return t;
                }
            }
            return UNKNOWN;
        }

        public void assertNotUnknown() {
            if (this == UNKNOWN) {
                throw new Error("Call method for unknown type");
            }
        }

        public String asJavaSingleFieldType() {
            assertNotUnknown();
            return this.javaSingleType;
        }

        public String asJavaArrayFieldType() {
            assertNotUnknown();
            return this.javaArrayType;
        }

        public String makeReaderForSingleField(final String streamName, final JBBPByteOrder byteOrder) {
            assertNotUnknown();
            return String.format(this.methodReadOne, streamName, "JBBPByteOrder." + byteOrder.name());
        }

        public String makeWriterForSingleField(final String streamName, final String fieldName, final JBBPByteOrder byteOrder) {
            assertNotUnknown();
            return String.format(this.methodWriteOne, streamName, fieldName, "JBBPByteOrder." + byteOrder.name());
        }

        public String makeReaderForArray(final String streamName, final String arraySize, final JBBPByteOrder byteOrder) {
            assertNotUnknown();
            return String.format(this.methodReadArray, streamName, arraySize, "JBBPByteOrder." + byteOrder.name());
        }

        public String makeWriterForArray(final String streamName, final String fieldName, final String arraySize, final JBBPByteOrder byteOrder) {
            assertNotUnknown();
            return String.format(this.methodWriteArray, streamName, fieldName, arraySize, "JBBPByteOrder." + byteOrder.name());
        }

        public String makeWriterForArrayWithUnknownSize(final String streamName, final String fieldName, final JBBPByteOrder byteOrder) {
            assertNotUnknown();
            return String.format(this.methodWriteArrayWithUnknownSize, streamName, fieldName, "JBBPByteOrder." + byteOrder.name());
        }
    }

    /**
     * Builder to build instance of converter.
     */
    public static final class Builder {

        /**
         * Set of interfaces to be implemented by the result class.
         */
        private final Set<String> interfaces = new HashSet<String>();
        /**
         * The Parser to provide compiled data.
         */
        private final JBBPParser parser;
        /**
         * The Package name.
         */
        private String classPackage;
        /**
         * The Result class name.
         */
        private String className;
        /**
         * The Flag to force the result class as abstract one.
         */
        private boolean forceAbstract;
        /**
         * The Name of class to be extended by the result class.
         */
        private String extendsClass;
        /**
         * The Comment to be placed before package info.
         */
        private String classHeadComment;
        /**
         * Flag to lock the builder.
         */
        private boolean lock;
        /**
         * Parser flags.
         */
        private int parserFlags;
        /**
         * Generate getters and setters.
         */
        private boolean doGettersSetters;
        /**
         * Text to be inserted into custom section of the resut class.
         */
        private String customText;

        private Builder(final JBBPParser parser) {
            this.parser = parser;
            this.parserFlags = parser.getFlags();
        }

        private void assertNonLocked() {
            if (this.lock) {
                throw new IllegalStateException("the Bui8lder already locked for changes");
            }
        }

        private void assertNotNull(final String message, final Object value) {
            if (value == null) {
                throw new NullPointerException(message);
            }
        }

        /**
         * Set custom text, the text will be added into the end of the result class.
         *
         * @param value text value, it can be null
         * @return the builder instance, must not be null
         */
        public Builder setCustomText(final String value) {
            assertNonLocked();
            this.customText = value;
            return this;
        }

        /**
         * Set flag to generate getters setters and all fields will be private ones.
         *
         * @param value flag, if true then generate getters setters, false otherwise
         * @return the builder instance, must not be null
         */
        public Builder setDoGettersSetters(final boolean value) {
            assertNonLocked();
            this.doGettersSetters = value;
            return this;
        }

        /**
         * Set the parser flags for the generated class, by default the flags are imported from the base parser.
         *
         * @param value the parser flags.
         * @return the builder instance, must not be null
         */
        public Builder setParserFlags(final int value) {
            assertNonLocked();
            this.parserFlags = value;
            return this;
        }

        /**
         * Set the package for the generated class.
         *
         * @param value name of the package, it can be empty or null in the case the class will be in the default package
         * @return the builder instance, must not be null
         */
        public Builder setClassPackage(final String value) {
            assertNonLocked();
            this.classPackage = value;
            return this;
        }

        /**
         * Set flag to force abstract modifier for the generated class, by default the class is abstract one only if it contains abstract methods.
         *
         * @param value true if to force the abstract modifier, false otherwise
         * @return the builder instance, must not be null
         */
        public Builder setForceAbstract(final boolean value) {
            assertNonLocked();
            this.forceAbstract = value;
            return this;
        }

        /**
         * The Name of the generated class. Must be provided.
         *
         * @param value the class name for the generated class, must not be null
         * @return the builder instance, must not be null
         */
        public Builder setClassName(final String value) {
            assertNonLocked();
            this.className = value;
            return this;
        }

        /**
         * Set the superclass for the generated class.
         *
         * @param value the superclass name, it can be null
         * @return the builder instance, must not be null
         */
        public Builder setSuperclass(final String value) {
            assertNonLocked();
            this.extendsClass = value;
            return this;
        }

        /**
         * Set insterfaces to be added into 'implements' for the generated class.
         *
         * @param values interface names
         * @return the builder instance, must not be null
         */
        public Builder setInterfaces(final String... values) {
            assertNonLocked();
            Collections.addAll(this.interfaces, values);
            return this;
        }

        /**
         * Set commentaries placed just before first package directive of the generated class.
         *
         * @param text text to be used as comment, it can be null
         * @return the builder instance, must not be null
         */
        public Builder setClassHeadComments(final String text) {
            assertNonLocked();
            this.classHeadComment = text;
            return this;
        }

        /**
         * Build converter with provided parameters. NB! It locks builder parameters and they can't be changed in future.
         *
         * @return a converter instance.
         */
        public JBBPToJava6Converter build() {
            this.lock = true;
            assertNotNull("Class name must not be null", this.className);
            return new JBBPToJava6Converter(this);
        }
    }

    private static class Struct {

        private final String classModifiers;
        private final String className;
        private final Struct parent;
        private final List<Struct> children = new ArrayList<Struct>();
        private final JavaSrcTextBuffer fields = new JavaSrcTextBuffer();
        private final JavaSrcTextBuffer readFunc = new JavaSrcTextBuffer();
        private final JavaSrcTextBuffer writeFunc = new JavaSrcTextBuffer();
        private final JavaSrcTextBuffer gettersSetters = new JavaSrcTextBuffer();
        private final String path;

        private Struct(final Struct parent, final String className, final String classModifiers) {
            this.path = parent == null ? "" : parent.path + (parent.path.length() == 0 ? "" : ".") + className.toLowerCase(Locale.ENGLISH);
            this.classModifiers = classModifiers;
            this.className = className;
            this.parent = parent;
            if (this.parent != null) {
                this.parent.children.add(this);
            }
        }

        private static String interfaces2str(final Set<String> set) {
            final StringBuilder buffer = new StringBuilder();
            for (final String s : set) {
                if (buffer.length() > 0) {
                    buffer.append(',');
                }
                buffer.append(s);
            }
            return buffer.toString();
        }

        public boolean isRoot() {
            return this.parent == null;
        }

        public String getPath() {
            return this.path;
        }

        public Struct findRoot() {
            if (this.parent == null) {
                return this;
            }
            return this.parent.findRoot();
        }

        public void write(final JavaSrcTextBuffer buffer, final String extraModifier, final String superClass, final Set<String> implementedInterfaces, final String commonSectionText, final String specialMethods, final String customText) {
            buffer.indent().printf(
                    "%s%sclass %s%s%s {%n",
                    this.classModifiers,
                    extraModifier == null ? " " : ' ' + extraModifier + ' ',
                    this.className,
                    superClass != null ? " extends " + superClass + ' ' : "",
                    implementedInterfaces != null && !implementedInterfaces.isEmpty() ? " implements " + interfaces2str(implementedInterfaces) + ' ' : ""
            );
            buffer.incIndent();

            if (commonSectionText != null) {
                buffer.printLinesWithIndent(commonSectionText);
            }

            for (final Struct c : this.children) {
                c.write(buffer, null, null, null, null, null, null);
            }
            buffer.println();

            buffer.printLinesWithIndent(this.fields.toString());
            if (this.parent != null) {
                buffer.indent().println("private final " + findRoot().className + ' ' + NAME_ROOT_STRUCT + ';');
            }
            buffer.println();

            buffer.indent().print("public ").print(this.className).print(" (")
                    .print(this.parent == null ? "" : (findRoot().className + " root"))
                    .println(") {");

            buffer.incIndent();
            if (this.parent != null) {
                buffer.indent().print(NAME_ROOT_STRUCT).print(" = ").println("root;");
            }
            buffer.decIndent();

            buffer.indent().println("}");

            buffer.println();

            buffer.indent().printf("public %s read(final JBBPBitInputStream In) throws IOException {%n", this.className);
            buffer.incIndent();
            buffer.printLinesWithIndent(this.readFunc.toString());
            buffer.indent().println("return this;");
            buffer.decIndent();
            buffer.indent().println("}");

            buffer.println();

            buffer.indent().printf("public %s write(final JBBPBitOutputStream Out) throws IOException {%n", this.className);
            buffer.incIndent();
            buffer.printLinesWithIndent(this.writeFunc.toString());
            buffer.indent().println("return this;");
            buffer.decIndent();
            buffer.indent().println("}");

            if (specialMethods != null) {
                buffer.println();
                buffer.printLinesWithIndent(specialMethods);
                buffer.println();
            }

            if (!this.gettersSetters.isEmpty()) {
                buffer.println();
                buffer.printLinesWithIndent(this.gettersSetters.toString());
                buffer.println();
            }

            if (customText != null && customText.length() != 0) {
                buffer.printCommentLinesWithIndent("------ Custom section START");
                buffer.printLinesWithIndent(customText);
                buffer.printCommentLinesWithIndent("------ Custom section END");
            }

            buffer.decIndent();
            buffer.indent().println("}");

        }

        public JavaSrcTextBuffer getWriteFunc() {
            return this.writeFunc;
        }

        public JavaSrcTextBuffer getReadFunc() {
            return this.readFunc;
        }

        public JavaSrcTextBuffer getFields() {
            return this.fields;
        }

        public JavaSrcTextBuffer getGettersSetters() {
            return this.gettersSetters;
        }
    }

    private static final class NamedFieldInfo {

        final JBBPNamedFieldInfo info;
        final Struct struct;
        final FieldType fieldType;

        NamedFieldInfo(final JBBPNamedFieldInfo info, final Struct struct, final FieldType fieldType) {
            this.info = info;
            this.struct = struct;
            this.fieldType = fieldType;
        }

        public String makeSrcPath(final Struct currentStruct) {
            if (this.struct == currentStruct) {
                return "this." + info.getFieldName();
            } else {
                final String structPath = this.struct.getPath();
                if (currentStruct.isRoot()) {
                    return "this." + (structPath.length() == 0 ? "" : structPath + ".") + info.getFieldName();
                } else {
                    return "this." + NAME_ROOT_STRUCT + '.' + (structPath.length() == 0 ? "" : structPath + ".") + info.getFieldName();
                }
            }
        }
    }
}
