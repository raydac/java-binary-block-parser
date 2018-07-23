package jbbp.io;

import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Binary {
    default List<Field> getSortedBinFieldList() {
        Class clazz = this.getClass();

        Stream<Field> fields = Arrays.stream(clazz.getDeclaredFields());

        Stream<Field> binFields = fields.filter(f -> Arrays.stream(f.getDeclaredAnnotations()).anyMatch(a -> a instanceof Bin));

        Comparator<Bin> binComparator = Comparator.comparing(Bin::outOrder);
        Comparator<Field> binFieldComparator = Comparator.comparing(f -> f.getDeclaredAnnotation(Bin.class), binComparator);

        Stream<Field> sortedBinFields = binFields.sorted(binFieldComparator);

        List<Field> sortedBinFieldList = sortedBinFields.collect(Collectors.toList());

        return sortedBinFieldList;
    }

    default String getFormat() {
        List<Field> sortedBinFields = getSortedBinFieldList();

        String format = sortedBinFields.stream().map(this::getString)
                .collect(Collectors.joining(" "));

        return format;
    }

    default String getString(Field field) {
        try {
            boolean array = false;

            Class<?> clazz = field.getType();

            if (clazz.isArray()) {
                array = true;
                clazz = clazz.getComponentType();
            }

            StringBuilder stringBuilder = new StringBuilder();
            String name = field.getName();

            if (Binary.class.isAssignableFrom(clazz)) {
                // Embedded object
                stringBuilder.append(name);
                stringBuilder.append(" ");
                addArrayIndicatorIfNecessary(array, stringBuilder);
                stringBuilder.append(" { ");
                Object a = field.get(this);
                Object b = clazz.newInstance();
                stringBuilder.append(((Binary) b).getFormat());
                stringBuilder.append(" } ");
            } else {
                Bin bin = field.getDeclaredAnnotation(Bin.class);
                BinType binType = bin.type();
                String arraySize = "_";

                if (!bin.extra().isEmpty()) {
                    arraySize = bin.extra();
                }

                String binTypeString = binType.toString().replace("_ARRAY", "[" + arraySize + "]");
                stringBuilder.append(binTypeString);
                stringBuilder.append(" ");
                stringBuilder.append(name);
                stringBuilder.append(";");
            }

            return stringBuilder.toString();
        } catch (IllegalAccessException e) {
            // THIS IS A BUG
            throw new UnsupportedOperationException(e);
        } catch (InstantiationException e) {
            // THIS IS A BUG
            throw new UnsupportedOperationException(e);
        }
    }

    default void addArrayIndicatorIfNecessary(boolean array, StringBuilder stringBuilder) {
        if (array) {
            stringBuilder.append("[_]");
        }
    }
}