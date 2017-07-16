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
package com.igormaznitsa.jbbp.compiler.utils.converter;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.compiler.JBBPCompiledBlock;
import com.igormaznitsa.jbbp.compiler.JBBPCompiler;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.compiler.varlen.JBBPIntegerValueEvaluator;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.igormaznitsa.jbbp.compiler.JBBPCompiler.*;

public class ConverterToJavaClassSrc extends AbstractCompiledBlockConverter<ConverterToJavaClassSrc> {

    private static final String ROOT_STRUCT_NAME = "_Root_";
    private static final String PARSER_FLAGS_FIELD = "_ParserFlags_";

    private final String packageName;

    private final String className;
    private final AtomicBoolean detectedCustomFields = new AtomicBoolean();
    private final AtomicBoolean detectedExternalFieldsInEvaluator = new AtomicBoolean();
    private final AtomicBoolean detectedVarFields = new AtomicBoolean();
    private final AtomicInteger anonymousFieldCounter = new AtomicInteger();
    private final AtomicInteger specialFieldsCounter = new AtomicInteger();
    private final Map<JBBPNamedFieldInfo, NamedFieldInfo> detectedNamedFields = new HashMap<JBBPNamedFieldInfo, NamedFieldInfo>();
    private final List<Struct> structStack = new ArrayList<Struct>();
    private final TextBuffer specialSection = new TextBuffer();
    private final TextBuffer specialMethods = new TextBuffer();
    private String result;

    public ConverterToJavaClassSrc(final String packageName, final String className, final JBBPParser notNullParser) {
        this(packageName, className, notNullParser.getFlags(), notNullParser.getCompiledBlock());
    }

    public ConverterToJavaClassSrc(final String packageName, final String className, final int parserFlags, final JBBPCompiledBlock notNullCompiledBlock) {
        super(parserFlags, notNullCompiledBlock);
        this.packageName = packageName;
        this.className = className;
    }

    private NamedFieldInfo registerNamedField(final JBBPNamedFieldInfo fieldInfo, final FieldType fieldType) {
        NamedFieldInfo result = null;
        if (fieldInfo != null) {
            if (this.detectedNamedFields.containsKey(fieldInfo))
                throw new Error("Detected duplication of named field : " + fieldInfo);
            result = new NamedFieldInfo(fieldInfo, this.getCurrentStruct(), fieldType);
            this.detectedNamedFields.put(fieldInfo, result);
        }
        return result;
    }

    private Struct getCurrentStruct() {
        return this.structStack.get(0);
    }

    @Override
    public void onConvertStart() {
        this.detectedCustomFields.set(false);
        this.detectedExternalFieldsInEvaluator.set(false);
        this.detectedVarFields.set(false);
        this.detectedNamedFields.clear();
        this.anonymousFieldCounter.set(1234);
        this.specialFieldsCounter.set(1);
        this.specialSection.clean();
        this.structStack.clear();
        this.specialMethods.clean();

        this.structStack.add(new Struct(null, null, className, "public"));
    }

    public String getResult() {
        return this.result;
    }

