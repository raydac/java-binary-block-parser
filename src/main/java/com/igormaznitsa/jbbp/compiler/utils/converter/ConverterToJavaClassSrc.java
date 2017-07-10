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

    private static final String ROOT_STRUCT_NAME = "__root_struct_1975__";

    private enum PrimitiveType {
        BOOL(CODE_BOOL, false, "boolean", "%s.readBoolean()", "%s.readBoolArray(%s)", "%s.write(%s ? 1 : 0)", "for(int i=0;i<%3$s;i++){%1$s.write(%2$s[i] ? 1 : 0);}"),
        BYTE(CODE_BYTE, false, "byte", "(byte)%s.readByte()", "%s.readByteArray(%s)", "%s.write(%s)", "for(int i=0;i<%3$s;i++){%1$s.write(%2$s[i]);}"),
        UBYTE(CODE_UBYTE, false, "byte", "(byte)%s.readByte()", "%s.readByteArray(%s)", "%s.write(%s)", "for(int i=0;i<%3$s;i++){%1$s.write(%2$s[i] & 0xFF);}"),
        SHORT(CODE_SHORT, true, "short", "(short)%s.readUnsignedShort(%s)", "%s.readShortArray(%s,%s)", "%s.writeShort(%s,%s)", "for(int i=0;i<%3$s;i++){%1$s.writeShort(%2$s[i],%4$s);}"),
        USHORT(CODE_USHORT, true, "char", "(char)%s.readUnsignedShort(%s)", "%s.readUShortArray(%s,%s)", "%s.writeShort(%s,%s)", "for(int i=0;i<%3$s;i++){%1$s.writeShort(%2$s[i],%4$s);}"),
        INT(CODE_INT, true, "int", "%s.readInt(%s)", "%s.readIntArray(%s,%s)", "%s.writeInt(%s,%s)", "for(int i=0;i<%3$s;i++){%1$s.writeInt(%2$s[i],%4$s);}"),
        LONG(CODE_LONG, true, "long", "%s.readLong(%s)", "%s.readLongArray(%s,%s)", "%s.writeLong(%s,%s)", "for(int i=0;i<%3$s;i++){%1$s.writeLong(%2$s[i],%4$s);}");

        private final int code;
        private final String javaType;
        private final String methodReadOne;
        private final String methodReadArray;
        private final String methodWriteOne;
        private final String methodWriteArray;
        private final boolean multiByte;

        PrimitiveType(final int code, final boolean multiByte, final String javaType, final String readOne, final String readArray, final String writeOne, final String writeArray) {
            this.code = code;
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

        public void write(final TextBuffer buffer) {
            buffer.indent().print(this.classModifiers).print(" class ").print(this.className).println(" {");
            buffer.incIndent();

            for (final Struct c : this.children) {
                c.write(buffer);
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

            buffer.indent().println(String.format("public %s read(final JBBPBitInputStream in) throws IOException {", this.className));
            buffer.incIndent();
            buffer.printLinesWithIndent(this.readFunc.toString());
            buffer.indent().println("return this;");
            buffer.decIndent();
            buffer.indent().println("}");

            buffer.println();

            buffer.indent().println(String.format("public %s write(final JBBPBitOutputStream out) throws IOException {", this.className));
            buffer.incIndent();
            buffer.printLinesWithIndent(this.writeFunc.toString());
            buffer.indent().println("return this;");
            buffer.decIndent();
            buffer.indent().println("}");

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
    private final List<Struct> structStack = new ArrayList<Struct>();
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
        this.structStack.clear();

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
        buffer.println("import java.io.IOException;");
        buffer.println("import java.util.*;");

        buffer.println();

        this.structStack.get(0).write(buffer);
        this.result = buffer.toString();
    }

    @Override
    public void onStructStart(final int offsetInCompiledBlock, final JBBPNamedFieldInfo nullableNameFieldInfo, final JBBPIntegerValueEvaluator nullableArraySize) {
        final String structName = (nullableNameFieldInfo == null ? makeAnonymousStructName() : nullableNameFieldInfo.getFieldName()).toLowerCase(Locale.ENGLISH);
        final String structType = structName.toUpperCase(Locale.ENGLISH);
        final String arraySize = nullableArraySize == null ? null : evaluatorToString(offsetInCompiledBlock, nullableArraySize, this.detectedExternalFieldsInEvaluator);
        final Struct newStruct = new Struct(nullableNameFieldInfo, this.getCurrentStruct(), structType, "public static");

        final String fieldModifier;
        if (nullableNameFieldInfo == null) {
            fieldModifier = "protected";
        } else {
            fieldModifier = "public";
        }

        if (arraySize == null) {
            this.getCurrentStruct().getFields().indent().print(fieldModifier).print(" %s %s;", structType, structName).println();
            processSkipRemainingFlag();
            this.getCurrentStruct().getReadFunc().indent()
                    .print(String.format("if ( this.%1$s == null) { this.%1$s = new %2$s(%3$s);}", structName, structType, this.structStack.size() == 1 ? "this" : "this." + ROOT_STRUCT_NAME))
                    .println(String.format(" this.%s.read(in);", structName));
            this.getCurrentStruct().getWriteFunc().indent().print(structName).println(".write(out);");
        } else {
            this.getCurrentStruct().getFields().indent().print(fieldModifier).print(" %s [] %s;", structType, structName).println();
            processSkipRemainingFlag();
            if ("-1".equals(arraySize)) {
                this.getCurrentStruct().getReadFunc().indent()
                        .println(String.format("List<%3$s> __%1$s_tmplst__ = new ArrayList<%3$s>(); while (in.hasAvailableData()){ __%1$s_tmplst__.add(new %3$s(%4$s).read(in));} this.%1$s = __%1$s_tmplst__.toArray(new %3$s[__%1$s_tmplst__.size()]);__%1$s_tmplst__ = null;", structName, arraySize, structType, (this.structStack.size() == 1 ? "this" : ROOT_STRUCT_NAME)));
            } else {
                this.getCurrentStruct().getReadFunc().indent()
                        .print("if (").print(String.format("this.%1$s == null || this.%1$s.length != %2$s){ this.%1$s = new %3$s[%2$s]; for(int i=0;i<%2$s;i++){ this.%1$s[i] = new %3$s(%4$s);}}", structName, arraySize, structType, (this.structStack.size() == 1 ? "this" : "this." + ROOT_STRUCT_NAME)))
                        .println(String.format("for (int i=0;i<%2$s;i++){ this.%1$s[i].read(in); }", structName, arraySize));
            }
        }


        this.structStack.add(0, newStruct);
    }

    private void processSkipRemainingFlag(){
        if (this.isFlagSkipRemainingFieldsIfEOF()){
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

        final String arraySize = nullableArraySize == null ? null : evaluatorToString(offsetInCompiledBlock, nullableArraySize, this.detectedExternalFieldsInEvaluator);

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
            getCurrentStruct().getReadFunc().println(String.format("this.%s = %s;", fieldName, type.makeReaderForSingleField("in", byteOrder)));
            getCurrentStruct().getWriteFunc().print(type.makeWriterForSingleField("out", "this." + fieldName, byteOrder)).println(";");
        } else {
            if ("-1".equals(nullableArraySize)) {

            } else {
                getCurrentStruct().getFields().println(String.format("%s %s [] %s;", fieldModifier, type.asJavaType(), fieldName));
                getCurrentStruct().getReadFunc().println(String.format("this.%s = %s;", fieldName, type.makeReaderForArray("in", arraySize, byteOrder)));
                getCurrentStruct().getWriteFunc().print(type.makeWriterForArray("out", "this." + fieldName, arraySize, byteOrder)).println(";");
            }
        }
    }

    @Override
    public void onBitField(final int offsetInCompiledBlock, final JBBPNamedFieldInfo nullableNameFieldInfo, final JBBPIntegerValueEvaluator notNullFieldSize, final JBBPIntegerValueEvaluator nullableArraySize) {
        final String fieldName = nullableNameFieldInfo == null ? makeAnonymousFieldName() : nullableNameFieldInfo.getFieldName();
        final String javaFieldType = "byte";

        String sizeOfField = evaluatorToString(offsetInCompiledBlock, notNullFieldSize, this.detectedExternalFieldsInEvaluator);
        try {
            sizeOfField = "JBBPBitNumber." + JBBPBitNumber.decode(Integer.parseInt(sizeOfField)).name();
        } catch (NumberFormatException ex) {
            sizeOfField = "JBBPBitNumber.decode(" + sizeOfField + ')';
        }

        final String arraySize = nullableArraySize == null ? null : evaluatorToString(offsetInCompiledBlock, nullableArraySize, this.detectedExternalFieldsInEvaluator);

        final String fieldModifier;
        if (nullableNameFieldInfo == null) {
            fieldModifier = "protected ";
        } else {
            fieldModifier = "public ";
        }

        processSkipRemainingFlag();
        if (arraySize == null) {
            getCurrentStruct().getReadFunc().indent().println(String.format("this.%s = in.readBitField(%s);", fieldName, sizeOfField));
            getCurrentStruct().getWriteFunc().indent().println(String.format("out.writeBits(this.%s,%s);", fieldName, sizeOfField));
        } else {
            getCurrentStruct().getReadFunc().indent().print(fieldName).print(" = in.readBitsArray(").print(arraySize).print(",").print(sizeOfField).println(");");
            getCurrentStruct().getWriteFunc().indent().print("for(int i=0;i<").print(arraySize).print(";i++)").println(String.format(" out.writeBits(this.%s[i],%s);", fieldName, sizeOfField));
        }

        getCurrentStruct().getFields().indent().print(fieldModifier).print(javaFieldType).print(" ").print(nullableArraySize == null ? "" : "[] ").print(fieldName).println(";").println();
    }

    private String makeAnonymousFieldName() {
        return "_a_field" + this.anonymousFieldCounter.getAndIncrement();
    }

    private String makeAnonymousStructName() {
        return "_a_struct" + this.anonymousFieldCounter.getAndIncrement();
    }

    @Override
    public void onCustom(final int offsetInCompiledBlock, final JBBPFieldTypeParameterContainer notNullfieldType, final JBBPNamedFieldInfo nullableNameFieldInfo, final JBBPByteOrder byteOrder, final boolean readWholeStream, final JBBPIntegerValueEvaluator nullableArraySizeEvaluator, final JBBPIntegerValueEvaluator extraDataValueEvaluator) {
//        this.detectedCustomFields.set(true);
//
//        final String fieldModifier;
//        if (nullableNameFieldInfo == null) {
//            fieldModifier = "protected ";
//        } else {
//            fieldModifier = "public ";
//        }
//
//        final TextBuffer fieldOut = this.structStack.isEmpty() ? this.mainFields : this.structs;
//        final String fieldName = nullableNameFieldInfo == null ? "_afield" + anonymousFieldCounter.getAndIncrement() : nullableNameFieldInfo.getFieldName();
//
//        fieldOut.println(nullableNameFieldInfo == null ? "// an anonymous field" : "// the named field '" + nullableNameFieldInfo.getFieldName() + '\'');
//        fieldOut.print(fieldModifier).print("JBBPAbstractField ").print(fieldName).println(";").println();
//
//        final String jbbpNFI = nullableNameFieldInfo == null ? null : "new JBBPNamedFieldInfo(\"" + nullableNameFieldInfo.getFieldName() + "\",\"" + nullableNameFieldInfo.getFieldPath() + "\"," + nullableNameFieldInfo.getFieldOffsetInCompiledBlock() + ")";
//        final String jbbpFTPC = "new JBBPFieldTypeParameterContainer(JBBPByteOrder." + notNullfieldType.getByteOrder().name() + "," + toJStr(notNullfieldType.getTypeName()) + "," + toJStr(notNullfieldType.getExtraData()) + ")";
//
//        if (jbbpNFI != null) {
//            this.staticFields.print("private static final JBBPNamedFieldInfo __nfi_").print(fieldName).print(" = ").print(jbbpNFI).println(";");
//        }
//        this.staticFields.print("private static final JBBPFieldTypeParameterContainer __ftpc_").print(fieldName).print(" = ").print(jbbpFTPC).println(";");
//
//        this.readFields
//                .print(path2var(fieldName))
//                .print(" = ")
//                .print("this.__cftProcessor.readCustomFieldType(theStream,theStream.getBitOrder()")
//                .print(",").print(this.parserFlags)
//                .print(",").print("${className}.__ftpc_").print(fieldName)
//                .print(",").print(jbbpNFI == null ? "null" : "${className}.__nfi_" + fieldName)
//                .print(",").print(extraDataValueEvaluator == null ? "0" : evaluatorToString(offsetInCompiledBlock, extraDataValueEvaluator, this.detectedExternalFieldsInEvaluator))
//                .print(",").print(readWholeStream)
//                .print(",").print(nullableArraySizeEvaluator == null ? "-1" : evaluatorToString(offsetInCompiledBlock, nullableArraySizeEvaluator, this.detectedExternalFieldsInEvaluator))
//                .println(");");
    }

    @Override
    public void onVar(final int offsetInCompiledBlock, final JBBPNamedFieldInfo nullableNameFieldInfo, final JBBPByteOrder byteOrder, final JBBPIntegerValueEvaluator nullableArraySize) {
        this.detectedVarFields.set(true);
    }

    private String evaluatorToString(final int offsetInBlock, final JBBPIntegerValueEvaluator evaluator, final AtomicBoolean detectedExternalField) {
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
                            return "(int)theStream.getCounter()";
                        default:
                            throw new Error("Unexpected special");
                    }
                } else if (obj instanceof Integer) {
                    return obj.toString();
                } else if (obj instanceof String) {
                    return "this.getValueForName(\"" + obj.toString() + "\")";
                } else if (obj instanceof JBBPNamedFieldInfo) {
                    final String pathToCurrentStruct = getCurrentStruct().getPath();
                    final String fieldPath = ((JBBPNamedFieldInfo) obj).getFieldPath();
                    if (fieldPath.startsWith(pathToCurrentStruct)) {
                        String raw = fieldPath.substring(pathToCurrentStruct.length());
                        if (raw.startsWith(".")) {
                            raw = raw.substring(1);
                        }
                        return "this."+raw;
                    } else {
                        return "this."+ROOT_STRUCT_NAME+'.'+fieldPath;
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
        final String valueTxt = nullableArgument == null ? null : evaluatorToString(offsetInCompiledBlock, nullableArgument, this.detectedExternalFieldsInEvaluator);

        switch (actionType) {
            case JBBPCompiler.CODE_RESET_COUNTER: {
                getCurrentStruct().getReadFunc().println("in.resetCounter();");
                getCurrentStruct().getWriteFunc().println("out.resetCounter();");
            }
            break;
            case JBBPCompiler.CODE_ALIGN: {
                getCurrentStruct().getReadFunc().indent().print("in.align(").print(valueTxt).println(");");
                getCurrentStruct().getWriteFunc().indent().print("out.align(").print(valueTxt).println(");");
            }
            break;
            case JBBPCompiler.CODE_SKIP: {
                getCurrentStruct().getReadFunc().indent().print("in.skip(").print(valueTxt).println(");");
                getCurrentStruct().getWriteFunc().indent().print("for(int i=0; i<").print(valueTxt).println(";i++) out.write(0);");
            }
            break;
            default: {
                throw new Error("Detected unknown action, contact developer!");
            }
        }
    }
}
