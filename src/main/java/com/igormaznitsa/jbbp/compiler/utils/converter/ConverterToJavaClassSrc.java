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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.igormaznitsa.jbbp.compiler.JBBPCompiler.*;

public class ConverterToJavaClassSrc extends AbstractCompiledBlockConverter<ConverterToJavaClassSrc> {

    private static final String ROOT_STRUCT_NAME = "mainRootStruct";

    private enum PrimitiveType {
        BOOL(CODE_BOOL, false, "boolean", "%s.readBoolean()", "%s.readBoolArray(%s)", "%s.write(%s ? 1 : 0)", "for(int I=0;I<%3$s;I++){%1$s.write(%2$s[I] ? 1 : 0);}", "for(int I=0;I<%2$s.length;I++){%1$s.write(%2$s[I] ? 1 : 0);}"),
        BYTE(CODE_BYTE, false, "byte", "(byte)%s.readByte()", "%s.readByteArray(%s)", "%s.write(%s)", "for(int I=0;I<%3$s;I++){%1$s.write(%2$s[I]);}", "for(int I=0;I<%2$s.length;I++){%1$s.write(%2$s[I]);}"),
        UBYTE(CODE_UBYTE, false, "byte", "(byte)%s.readByte()", "%s.readByteArray(%s)", "%s.write(%s)", "for(int I=0;I<%3$s;I++){%1$s.write(%2$s[I] & 0xFF);}", "for(int I=0;I<%2$s.length;I++){%1$s.write(%2$s[I] & 0xFF);}"),
        SHORT(CODE_SHORT, true, "short", "(short)%s.readUnsignedShort(%s)", "%s.readShortArray(%s,%s)", "%s.writeShort(%s,%s)", "for(int I=0;I<%3$s;I++){%1$s.writeShort(%2$s[I],%4$s);}", "for(int I=0;I<%2$s.length;I++){%1$s.writeShort(%2$s[I],%3$s);}"),
        USHORT(CODE_USHORT, true, "char", "(char)%s.readUnsignedShort(%s)", "%s.readUShortArray(%s,%s)", "%s.writeShort(%s,%s)", "for(int I=0;I<%3$s;I++){%1$s.writeShort(%2$s[I],%4$s);}", "for(int I=0;I<%2$s.length;I++){%1$s.writeShort(%2$s[I],%3$s);}"),
        INT(CODE_INT, true, "int", "%s.readInt(%s)", "%s.readIntArray(%s,%s)", "%s.writeInt(%s,%s)", "for(int I=0;I<%3$s;I++){%1$s.writeInt(%2$s[I],%4$s);}", "for(int I=0;I<%2$s.length;I++){%1$s.writeInt(%2$s[I],%3$s);}"),
        LONG(CODE_LONG, true, "long", "%s.readLong(%s)", "%s.readLongArray(%s,%s)", "%s.writeLong(%s,%s)", "for(int I=0;I<%3$s;I++){%1$s.writeLong(%2$s[I],%4$s);}", "for(int I=0;I<%2$s.length;I++){%1$s.writeLong(%2$s[I],%3$s);}");

        private final int code;
        private final String javaType;
        private final String methodReadOne;
        private final String methodReadArray;
        private final String methodWriteOne;
        private final String methodWriteArray;
        private final String methodWriteArrayWithUnknownSize;
        private final boolean multiByte;

        PrimitiveType(final int code, final boolean multiByte, final String javaType, final String readOne, final String readArray, final String writeOne, final String writeArray, final String writeArrayWithUnknownSize) {
            this.code = code;
            this.methodWriteArrayWithUnknownSize = writeArrayWithUnknownSize;
            this.multiByte = multiByte;
            this.javaType = javaType;
            this.methodReadArray = readArray;
            this.methodReadOne = readOne;
            this.methodWriteArray = writeArray;
            this.methodWriteOne = writeOne;
        }