    @Override
    public void onConvertEnd() {
        final TextBuffer buffer = new TextBuffer();

        buffer.print("package ").print(this.packageName).println(";");

        buffer.println();

        buffer.println("import com.igormaznitsa.jbbp.model.*;");
        buffer.println("import com.igormaznitsa.jbbp.io.*;");
        buffer.println("import com.igormaznitsa.jbbp.compiler.*;");
        buffer.println("import com.igormaznitsa.jbbp.compiler.tokenizer.*;");
        buffer.println("import java.io.IOException;");
        buffer.println("import java.util.*;");

        buffer.println();

        this.specialSection.println();
        this.specialSection.printJavaDocLinesWithIndent("Constant contains parser flags\n@see JBBPParser#FLAG_SKIP_REMAINING_FIELDS_IF_EOF");
        this.specialSection.indent().printf("protected static final int %s = %d;",PARSER_FLAGS_FIELD,this.parserFlags);

        if (this.detectedCustomFields.get()) {
            this.specialMethods.printJavaDocLinesWithIndent("Reading of custom fields\n@param sourceStruct source structure holding the field, must not be null\n@param inStream the input stream, must not be null\n@param bitOrder bit order to read data, must not be null\n@param typeParameterContainer info about field type, must not be null\n@param nullableNamedFieldInfo info abut field name, it can be null\n@param extraValue value from extra field part\n@param readWholeStream flag to read the stream as array till the stream end if true\n@param arraySize if array then it is zero or great\n@exception IOException if data can't be read\n@return read value as abstract field, must not be null");
            this.specialMethods.println("public abstract JBBPAbstractField readCustomFieldType(Object sourceStruct, JBBPBitInputStream inStream, JBBPBitOrder bitOrder, JBBPFieldTypeParameterContainer typeParameterContainer, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue, boolean readWholeStream, int arraySize) throws IOException;");
            this.specialMethods.println();
            this.specialMethods.printJavaDocLinesWithIndent("Writing custom fields\n@param sourceStruct source structure holding the field, must not be null\n@param outStream the output stream, must not be null\n@param fieldValue value to be written\n@param typeParameterContainer info about field type, must not be null\n@param nullableNamedFieldInfo info abut field name, it can be null\n@param extraValue value from extra field part\n@param arraySize if array then it is zero or great\n@exception IOException if data can't be written");
            this.specialMethods.println("public abstract void writeCustomFieldType(Object sourceStruct, JBBPBitOutputStream outStream, JBBPAbstractField fieldValue, JBBPFieldTypeParameterContainer typeParameterContainer, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue, int arraySize) throws IOException;");
        }

        if (this.detectedExternalFieldsInEvaluator.get()) {
            if (!this.specialMethods.isEmpty()) {
                this.specialMethods.println();
            }
            this.specialMethods.printJavaDocLinesWithIndent("Method is called from expressions to provide value\n@param sourceStruct source structure holding the field, must not be null\n@param valueName name of value to be provided, must not be null\n@return integer value for the named parameter");
            this.specialMethods.println("public abstract int getNamedValue(Object sourceStruct, String valueName);");
        }

        if (this.detectedVarFields.get()) {
            if (!this.specialMethods.isEmpty()) {
                this.specialMethods.println();
            }
            this.specialMethods.printJavaDocLinesWithIndent("Read variable field\n@param sourceStruct source structure holding the field, must not be null\n@param inStream the input stream, must not be null\n@param byteOrder\n@param nullableNamedFieldInfo info abut field name, it can be null\n@param extraValue value from extra field part\n@return\n@exception IOException");
            this.specialMethods.println("public abstract JBBPAbstractField readVarField(Object sourceStruct, JBBPBitInputStream inStream, JBBPByteOrder byteOrder, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue) throws IOException;");
            this.specialMethods.println();
            this.specialMethods.printJavaDocLinesWithIndent("Read variable array field\n@param sourceStruct source structure holding the field, must not be null\n@param inStream the input stream, must not be null\n@param byteOrder byte order to be used for reading, must not be null\n@param nullableNamedFieldInfo info abut field name, it can be null\n@param extraValue value from extra field part\n@param readWholeStream if true then whole stream should be read\n@param arraySize size of array to read (if whole stream flag is false)\n@return array object contains read data, must not be null\n@exception IOException if error during data reading");
            this.specialMethods.println("public abstract JBBPAbstractArrayField<? extends JBBPAbstractField> readVarArray(Object sourceStruct, JBBPBitInputStream inStream, JBBPByteOrder byteOrder, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue, boolean readWholeStream, int arraySize) throws IOException;");
            this.specialMethods.println();
            this.specialMethods.printJavaDocLinesWithIndent("Read variable field\n@param sourceStruct source structure holding the field, must not be null\n@param value field value, must not be null\n@param outStream the output stream, must not be null,\n@param byteOrder byte order to be used for reading, must not be null\n@param nullableNamedFieldInfo info abut field name, it can be null\n@param extraValue value from extra field part\n@exception IOException  it is thrown if any transport error during operation");
            this.specialMethods.println("public abstract void writeVarField(Object sourceStruct, JBBPAbstractField value, JBBPBitOutputStream outStream, JBBPByteOrder byteOrder, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue) throws IOException;");
            this.specialMethods.println();
            this.specialMethods.printJavaDocLinesWithIndent("Write variable array\n@param sourceStruct source structure holding the field, must not be null\n@param array array value to be written, must not be null\n@param outStream the output stream, must not be null\n@param byteOrder byte order to be used for reading, must not be null\n@param nullableNamedFieldInfo info abut field name, it can be null\n@param extraValue value from extra field part\n@param arraySizeToWrite\n@exception IOException it is thrown if any transport error during operation");
            this.specialMethods.println("public abstract void writeVarArray(Object sourceStruct, JBBPAbstractArrayField<? extends JBBPAbstractField> array, JBBPBitOutputStream outStream, JBBPByteOrder byteOrder, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue, int arraySizeToWrite) throws IOException;");
        }

        final String specialMethodsText = this.specialMethods.toString();

        buffer.printJavaDocLinesWithIndent("Generated from JBBP script by internal JBBP Class Source Generator");

        this.structStack.get(0).write(buffer,
                this.detectedCustomFields.get() || this.detectedVarFields.get() || this.detectedExternalFieldsInEvaluator.get() ? "abstract" : null,
                this.specialSection.toString(), specialMethodsText.length() == 0 ? null : specialMethodsText);

        this.result = buffer.toString();
    }

