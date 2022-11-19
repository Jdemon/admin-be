package com.jpos.adm.core.excel;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jpos.adm.core.exception.ExcelException;
import com.jpos.adm.core.extension.ClazzUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

@Slf4j
public class ExcelImporter<T> {

    private final Class<T> clazz;
    private final Map<String, String> fields = new HashMap<>();
    private static ObjectMapper mapper;

    public ExcelImporter(Class<T> clazz) {
        this.clazz = clazz;
        mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
    }

    public List<T> importExcel(byte[] fileData) {
        try (ByteArrayInputStream is = new ByteArrayInputStream(fileData)) {
            XSSFWorkbook workbook = new XSSFWorkbook(is);

            Sheet sheet = workbook.getSheet(ClazzUtil.tableName(clazz));
            Iterator<Row> rows = sheet.iterator();

            List<T> t = new ArrayList<>();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();

                Iterator<Cell> cellsInRow = currentRow.iterator();

                // skip header
                if (rowNumber == 0) {
                    readHeaderLine(cellsInRow);
                } else {
                    t.add(readDataLine(cellsInRow));
                }
                rowNumber++;
            }
            return t;
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new ExcelException("fail to parse Excel file.", e);
        }
    }

    private void readHeaderLine(Iterator<Cell> cellsInRow) {
        while (cellsInRow.hasNext()) {
            Cell currentCell = cellsInRow.next();
            String column = ClazzUtil.titleToCamelCase(currentCell.getStringCellValue());
            this.fields.put(String.valueOf(currentCell.getColumnIndex()), column);
        }
    }

    private T readDataLine(Iterator<Cell> cellsInRow) {
        ObjectNode objectNode = mapper.createObjectNode();
        while (cellsInRow.hasNext()) {
            Cell currentCell = cellsInRow.next();
            CellType type = currentCell.getCellType();
            String columnIndex = String.valueOf(currentCell.getColumnIndex());
            if (!fields.containsKey(columnIndex)) {
                continue;
            }
            String fieldName = fields.get(columnIndex);
            if (type.equals(CellType.BOOLEAN)) {
                objectNode.put(fieldName, currentCell.getBooleanCellValue());
            } else if (type.equals(CellType.NUMERIC)) {
                if (fieldName.contains("At") || fieldName.contains("Date")) {
                    objectNode.put(fieldName, currentCell.getLocalDateTimeCellValue().toString());
                } else {
                    objectNode.put(fieldName, currentCell.getNumericCellValue());
                }
            } else {
                objectNode.set(fieldName, mapper.convertValue(currentCell.getStringCellValue(), JsonNode.class));
            }
        }
        return mapper.convertValue(objectNode, clazz);
    }
}
