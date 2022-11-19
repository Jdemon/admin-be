package com.jpos.adm.core.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jpos.adm.core.annotation.NotEditable;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ClazzUtil {

    public static final String TEXT = "text";
    public static final String NUMBER = "number";
    public static final String DATETIME = "datetime";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE = "date";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final ObjectMapper mapper = new ObjectMapper();
    public static Field[] getAllFields(Class<?> clazz) {
        if (clazz != null) {
            Field[] fields = clazz.getDeclaredFields();
            fields = ArrayUtils.addAll(fields, getAllFields(clazz.getSuperclass()));
            Arrays.sort(
                    fields,
                    Comparator.comparing(
                            field -> !field.getName().equalsIgnoreCase(ModelConstant.ID)
                    )
            );
            return fields;
        }
        return new Field[0];
    }

    public static JsonNode entitySchema(Class<?> clazz) {
        ObjectNode result = mapper.createObjectNode();
        String tableName = tableName(clazz);
        Field[] fields = getAllFields(clazz);
        if (fields == null || fields.length < 1) {
            return result;
        }
        List<ObjectNode> listNode = new ArrayList<>();
        for (Field field : fields) {
            ObjectNode fieldNode = mapper.createObjectNode();
            fieldNode.put("fieldName", field.getName());
            fieldNode.put("tableName", tableName);
            fieldNode.put("displayName", camelToTitleCase(field.getName()));
            fieldNode.put("editable", !field.isAnnotationPresent(NotEditable.class));
            prepareType(fieldNode, field);
            listNode.add(fieldNode);
        }
        result.putArray(tableName).addAll(listNode);
        return result;
    }

    private static void prepareType(ObjectNode fieldNode, Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null) {
            fieldNode.put("nullable", column.nullable());
            fieldNode.put("size", column.length());
        }

        if (field.getAnnotation(Id.class) != null) {
            fieldNode.put("isPrimaryKey", field.getAnnotation(Id.class) != null);
        }
        if (!field.getType().getSimpleName().equals("Serializable")) {
            if (field.getType().isAssignableFrom(int.class) ||
                    field.getType().isAssignableFrom(long.class) ||
                    field.getType().isAssignableFrom(double.class) ||
                    field.getType().isAssignableFrom(float.class) ||
                    field.getType().isAssignableFrom(Number.class) ||
                    field.getType().isAssignableFrom(Integer.class) ||
                    field.getType().isAssignableFrom(Float.class) ||
                    field.getType().isAssignableFrom(Long.class) ||
                    field.getType().isAssignableFrom(Double.class) ||
                    field.getType().isAssignableFrom(BigDecimal.class) ||
                    field.getType().isAssignableFrom(BigInteger.class)
            ) {
                fieldNode.put("type", NUMBER);
                if (column != null) {
                    if (column.precision() != 0) fieldNode.put("precision", column.precision());
                    if (column.scale() != 0) fieldNode.put("scale", column.scale());
                }
                return;
            } else if (field.getType().isAssignableFrom(LocalDateTime.class)) {
                fieldNode.put("format", DATETIME_FORMAT);
                fieldNode.put("type", DATETIME);
                return;
            } else if (field.getType().isAssignableFrom(LocalDate.class)) {
                fieldNode.put("format", DATE_FORMAT);
                fieldNode.put("type", DATE);
                return;
            }
        }
        fieldNode.put("type", TEXT);
    }

    public static Field[] removeAuditableFields(Field[] fields) {
        return ClazzUtil.removeFieldsByName(
                fields,
                ModelConstant.CREATED_BY,
                ModelConstant.CREATED_AT,
                ModelConstant.UPDATED_BY,
                ModelConstant.UPDATED_AT
        );
    }

    public static Field[] removeFieldsByName(Field[] fields, String... names) {
        List<Field> fieldList = new ArrayList<>();
        for (Field field : fields) {
            boolean isRemove = false;
            for (String name : names) {
                if (field.getName().equals(name)) {
                    isRemove = true;
                    break;
                }
            }
            if (!isRemove) {
                fieldList.add(field);
            }
        }
        return fieldList.toArray(new Field[0]);
    }

    public static String camelToTitleCase(String fieldName) {
        if (StringUtils.isNotBlank(fieldName)) {
            return StringUtils.capitalize(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(fieldName), StringUtils.SPACE));
        }
        return fieldName;
    }

    public static String titleToCamelCase(String fieldName) {
        if (StringUtils.isNotBlank(fieldName)) {
            fieldName = StringUtils.replace(fieldName, StringUtils.SPACE, StringUtils.EMPTY);
            fieldName = Character.toLowerCase(fieldName.toCharArray()[0]) + StringUtils.substring(fieldName, 1);
            return fieldName;
        }
        return fieldName;
    }

    public static String tableName(Class<?> clazz) {
        String tableName = clazz.getSimpleName();
        javax.persistence.Table table = clazz.getAnnotation(Table.class);
        if (table != null) {
            tableName = table.name();
        }
        return tableName.toLowerCase();
    }
}