    @Override
    public void onStructStart(final int offsetInCompiledBlock, final JBBPNamedFieldInfo nullableNameFieldInfo, final JBBPIntegerValueEvaluator nullableArraySize) {
        final String structName = (nullableNameFieldInfo == null ? makeAnonymousStructName() : nullableNameFieldInfo.getFieldName()).toLowerCase(Locale.ENGLISH);
        final String structType = structName.toUpperCase(Locale.ENGLISH);
        final String arraySizeIn = nullableArraySize == null ? null : evaluatorToString("In", offsetInCompiledBlock, nullableArraySize, this.detectedExternalFieldsInEvaluator);
        final String arraySizeOut = nullableArraySize == null ? null : evaluatorToString("Out", offsetInCompiledBlock, nullableArraySize, this.detectedExternalFieldsInEvaluator);
        final Struct newStruct = new Struct(nullableNameFieldInfo, this.getCurrentStruct(), structType, "public static");

        final String fieldModifier;
        if (nullableNameFieldInfo == null) {
            fieldModifier = "protected";
        } else {
            fieldModifier = "public";
        }

        if (nullableArraySize == null) {
            this.getCurrentStruct().getFields().indent().print(fieldModifier).printf(" %s %s;", structType, structName).println();
            processSkipRemainingFlag();
            this.getCurrentStruct().getReadFunc().indent()
                    .printf("if ( this.%1$s == null) { this.%1$s = new %2$s(%3$s);}", structName, structType, this.structStack.size() == 1 ? "this" : "this." + ROOT_STRUCT_NAME)
                    .printf(" this.%s.read(In);%n", structName);
            this.getCurrentStruct().getWriteFunc().indent().print(structName).println(".write(Out);");
        } else {
            this.getCurrentStruct().getFields().indent().print(fieldModifier).printf(" %s [] %s;", structType, structName).println();
            processSkipRemainingFlag();
            if ("-1".equals(arraySizeIn)) {
                this.getCurrentStruct().getReadFunc().indent()
                        .printf("List<%3$s> __%1$s_tmplst__ = new ArrayList<%3$s>(); while (In.hasAvailableData()){ __%1$s_tmplst__.add(new %3$s(%4$s).read(In));} this.%1$s = __%1$s_tmplst__.toArray(new %3$s[__%1$s_tmplst__.size()]);__%1$s_tmplst__ = null;%n", structName, arraySizeIn, structType, (this.structStack.size() == 1 ? "this" : ROOT_STRUCT_NAME));
                this.getCurrentStruct().getWriteFunc().indent().printf("for (int I=0;I<this.%1$s.length;I++){ this.%1$s[I].write(Out); }%n", structName);
            } else {
                this.getCurrentStruct().getReadFunc().indent()
                        .printf("if (this.%1$s == null || this.%1$s.length != %2$s){ this.%1$s = new %3$s[%2$s]; for(int I=0;I<%2$s;I++){ this.%1$s[I] = new %3$s(%4$s);}}", structName, arraySizeIn, structType, (this.structStack.size() == 1 ? "this" : "this." + ROOT_STRUCT_NAME))
                        .printf("for (int I=0;I<%2$s;I++){ this.%1$s[I].read(in); }%n", structName, arraySizeIn);
                this.getCurrentStruct().getWriteFunc().indent().printf("for (int I=0;I<%2$s;I++){ this.%1$s[I].write(Out); }", structName, arraySizeOut);
            }
        }


        this.structStack.add(0, newStruct);
    }

    private void processSkipRemainingFlag() {
        if (this.isFlagSkipRemainingFieldsIfEOF()) {
            this.getCurrentStruct().getReadFunc().indent().println("if (!in.hasAvailableData()) return this;");
        }
    }

    @Override
    public void onStructEnd(final int offsetInCompiledBlock, final JBBPNamedFieldInfo nullableNameFieldInfo) {
        this.structStack.remove(0);
    }

