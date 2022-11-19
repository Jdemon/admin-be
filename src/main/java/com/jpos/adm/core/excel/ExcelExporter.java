package com.jpos.adm.core.excel;

import com.jpos.adm.core.exception.ExcelException;
import com.jpos.adm.core.extension.ClazzUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.List;

@Slf4j
@Valid
public class ExcelExporter<T> {

    private XSSFSheet sheet;
    private final List<T> lists;
    private final Class<T> clazz;
    private final Field[] fields;

    public ExcelExporter(@NotNull List<T> lists, Class<T> clazz) {
        this.lists = lists;
        this.clazz = clazz;
        this.fields = ClazzUtil.removeAuditableFields(ClazzUtil.getAllFields(clazz));
    }

    private void writeHeaderLine(XSSFWorkbook workbook) {
        String tableName = clazz.getSimpleName();

        this.sheet = workbook.createSheet(ClazzUtil.tableName(clazz));

        Row row = this.sheet.createRow(0);

        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeight(16);
        style.setFont(font);

        for (int index = 0; index < fields.length; index++) {
            createCell(row, index, ClazzUtil.camelToTitleCase(fields[index].getName()), style);
        }
    }

    private void createCell(Row row, int columnCount, Object value, CellStyle style) {
        sheet.autoSizeColumn(columnCount);
        Cell cell = row.createCell(columnCount);
        if (value != null) {
            if (value instanceof Integer) {
                cell.setCellValue((Integer) value);
            } else if (value instanceof Double) {
                cell.setCellValue((Double) value);
            } else if (value instanceof Float) {
                cell.setCellValue((Float) value);
            } else {
                cell.setCellValue(value.toString());
            }
        }
        cell.setCellStyle(style);
    }

    private void writeDataLines(XSSFWorkbook workbook) throws IllegalAccessException {

        if (lists == null || lists.isEmpty()) return;

        int rowCount = 1;

        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        XSSFFont font = workbook.createFont();
        font.setFontHeight(14);
        style.setFont(font);

        for (T data : lists) {
            Row row = sheet.createRow(rowCount++);
            int columnCount = 0;

            for (Field field : fields) {
                field.setAccessible(true);
                createCell(row, columnCount++, field.get(data), style);
            }
        }
    }

    public byte[] exportExcel() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeHeaderLine(workbook);
            writeDataLines(workbook);
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new ExcelException("fail to export data to Excel file.", e);
        } finally {
            sheet = null;
        }
    }
}
