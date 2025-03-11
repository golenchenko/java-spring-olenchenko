package com.olenchenko.services;

import com.olenchenko.Model.Product;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelService {

    Workbook workbook;
    Sheet sheet;
    private Row getRow(int rowNum) {
        if (sheet.getRow(rowNum) == null) {
            return sheet.createRow(rowNum);
        }
        return sheet.getRow(rowNum);
    }
    private Cell getCell(int rowNum, int cellNum) {
        Row row = getRow(rowNum);
        if (row.getCell(cellNum) == null) {
            return row.createCell(cellNum);
        }
        return row.getCell(cellNum);
    }
    private void setStyleForCells(int firstRow, int lastRow, int firstColumn, int lastColumn, CellStyle cellStyle) {
        for (int i = firstRow; i <= lastRow; i++) {
            for (int j = firstColumn; j <= lastColumn; j++) {
                Row rowFromFirstColumn = getRow(i);
                Cell cellFromFirstColumn = getCell(rowFromFirstColumn,j);
                cellFromFirstColumn.setCellStyle(cellStyle);
            }
        }
    }

    private Cell getCell(Row row, int cellNum) {
        if (row.getCell(cellNum) == null) {
            return row.createCell(cellNum);
        }
        return row.getCell(cellNum);
    }

    private void saveWorksheetBook(OutputStream outputStream) throws IOException {
        workbook.write(outputStream);
        workbook.close();
    }
    private CellStyle createMergedCellStyle() {
        CellStyle mergedCellStyle = workbook.createCellStyle();
        mergedCellStyle.setAlignment(HorizontalAlignment.LEFT);
        mergedCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        mergedCellStyle.setWrapText(true);
        return mergedCellStyle;
    }
    public byte[] generateXLSXFile(Product product) {
        workbook = new XSSFWorkbook();
        CellStyle mergedCellStyle = createMergedCellStyle();
        // Create a new sheet
        sheet = workbook.createSheet("Інформація про товар");


        Row firstRow = getRow(0);
        firstRow.setHeight((short) 1000);
        getCell(0, 0).setCellValue("Назва товару");

        Cell titleCell = getCell(0, 1);
        XSSFFont textRun = new XSSFFont();
        textRun.setStrikeout(true);

        titleCell.setCellValue(product.getTitle());
        titleCell.setCellStyle(mergedCellStyle);
        //        Merge B1 to G1
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 9));


        Row secondRow = getRow(1);
        getCell(secondRow, 0).setCellValue("Опис");
        secondRow.setHeight((short) 5000);

        Cell descriptionCell = getCell(secondRow, 1);
        descriptionCell.setCellStyle(mergedCellStyle);
        descriptionCell.setCellValue(product.getDescription());
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 9));

        Row thirdRow = getRow(2);
        Cell priceWithoutDiscountCell = getCell(thirdRow, 1);
        Cell priceWithDiscountCell = getCell(thirdRow,5);
        priceWithoutDiscountCell.setCellStyle(mergedCellStyle);
        priceWithDiscountCell.setCellStyle(mergedCellStyle);

        getCell(thirdRow, 0).setCellValue("Ціна");
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 1, 4));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 5, 9));


        priceWithoutDiscountCell.setCellValue(product.getPriceWithoutDiscount());
        priceWithDiscountCell.setCellValue(product.getPriceWithDiscount());

        Row fourthRow = getRow(3);
        getCell(fourthRow, 0).setCellValue("Посилання");
        fourthRow.setRowStyle(mergedCellStyle);
        Cell urlCell = getCell(fourthRow, 1);
        urlCell.setCellValue(product.getUrl());
        urlCell.setCellStyle(mergedCellStyle);
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 1, 9));

        Row fifthRow = getRow(4);
        fifthRow.setRowStyle(mergedCellStyle);
        getCell(fifthRow, 0).setCellValue("Артикул");
        Cell articulCell = getCell(fifthRow, 1);
        articulCell.setCellValue(product.getArticle());
        articulCell.setCellStyle(mergedCellStyle);
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 1, 9));

        Row sixthRow = getRow(5);
        getCell(sixthRow, 0).setCellValue("Характеристики");
        HashMap<String, String> properties = product.getProperties();

        int rowNum = 5;
        int startRowNum = rowNum;
        Row row;
        Cell cell;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            row = getRow(rowNum);
            cell = getCell(row, 1);
            cell.setCellValue(key);
            cell.setCellStyle(mergedCellStyle);
            cell = getCell(row, 4);
            cell.setCellValue(value);
            cell.setCellStyle(mergedCellStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 1, 3));
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 4, 9));
            rowNum++;
        }
        sheet.addMergedRegion(new CellRangeAddress(startRowNum, rowNum-1, 0, 0));
//Variations
        HashMap<String, Object> variations = product.getVariations();
        startRowNum = rowNum;

//        "variations": {
//    "Комунікації": [
//      {
//        "title": "Wi-Fi",
//        "url": "https://touch.com.ua/ua/item/xiaomi-redmi-pad-se-8-7-6-128gb-graphite-gray-global-eu-117817-smartfony-smartfoni/"
//      },
//      {
//        "title": "Wi-Fi + LTE",
//        "url": "https://touch.com.ua/ua/item/xiaomi-redmi-pad-se-8-7-4g-6-128gb-graphite-gray-global-eu-113420-planshety-plansheti/"
//      }
//    ],
        if (!variations.isEmpty()) {
            row = getRow(startRowNum);
            getCell(row, 0).setCellValue("Варіації");
            for (Map.Entry<String, Object> entry : variations.entrySet()) {
                String variationName = entry.getKey(); // Комунікації
                row = getRow(rowNum);
                cell = getCell(row, 1);
                cell.setCellValue(variationName);
                cell.setCellStyle(mergedCellStyle);
                List<HashMap<String, String>> variationsValues = (List<HashMap<String, String>>) entry.getValue();

                sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + variationsValues.size() - 1, 1, 2));

                for (HashMap<String, String> variationsValue : variationsValues) {
                    row = getRow(rowNum);
                    cell = getCell(row, 3);
                    cell.setCellValue(variationsValue.get("title"));
                    cell.setCellStyle(mergedCellStyle);
                    sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 3, 4));
                    cell = getCell(row, 5);
                    workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);

                    Hyperlink cellHyperlink = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                    cellHyperlink.setAddress(variationsValue.get("url"));
                    cell.setHyperlink(cellHyperlink);
                    cell.setCellValue(variationsValue.get("url"));

                    cell.setCellStyle(mergedCellStyle);
                    sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 5, 9));
                    rowNum++;
                }
            }
            if (startRowNum < rowNum-1) {
                sheet.addMergedRegion(new CellRangeAddress(startRowNum, rowNum-1, 0, 0));
            }
        }

        sheet.autoSizeColumn(0);
        setStyleForCells(0, rowNum - 1, 0, 0, mergedCellStyle);


        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            saveWorksheetBook(outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputStream.toByteArray();
    }
}