    @Override
    public void onPrimitive(final int offsetInCompiledBlock, final int primitiveType, final JBBPNamedFieldInfo nullableNameFieldInfo, final JBBPByteOrder byteOrder, final JBBPIntegerValueEvaluator nullableArraySize) {
        final String fieldName = nullableNameFieldInfo == null ? makeAnonymousFieldName() : nullableNameFieldInfo.getFieldName();
        final FieldType type = FieldType.findForCode(primitiveType);

        registerNamedField(nullableNameFieldInfo, type);

        final String arraySizeIn = nullableArraySize == null ? null : evaluatorToString("In", offsetInCompiledBlock, nullableArraySize, this.detectedExternalFieldsInEvaluator);
        final String arraySizeOut = nullableArraySize == null ? null : evaluatorToString("Out", offsetInCompiledBlock, nullableArraySize, this.detectedExternalFieldsInEvaluator);


        final String fieldModifier;
        if (nullableNameFieldInfo == null) {
            fieldModifier = "protected";
        } else {
            fieldModifier = "public";
        }

        processSkipRemainingFlag();
        if (nullableArraySize == null) {
            getCurrentStruct().getFields().printf("%s %s %s;%n", fieldModifier, type.asJavaSingleFieldType(), fieldName);
            getCurrentStruct().getReadFunc().println(String.format("this.%s = %s;", fieldName, type.makeReaderForSingleField("In", byteOrder)));
            getCurrentStruct().getWriteFunc().print(type.makeWriterForSingleField("Out", "this." + fieldName, byteOrder)).println(";");
        } else {
            getCurrentStruct().getFields().printf("%s %s [] %s;%n", fieldModifier, type.asJavaArrayFieldType(), fieldName);
            getCurrentStruct().getReadFunc().printf("this.%s = %s;%n", fieldName, type.makeReaderForArray("In", arraySizeIn, byteOrder));
            if ("-1".equals(arraySizeOut)) {
                getCurrentStruct().getWriteFunc().print(type.makeWriterForArrayWithUnknownSize("Out", "this." + fieldName, byteOrder)).println(";");
            } else {
                getCurrentStruct().getWriteFunc().print(type.makeWriterForArray("Out", "this." + fieldName, arraySizeOut, byteOrder)).println(";");
            }
        }
    }