        public String asJavaType() {
            return this.javaType;
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

        public static PrimitiveType findForCode(final int code) {
            for (final PrimitiveType t : values()) {
                if (t.code == code) return t;
            }
            return null;
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
            this.path = parent == null ? "" : parent.path + (parent.path.isEmpty() ? "" : ".") + className.toLowerCase(Locale.ENGLISH);
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

        public JBBPNamedFieldInfo getNamedFieldInfo() {
            return this.fieldInfo;
        }

        public Struct findRoot() {
            if (this.parent == null) return this;
            return this.parent.findRoot();
        }

        public String getClassName() {
            return this.className;
        }

        public Struct getParent() {
            return this.parent;
        }

        public void write(final TextBuffer buffer, final String extraModifier, final String commonSectionText, final String specialMethods) {
            buffer.indent().println(String.format("%s%sclass %s {",this.classModifiers, extraModifier == null ? " " : ' '+extraModifier+' ', this.className));
            buffer.incIndent();

            if (commonSectionText!=null) {
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

            buffer.indent().println(String.format("public %s read(final JBBPBitInputStream In) throws IOException {", this.className));
            buffer.incIndent();
            buffer.printLinesWithIndent(this.readFunc.toString());
            buffer.indent().println("return this;");
            buffer.decIndent();
            buffer.indent().println("}");

            buffer.println();

            buffer.indent().println(String.format("public %s write(final JBBPBitOutputStream Out) throws IOException {", this.className));
            buffer.incIndent();
            buffer.printLinesWithIndent(this.writeFunc.toString());
            buffer.indent().println("return this;");
            buffer.decIndent();
            buffer.indent().println("}");

            if (specialMethods!=null){
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


    private final String packageName;
    private final String className;
    private final AtomicBoolean detectedCustomFields = new AtomicBoolean();
    private final AtomicBoolean detectedExternalFieldsInEvaluator = new AtomicBoolean();
    private final AtomicBoolean detectedVarFields = new AtomicBoolean();
    private final AtomicInteger anonymousFieldCounter = new AtomicInteger();
    private final AtomicInteger specialFieldsCounter = new AtomicInteger();
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

    private Struct getCurrentStruct() {
        return this.structStack.get(0);
    }

    @Override
    public void onConvertStart() {
        this.detectedCustomFields.set(false);
        this.detectedExternalFieldsInEvaluator.set(false);
        this.detectedVarFields.set(false);
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

        if (this.detectedCustomFields.get()) {
            this.specialMethods.println("public abstract JBBPAbstractField readCustomFieldType(JBBPBitInputStream inStrean, JBBPBitOrder bitOrder, int parserFlags, JBBPFieldTypeParameterContainer typeParameterContainer, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue, boolean readWholeStream, int arraySize);");
            this.specialMethods.println();
            this.specialMethods.println("public abstract void writeCustomFieldType(JBBPBitOutputStream outStream, JBBPAbstractField fieldValue, JBBPFieldTypeParameterContainer typeParameterContainer, JBBPNamedFieldInfo nullableNamedFieldInfo, int extraValue, int arraySize);");
        }

        if (this.detectedExternalFieldsInEvaluator.get()) {
            if (!this.specialMethods.isEmpty()){
                this.specialMethods.println();
            }
            this.specialMethods.println("public abstract int getNamedValueForExpression(Object callSource, String valueName);");
        }

        final String specialMethodsText = this.specialMethods.toString();

        this.structStack.get(0).write(buffer,
                this.detectedCustomFields.get() || this.detectedVarFields.get() || this.detectedExternalFieldsInEvaluator.get() ? "abstract" : null,
                this.specialSection.toString(), specialMethodsText.isEmpty() ? null : specialMethodsText);

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
            this.getCurrentStruct().getFields().indent().print(fieldModifier).print(" %s %s;", structType, structName).println();
            processSkipRemainingFlag();
            this.getCurrentStruct().getReadFunc().indent()
                    .print(String.format("if ( this.%1$s == null) { this.%1$s = new %2$s(%3$s);}", structName, structType, this.structStack.size() == 1 ? "this" : "this." + ROOT_STRUCT_NAME))
                    .println(String.format(" this.%s.read(In);", structName));
            this.getCurrentStruct().getWriteFunc().indent().print(structName).println(".write(Out);");
        } else {
            this.getCurrentStruct().getFields().indent().print(fieldModifier).print(" %s [] %s;", structType, structName).println();
            processSkipRemainingFlag();
            if ("-1".equals(arraySizeIn)) {
                this.getCurrentStruct().getReadFunc().indent()
                        .println(String.format("List<%3$s> __%1$s_tmplst__ = new ArrayList<%3$s>(); while (In.hasAvailableData()){ __%1$s_tmplst__.add(new %3$s(%4$s).read(In));} this.%1$s = __%1$s_tmplst__.toArray(new %3$s[__%1$s_tmplst__.size()]);__%1$s_tmplst__ = null;", structName, arraySizeIn, structType, (this.structStack.size() == 1 ? "this" : ROOT_STRUCT_NAME)));
                this.getCurrentStruct().getWriteFunc().indent().print(String.format("for (int I=0;I<this.%1$s.length;I++){ this.%1$s[I].write(Out); }",structName));
            } else {
                this.getCurrentStruct().getReadFunc().indent()
                        .print("if (").print(String.format("this.%1$s == null || this.%1$s.length != %2$s){ this.%1$s = new %3$s[%2$s]; for(int I=0;I<%2$s;I++){ this.%1$s[I] = new %3$s(%4$s);}}", structName, arraySizeIn, structType, (this.structStack.size() == 1 ? "this" : "this." + ROOT_STRUCT_NAME)))
                        .println(String.format("for (int I=0;I<%2$s;I++){ this.%1$s[I].read(in); }", structName, arraySizeIn));
                this.getCurrentStruct().getWriteFunc().indent().print(String.format("for (int I=0;I<%2$s;I++){ this.%1$s[I].write(Out); }",structName, arraySizeOut));
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

        final String arraySizeIn = nullableArraySize == null ? null : evaluatorToString("In", offsetInCompiledBlock, nullableArraySize, this.detectedExternalFieldsInEvaluator);
        final String arraySizeOut = nullableArraySize == null ? null : evaluatorToString("Out", offsetInCompiledBlock, nullableArraySize, this.detectedExternalFieldsInEvaluator);

        final PrimitiveType type = PrimitiveType.findForCode(primitiveType);

        final String fieldModifier;
        if (nullableNameFieldInfo == null) {
            fieldModifier = "protected";
        } else {
            fieldModifier = "public";
        }

        processSkipRemainingFlag();
        if (nullableArraySize == null) {
            getCurrentStruct().getFields().println(String.format("%s %s %s;", fieldModifier, type.asJavaType(), fieldName));
            getCurrentStruct().getReadFunc().println(String.format("this.%s = %s;", fieldName, type.makeReaderForSingleField("In", byteOrder)));
            getCurrentStruct().getWriteFunc().print(type.makeWriterForSingleField("Out", "this." + fieldName, byteOrder)).println(";");
        } else {
            getCurrentStruct().getFields().println(String.format("%s %s [] %s;", fieldModifier, type.asJavaType(), fieldName));
            getCurrentStruct().getReadFunc().println(String.format("this.%s = %s;", fieldName, type.makeReaderForArray("In", arraySizeIn, byteOrder)));
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
        final String javaFieldType = "byte";

        String sizeOfFieldIn = evaluatorToString("In",offsetInCompiledBlock, notNullFieldSize, this.detectedExternalFieldsInEvaluator);
        String sizeOfFieldOut = evaluatorToString("Out",offsetInCompiledBlock, notNullFieldSize, this.detectedExternalFieldsInEvaluator);
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
            fieldModifier = "protected ";
        } else {
            fieldModifier = "public ";
        }

        processSkipRemainingFlag();

        if (arraySizeIn == null) {
            getCurrentStruct().getReadFunc().indent().println(String.format("this.%s = In.readBitField(%s);", fieldName, sizeOfFieldIn));
        } else {
            getCurrentStruct().getReadFunc().indent().print(fieldName).print(" = In.readBitsArray(").print(arraySizeIn).print(",").print(sizeOfFieldIn).println(");");
        }

        if (arraySizeOut == null) {
            getCurrentStruct().getWriteFunc().indent().println(String.format("Out.writeBits(this.%s,%s);", fieldName, sizeOfFieldOut));
        } else {
            if ("-1".equals(arraySizeIn)) {
                getCurrentStruct().getWriteFunc().indent().print("for(int I=0; I<").print(fieldName).print(".length").print(";I++)").println(String.format(" Out.writeBits(this.%s[I],%s);", fieldName, sizeOfFieldOut));
            } else {
                getCurrentStruct().getWriteFunc().indent().print("for(int I=0; I<").print(arraySizeOut).print("; I++)").println(String.format(" Out.writeBits(this.%s[I],%s);", fieldName, sizeOfFieldOut));
            }
        }

        getCurrentStruct().getFields().indent().print(fieldModifier).print(javaFieldType).print(" ").print(nullableArraySize == null ? "" : "[] ").print(fieldName).println(";").println();
    }

    private String makeAnonymousFieldName() {
        return "_AnoField" + this.anonymousFieldCounter.getAndIncrement();
    }

    private String makeSpecialFieldName() {
        return "_SpecField" + this.specialFieldsCounter.getAndIncrement();
    }

    private String makeAnonymousStructName() {
        return "_AnoStruct" + this.anonymousFieldCounter.getAndIncrement();
    }

    @Override
    public void onCustom(final int offsetInCompiledBlock, final JBBPFieldTypeParameterContainer notNullfieldType, final JBBPNamedFieldInfo nullableNameFieldInfo, final JBBPByteOrder byteOrder, final boolean readWholeStream, final JBBPIntegerValueEvaluator nullableArraySizeEvaluator, final JBBPIntegerValueEvaluator extraDataValueEvaluator) {
        this.detectedCustomFields.set(true);


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


        this.getCurrentStruct().getFields().println(String.format("%s JBBPAbstractField %s;", fieldModifier, fieldName));

        if (nullableNameFieldInfo != null) {
            this.specialSection.println(String.format("private static final JBBPNamedFieldInfo %s = %s;",
                    specialFieldName_fieldNameInfo,
                    "new JBBPNamedFieldInfo(\"" + nullableNameFieldInfo.getFieldName() + "\",\"" + nullableNameFieldInfo.getFieldPath() + "\"," + nullableNameFieldInfo.getFieldOffsetInCompiledBlock() + ")"
            ));
        }

        this.specialSection.println(String.format("private static final JBBPFieldTypeParameterContainer %s = %s;",
                specialFieldName_typeParameterContainer,
                "new JBBPFieldTypeParameterContainer(JBBPByteOrder." + notNullfieldType.getByteOrder().name() + ",\"" + notNullfieldType.getTypeName() + "\"," + notNullfieldType.getExtraData() + ")"
        ));

        processSkipRemainingFlag();
        this.getCurrentStruct().getReadFunc().println(String.format("%s = %s;",
                fieldName,
                String.format("%s.readCustomFieldType(In, In.getBitOrder(), %d, %s, %s, %s, %b, %s);",
                        this.getCurrentStruct().isRoot() ? "this" : "this." + ROOT_STRUCT_NAME,
                        this.parserFlags,
                        specialFieldName_typeParameterContainer,
                        nullableNameFieldInfo == null ? "null" : specialFieldName_fieldNameInfo,
                        extraDataValueEvaluator == null ? "0" : evaluatorToString("In", offsetInCompiledBlock, extraDataValueEvaluator, this.detectedExternalFieldsInEvaluator),
                        readWholeStream,
                        nullableArraySizeEvaluator == null ? "-1" : evaluatorToString("In", offsetInCompiledBlock, nullableArraySizeEvaluator, this.detectedExternalFieldsInEvaluator)
                )
        ));

        this.getCurrentStruct().getWriteFunc().println(String.format("%s;",
                String.format("%s.writeCustomFieldType(Out, %s, %s, %s, %s, %s);",
                        this.getCurrentStruct().isRoot() ? "this" : "this." + ROOT_STRUCT_NAME,
                        "this." + fieldName,
                        specialFieldName_typeParameterContainer,
                        nullableNameFieldInfo == null ? "null" : specialFieldName_fieldNameInfo,
                        extraDataValueEvaluator == null ? "0" : evaluatorToString("Out", offsetInCompiledBlock, extraDataValueEvaluator, this.detectedExternalFieldsInEvaluator),
                        nullableArraySizeEvaluator == null ? "-1" : evaluatorToString("Out", offsetInCompiledBlock, nullableArraySizeEvaluator, this.detectedExternalFieldsInEvaluator)
                )
        ));
   }

    @Override
    public void onVar(final int offsetInCompiledBlock, final JBBPNamedFieldInfo nullableNameFieldInfo, final JBBPByteOrder byteOrder, final JBBPIntegerValueEvaluator nullableArraySize) {
        this.detectedVarFields.set(true);
        //TODO to do

    }

    private String evaluatorToString(final String streamName, final int offsetInBlock, final JBBPIntegerValueEvaluator evaluator, final AtomicBoolean detectedExternalField) {
        final StringBuilder buffer = new StringBuilder();

        detectedExternalField.set(false);

        final ExpressionEvaluatorVisitor visitor = new ExpressionEvaluatorVisitor() {
            private final List<Object> stack = new ArrayList<Object>();

            @Override
            public ExpressionEvaluatorVisitor begin() {
                this.stack.clear();
                return this;
            }

            @Override
            public ExpressionEvaluatorVisitor visit(final Special specialField) {
                stack.add(specialField);
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

            private String argToString(final Object obj) {
                if (obj instanceof Special) {
                    switch ((Special) obj) {
                        case STREAM_COUNTER:
                            return "(int)" + streamName + ".getCounter()";
                        default:
                            throw new Error("Unexpected special");
                    }
                } else if (obj instanceof Integer) {
                    return obj.toString();
                } else if (obj instanceof String) {
                    return String.format("%s.getNamedValueForExpression(this, \"%s\")", (getCurrentStruct().isRoot() ?  "this" : "this."+ROOT_STRUCT_NAME), obj.toString());
                } else if (obj instanceof JBBPNamedFieldInfo) {
                    final String pathToCurrentStruct = getCurrentStruct().getPath();
                    final String fieldPath = ((JBBPNamedFieldInfo) obj).getFieldPath();
                    if (fieldPath.startsWith(pathToCurrentStruct)) {
                        String raw = fieldPath.substring(pathToCurrentStruct.length());
                        if (raw.startsWith(".")) {
                            raw = raw.substring(1);
                        }
                        return "this." + raw;
                    } else {
                        return "this." + ROOT_STRUCT_NAME + '.' + fieldPath;
                    }
                }
                throw new Error("Unexpected object : " + obj);
            }

            @Override
            public ExpressionEvaluatorVisitor end() {
                // process operators
                Operator lastOp = null;

                final List<String> values = new ArrayList<String>();

                for (int i = 0; i < this.stack.size(); i++) {
                    final Object cur = this.stack.get(i);
                    if (cur instanceof Operator) {
                        final Operator op = (Operator) cur;

                        if (lastOp != null && lastOp.getPriority() < op.getPriority()) {
                            buffer.insert(0, '(').append(')');
                        }

                        if (op.getArgsNumber() <= values.size()) {
                            if (op.getArgsNumber() == 1) {
                                buffer.append(op.getText()).append(values.remove(values.size() - 1));
                            } else {
                                buffer.append(values.remove(values.size() - 2)).append(op.getText()).append(values.remove(values.size() - 1));
                            }
                        } else {
                            buffer.append(op.getText()).append(values.remove(values.size() - 1));
                        }

                        lastOp = op;
                    } else {
                        values.add(argToString(cur));
                    }
                }

                if (!values.isEmpty()) {
                    buffer.append(values.get(0));
                }

                return this;
            }
        };

        evaluator.visit(this.compiledBlock, offsetInBlock, visitor);

        return buffer.toString();
    }


    @Override
    public void onActionItem(final int offsetInCompiledBlock, final int actionType, final JBBPIntegerValueEvaluator nullableArgument) {
        final String valueTxtIn = nullableArgument == null ? null : evaluatorToString("In", offsetInCompiledBlock, nullableArgument, this.detectedExternalFieldsInEvaluator);
        final String valueTxtOut = nullableArgument == null ? null : evaluatorToString("Out", offsetInCompiledBlock, nullableArgument, this.detectedExternalFieldsInEvaluator);

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
}