    @Override
    public void onBitField(final int offsetInCompiledBlock, final JBBPNamedFieldInfo nullableNameFieldInfo, final JBBPIntegerValueEvaluator notNullFieldSize, final JBBPIntegerValueEvaluator nullableArraySize) {
        final String fieldName = nullableNameFieldInfo == null ? makeAnonymousFieldName() : nullableNameFieldInfo.getFieldName();

        registerNamedField(nullableNameFieldInfo, FieldType.BIT);

        String sizeOfFieldIn = evaluatorToString("In", offsetInCompiledBlock, notNullFieldSize, this.detectedExternalFieldsInEvaluator);
        String sizeOfFieldOut = evaluatorToString("Out", offsetInCompiledBlock, notNullFieldSize, this.detectedExternalFieldsInEvaluator);
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

        final String arraySizeIn = nullableArraySize == null ? null : evaluatorToString("In", offsetInCompiledBlock, nullableArraySize, this.detectedExternalFieldsInEvaluator);
        final String arraySizeOut = nullableArraySize == null ? null : evaluatorToString("Out", offsetInCompiledBlock, nullableArraySize, this.detectedExternalFieldsInEvaluator);

        final String fieldModifier;
        if (nullableNameFieldInfo == null) {
            fieldModifier = "protected";
        } else {
            fieldModifier = "public";
        }

        processSkipRemainingFlag();

        if (arraySizeIn == null) {
            getCurrentStruct().getReadFunc().indent().printf("this.%s = (short)(In.readBitField(%s) & 0xFF);%n", fieldName, sizeOfFieldIn);
        } else {
            getCurrentStruct().getReadFunc().indent().print(fieldName).print(" = In.readBitsArray(").print(arraySizeIn).print(",").print(sizeOfFieldIn).println(");");
        }

        if (arraySizeOut == null) {
            getCurrentStruct().getWriteFunc().indent().printf("Out.writeBits(this.%s,%s);%n", fieldName, sizeOfFieldOut);
        } else {
            if ("-1".equals(arraySizeIn)) {
                getCurrentStruct().getWriteFunc().indent().printf("for(int I=0; I<%s.length; I++)", fieldName).printf(" Out.writeBits(this.%s[I],%s);%n", fieldName, sizeOfFieldOut);
            } else {
                getCurrentStruct().getWriteFunc().indent().printf("for(int I=0; I<%s; I++)", arraySizeOut).printf(" Out.writeBits(this.%s[I],%s);%n", fieldName, sizeOfFieldOut);
            }
        }

        if (nullableArraySize == null) {
            getCurrentStruct().getFields().indent().printf("%s short %s;%n", fieldModifier, fieldName);
        } else {
            getCurrentStruct().getFields().indent().printf("%s byte [] %s;%n", fieldModifier, fieldName);
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

    @Override
    public void onCustom(final int offsetInCompiledBlock, final JBBPFieldTypeParameterContainer notNullfieldType, final JBBPNamedFieldInfo nullableNameFieldInfo, final JBBPByteOrder byteOrder, final boolean readWholeStream, final JBBPIntegerValueEvaluator nullableArraySizeEvaluator, final JBBPIntegerValueEvaluator extraDataValueEvaluator) {
        this.detectedCustomFields.set(true);

        registerNamedField(nullableNameFieldInfo, FieldType.CUSTOM);

        final String fieldName = nullableNameFieldInfo == null ? makeAnonymousFieldName() : nullableNameFieldInfo.getFieldName();
        final String fieldModifier;
        if (nullableNameFieldInfo == null) {
            fieldModifier = "protected";
        } else {
            fieldModifier = "public";
        }

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
                "new JBBPFieldTypeParameterContainer(JBBPByteOrder." + notNullfieldType.getByteOrder().name() + ",\"" + notNullfieldType.getTypeName() + "\"," + notNullfieldType.getExtraData() + ")"
        );

        processSkipRemainingFlag();
        this.getCurrentStruct().getReadFunc().printf("%s = %s;%n",
                fieldName,
                String.format("%s.readCustomFieldType(this, In, In.getBitOrder(), %s, %s, %s, %b, %s)",
                        this.getCurrentStruct().isRoot() ? "this" : "this." + ROOT_STRUCT_NAME,
                        specialFieldName_typeParameterContainer,
                        nullableNameFieldInfo == null ? "null" : specialFieldName_fieldNameInfo,
                        extraDataValueEvaluator == null ? "0" : evaluatorToString("In", offsetInCompiledBlock, extraDataValueEvaluator, this.detectedExternalFieldsInEvaluator),
                        readWholeStream,
                        nullableArraySizeEvaluator == null ? "-1" : evaluatorToString("In", offsetInCompiledBlock, nullableArraySizeEvaluator, this.detectedExternalFieldsInEvaluator)
                )
        );

        this.getCurrentStruct().getWriteFunc().printf("%s;%n",
                String.format("%s.writeCustomFieldType(this, Out, %s, %s, %s, %s, %s)",
                        this.getCurrentStruct().isRoot() ? "this" : "this." + ROOT_STRUCT_NAME,
                        "this." + fieldName,
                        specialFieldName_typeParameterContainer,
                        nullableNameFieldInfo == null ? "null" : specialFieldName_fieldNameInfo,
                        extraDataValueEvaluator == null ? "0" : evaluatorToString("Out", offsetInCompiledBlock, extraDataValueEvaluator, this.detectedExternalFieldsInEvaluator),
                        nullableArraySizeEvaluator == null ? "-1" : evaluatorToString("Out", offsetInCompiledBlock, nullableArraySizeEvaluator, this.detectedExternalFieldsInEvaluator)
                )
        );
    }

    @Override
    public void onVar(final int offsetInCompiledBlock, final JBBPNamedFieldInfo nullableNameFieldInfo, final JBBPByteOrder byteOrder, final boolean readWholeStreamIntoArray, final JBBPIntegerValueEvaluator nullableArraySizeEvaluator, final JBBPIntegerValueEvaluator extraDataValueEvaluator) {
        this.detectedVarFields.set(true);

        registerNamedField(nullableNameFieldInfo, FieldType.VAR);

        final String fieldName = nullableNameFieldInfo == null ? makeAnonymousFieldName() : nullableNameFieldInfo.getFieldName();
        final String fieldModifier;
        if (nullableNameFieldInfo == null) {
            fieldModifier = "protected";
        } else {
            fieldModifier = "public";
        }

        final String specialFieldName = makeSpecialFieldName();
        final String specialFieldName_fieldNameInfo = specialFieldName + "FieldInfo";

        if (nullableNameFieldInfo != null) {
            this.specialSection.printf("private static final JBBPNamedFieldInfo %s = %s;%n",
                    specialFieldName_fieldNameInfo,
                    "new JBBPNamedFieldInfo(\"" + nullableNameFieldInfo.getFieldName() + "\",\"" + nullableNameFieldInfo.getFieldPath() + "\"," + nullableNameFieldInfo.getFieldOffsetInCompiledBlock() + ")"
            );
        }

        processSkipRemainingFlag();
        if (readWholeStreamIntoArray || nullableArraySizeEvaluator != null) {
            this.getCurrentStruct().getFields().printf("%s JBBPAbstractArrayField<? extends JBBPAbstractField> %s;%n", fieldModifier, fieldName);

            this.getCurrentStruct().getReadFunc().printf("%s = %s;%n",
                    fieldName,
                    String.format("%s.readVarArray(this, In, %s, %s, %s, %b, %s)",
                            this.getCurrentStruct().isRoot() ? "this" : "this." + ROOT_STRUCT_NAME,
                            "JBBPByteOrder." + byteOrder.name(),
                            nullableNameFieldInfo == null ? "null" : specialFieldName_fieldNameInfo,
                            extraDataValueEvaluator == null ? "0" : evaluatorToString("In", offsetInCompiledBlock, extraDataValueEvaluator, this.detectedExternalFieldsInEvaluator),
                            readWholeStreamIntoArray,
                            nullableArraySizeEvaluator == null ? "-1" : evaluatorToString("In", offsetInCompiledBlock, nullableArraySizeEvaluator, this.detectedExternalFieldsInEvaluator)
                    )
            );

            this.getCurrentStruct().getWriteFunc().printf("%s.writeVarArray(this, this.%s, Out, %s, %s, %s, %s);%n",
                    this.getCurrentStruct().isRoot() ? "this" : "this." + ROOT_STRUCT_NAME,
                    fieldName,
                    "JBBPByteOrder." + byteOrder.name(),
                    nullableNameFieldInfo == null ? "null" : specialFieldName_fieldNameInfo,
                    extraDataValueEvaluator == null ? "0" : evaluatorToString("In", offsetInCompiledBlock, extraDataValueEvaluator, this.detectedExternalFieldsInEvaluator),
                    nullableArraySizeEvaluator == null ? "-1" : evaluatorToString("Out", offsetInCompiledBlock, nullableArraySizeEvaluator, this.detectedExternalFieldsInEvaluator)
            );

        } else {
            this.getCurrentStruct().getFields().printf("%s JBBPAbstractField %s;%n", fieldModifier, fieldName);

            this.getCurrentStruct().getReadFunc().printf("%s = %s;%n",
                    fieldName,
                    String.format("%s.readVarField(this, In, %s, %s, %s)",
                            this.getCurrentStruct().isRoot() ? "this" : "this." + ROOT_STRUCT_NAME,
                            "JBBPByteOrder." + byteOrder.name(),
                            nullableNameFieldInfo == null ? "null" : specialFieldName_fieldNameInfo,
                            extraDataValueEvaluator == null ? "0" : evaluatorToString("In", offsetInCompiledBlock, extraDataValueEvaluator, this.detectedExternalFieldsInEvaluator))
            );

            this.getCurrentStruct().getWriteFunc().printf("%s.writeVarField(this, this.%s, Out, %s, %s, %s);%n",
                    this.getCurrentStruct().isRoot() ? "this" : "this." + ROOT_STRUCT_NAME,
                    fieldName,
                    "JBBPByteOrder." + byteOrder.name(),
                    nullableNameFieldInfo == null ? "null" : specialFieldName_fieldNameInfo,
                    extraDataValueEvaluator == null ? "0" : evaluatorToString("Out", offsetInCompiledBlock, extraDataValueEvaluator, this.detectedExternalFieldsInEvaluator)
            );
        }
    }

    private String evaluatorToString(final String streamName, final int offsetInBlock, final JBBPIntegerValueEvaluator evaluator, final AtomicBoolean detectedExternalField) {
        final StringBuilder buffer = new StringBuilder();

        final ExpressionEvaluatorVisitor visitor = new ExpressionEvaluatorVisitor() {
            private final List<Object> stack = new ArrayList<Object>();

            @Override
            public ExpressionEvaluatorVisitor begin() {
                this.stack.clear();
                return this;
            }

            @Override
            public ExpressionEvaluatorVisitor visit(final Special specialField) {
                this.stack.add(specialField);
                return this;
            }

            @Override
            public ExpressionEvaluatorVisitor visit(final JBBPNamedFieldInfo nullableNameFieldInfo, final String nullableExternalFieldName) {
                if (nullableNameFieldInfo != null) {
                    this.stack.add(nullableNameFieldInfo);
                } else if (nullableExternalFieldName != null) {
                    detectedExternalField.set(true);
                    this.stack.add(nullableExternalFieldName);
                }
                return this;
            }

            @Override
            public ExpressionEvaluatorVisitor visit(final Operator operator) {
                this.stack.add(operator);
                return this;
            }

            @Override
            public ExpressionEvaluatorVisitor visit(final int value) {
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
                    return String.format("%s.getNamedValue(this, \"%s\")", (getCurrentStruct().isRoot() ? "this" : "this." + ROOT_STRUCT_NAME), obj.toString());
                } else if (obj instanceof JBBPNamedFieldInfo) {
                    final NamedFieldInfo namedFieldInfo = detectedNamedFields.get(obj);
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
            public ExpressionEvaluatorVisitor end() {
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

                if (this.stack.size() != 1) throw new IllegalStateException("Stack must have only element");

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
                    if (obj == null || !(obj instanceof ExprTreeItem)) return false;
                    final ExprTreeItem that = (ExprTreeItem) obj;
                    return that.op.getPriority() < this.op.getPriority();
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

        evaluator.visit(this.compiledBlock, offsetInBlock, visitor);

        return buffer.toString();
    }

    @Override
    public void onActionItem(final int offsetInCompiledBlock, final int actionType, final JBBPIntegerValueEvaluator nullableArgument) {
        final String valueTxtIn = nullableArgument == null ? "1" : evaluatorToString("In", offsetInCompiledBlock, nullableArgument, this.detectedExternalFieldsInEvaluator);
        final String valueTxtOut = nullableArgument == null ? "1" : evaluatorToString("Out", offsetInCompiledBlock, nullableArgument, this.detectedExternalFieldsInEvaluator);

        switch (actionType) {
            case JBBPCompiler.CODE_RESET_COUNTER: {
                getCurrentStruct().getReadFunc().println("In.resetCounter();");
                getCurrentStruct().getWriteFunc().println("Out.resetCounter();");
            }
            break;
            case JBBPCompiler.CODE_ALIGN: {
                getCurrentStruct().getReadFunc().indent().print("In.align(").print(valueTxtIn).println(");");
                getCurrentStruct().getWriteFunc().indent().print("Out.align(").print(valueTxtOut).println(");");
            }
            break;
            case JBBPCompiler.CODE_SKIP: {
                getCurrentStruct().getReadFunc().indent().print("In.skip(").print(valueTxtIn).println(");");
                getCurrentStruct().getWriteFunc().indent().print("for(int I=0; I<").print(valueTxtOut).println(";I++) Out.write(0);");
            }
            break;
            default: {
                throw new Error("Detected unknown action, contact developer!");
            }
        }
    }

    private enum FieldType {
        BOOL(CODE_BOOL, false, "boolean", "boolean", "%s.readBoolean()", "%s.readBoolArray(%s)", "%s.write(%s ? 1 : 0)", "for(int I=0;I<%3$s;I++){%1$s.write(%2$s[I] ? 1 : 0);}", "for(int I=0;I<%2$s.length;I++){%1$s.write(%2$s[I] ? 1 : 0);}"),
        BYTE(CODE_BYTE, false, "byte", "byte", "(byte)%s.readByte()", "%s.readByteArray(%s)", "%s.write(%s)", "for(int I=0;I<%3$s;I++){%1$s.write(%2$s[I]);}", "for(int I=0;I<%2$s.length;I++){%1$s.write(%2$s[I]);}"),
        UBYTE(CODE_UBYTE, false, "char", "byte", "(char)(%s.readByte() & 0xFF)", "%s.readByteArray(%s)", "%s.write(%s)", "for(int I=0;I<%3$s;I++){%1$s.write(%2$s[I] & 0xFF);}", "for(int I=0;I<%2$s.length;I++){%1$s.write(%2$s[I] & 0xFF);}"),
        SHORT(CODE_SHORT, true, "short", "short", "(short)%s.readUnsignedShort(%s)", "%s.readShortArray(%s,%s)", "%s.writeShort(%s,%s)", "for(int I=0;I<%3$s;I++){%1$s.writeShort(%2$s[I],%4$s);}", "for(int I=0;I<%2$s.length;I++){%1$s.writeShort(%2$s[I],%3$s);}"),
        USHORT(CODE_USHORT, true, "char", "char", "(char)%s.readUnsignedShort(%s)", "%s.readUShortArray(%s,%s)", "%s.writeShort(%s,%s)", "for(int I=0;I<%3$s;I++){%1$s.writeShort(%2$s[I],%4$s);}", "for(int I=0;I<%2$s.length;I++){%1$s.writeShort(%2$s[I],%3$s);}"),
        INT(CODE_INT, true, "int", "int", "%s.readInt(%s)", "%s.readIntArray(%s,%s)", "%s.writeInt(%s,%s)", "for(int I=0;I<%3$s;I++){%1$s.writeInt(%2$s[I],%4$s);}", "for(int I=0;I<%2$s.length;I++){%1$s.writeInt(%2$s[I],%3$s);}"),
        LONG(CODE_LONG, true, "long", "long", "%s.readLong(%s)", "%s.readLongArray(%s,%s)", "%s.writeLong(%s,%s)", "for(int I=0;I<%3$s;I++){%1$s.writeLong(%2$s[I],%4$s);}", "for(int I=0;I<%2$s.length;I++){%1$s.writeLong(%2$s[I],%3$s);}"),
        CUSTOM(-1, false, "", "", "", "", "", "", ""),
        VAR(-2, false, "", "", "", "", "", "", ""),
        BIT(-3, false, "", "", "", "", "", "", "");

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
                if (t.code == code) return t;
            }
            return null;
        }

        public String asJavaSingleFieldType() {
            return this.javaSingleType;
        }

        public String asJavaArrayFieldType() {
            return this.javaArrayType;
        }

        public String makeReaderForSingleField(final String streamName, final JBBPByteOrder byteOrder) {
            if (this.multiByte) {
                return String.format(this.methodReadOne, streamName, "JBBPByteOrder." + byteOrder.name());
            } else {
                return String.format(this.methodReadOne, streamName);
            }
        }

        public String makeWriterForSingleField(final String streamName, final String fieldName, final JBBPByteOrder byteOrder) {
            if (this.multiByte) {
                return String.format(this.methodWriteOne, streamName, fieldName, "JBBPByteOrder." + byteOrder.name());
            } else {
                return String.format(this.methodWriteOne, streamName, fieldName);
            }
        }

        public String makeReaderForArray(final String streamName, final String arraySize, final JBBPByteOrder byteOrder) {
            if (this.multiByte) {
                return String.format(this.methodReadArray, streamName, arraySize, "JBBPByteOrder." + byteOrder.name());
            } else {
                return String.format(this.methodReadArray, streamName, arraySize);
            }
        }

        public String makeWriterForArray(final String streamName, final String fieldName, final String arraySize, final JBBPByteOrder byteOrder) {
            if (this.multiByte) {
                return String.format(this.methodWriteArray, streamName, fieldName, arraySize, "JBBPByteOrder." + byteOrder.name());
            } else {
                return String.format(this.methodWriteArray, streamName, fieldName, arraySize);
            }
        }

        public String makeWriterForArrayWithUnknownSize(final String streamName, final String fieldName, final JBBPByteOrder byteOrder) {
            if (this.multiByte) {
                return String.format(this.methodWriteArrayWithUnknownSize, streamName, fieldName, "JBBPByteOrder." + byteOrder.name());
            } else {
                return String.format(this.methodWriteArrayWithUnknownSize, streamName, fieldName);
            }
        }
    }

    private static class Struct {
        private final String classModifiers;
        private final String className;
        private final Struct parent;
        private final List<Struct> children = new ArrayList<Struct>();
        private final TextBuffer fields = new TextBuffer();
        private final TextBuffer readFunc = new TextBuffer();
        private final TextBuffer writeFunc = new TextBuffer();
        private final JBBPNamedFieldInfo fieldInfo;
        private final String path;

        private Struct(final JBBPNamedFieldInfo fieldInfo, final Struct parent, final String className, final String classModifiers) {
            this.path = parent == null ? "" : parent.path + (parent.path.length() == 0 ? "" : ".") + className.toLowerCase(Locale.ENGLISH);
            this.fieldInfo = fieldInfo;
            this.classModifiers = classModifiers;
            this.className = className;
            this.parent = parent;
            if (this.parent != null) {
                this.parent.children.add(this);
            }
        }

        public boolean isRoot() {
            return this.parent == null;
        }

        public String getPath() {
            return this.path;
        }

        public Struct findRoot() {
            if (this.parent == null) return this;
            return this.parent.findRoot();
        }

        public void write(final TextBuffer buffer, final String extraModifier, final String commonSectionText, final String specialMethods) {
            buffer.indent().printf("%s%sclass %s {%n", this.classModifiers, extraModifier == null ? " " : ' ' + extraModifier + ' ', this.className);
            buffer.incIndent();

            if (commonSectionText != null) {
                buffer.printLinesWithIndent(commonSectionText);
            }

            for (final Struct c : this.children) {
                c.write(buffer, null, null, null);
            }
            buffer.println();

            buffer.printLinesWithIndent(this.fields.toString());
            if (this.parent != null) {
                buffer.indent().println("private final " + findRoot().className + ' ' + ROOT_STRUCT_NAME + ';');
            }
            buffer.println();

            buffer.indent().print("public ").print(this.className).print(" (")
                    .print(this.parent == null ? "" : (findRoot().className + " root"))
                    .println(") {");

            buffer.incIndent();
            if (this.parent != null) {
                buffer.indent().print(ROOT_STRUCT_NAME).print(" = ").println("root;");
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

            buffer.decIndent();
            buffer.indent().println("}");

        }

        public TextBuffer getWriteFunc() {
            return this.writeFunc;
        }

        public TextBuffer getReadFunc() {
            return this.readFunc;
        }

        public TextBuffer getFields() {
            return this.fields;
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
                    return "this." + ROOT_STRUCT_NAME + '.' + (structPath.length() == 0 ? "" : structPath + ".") + info.getFieldName();
                }
            }
        }
    }
}
