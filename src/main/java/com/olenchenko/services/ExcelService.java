package com.olenchenko.services;

import com.olenchenko.Model.Product;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelService {

    private Workbook workbook;
    private Sheet sheet;

    public byte[] generateXLSXFile(Product product) {
        workbook = new XSSFWorkbook();
        sheet = createNewSheet("Інформація про товар");
        CellStyle mergedCellStyle = createMergedCellStyle();
        CellStyle mergedBoldCellStyle = createBoldMergedCellStyle();

        createProductTitleSection(product);
        createProductDescription(product);
        createProductPriceSection(product);
        createProductUrlSection(product);
        createProductArticleSection(product);
        createProductPropertiesSection(product);
        createProductVariationsSection(product);

        sheet.autoSizeColumn(0);
        setStyleForCells(0, sheet.getLastRowNum(), 0, 9, mergedCellStyle);
        setStyleForCells(0, sheet.getLastRowNum(), 0, 0, mergedBoldCellStyle);

        return writeToOutputStream();
    }

    private void createProductTitleSection(Product product) {
        Row row = getRow(0);

        getCell(row, 0).setCellValue("Назва товару");

        Cell cell = getCell(row, 1);
        cell.setCellValue(product.getTitle());
        row.setHeight((short) 1000);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 9));
    }

    private void createProductDescription(Product product) {
        Row row = getRow(1);
        getCell(row, 0).setCellValue("Опис");

        Cell cell = getCell(row, 1);
        String description = product.getDescription();
        cell.setCellValue(description.replace("\r", "").replace("\n", " "));
        int numberOfLines = description.split("\n").length;
        row.setHeight((short) (row.getHeight() * numberOfLines / 2) );
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 9));
    }

    private void createProductPriceSection(Product product) {
        Row row = getRow(2);
        getCell(row, 0).setCellValue("Ціна");

        Cell cell1 = getCell(row, 1);
        Cell cell2 = getCell(row, 5);

        String priceWithOutDiscountInUsd = String.format("%.2f", product.getPriceInUSDWithoutDiscount())  + "$";
        String priceWithDiscountInUsd = String.format("%.2f", product.getPriceInUSDWithDiscount())  + "$";
        cell1.setCellValue("Без знижки: " + (product.getPriceWithoutDiscount() == 0.0 ? "не вказано" : product.getPriceWithoutDiscount() + "₴ (" + priceWithOutDiscountInUsd + ")"));
        cell2.setCellValue("Зі знижкою: " + (product.getPriceWithDiscount() == 0.0 ? "не вказано" : product.getPriceWithDiscount() + "₴ (" + priceWithDiscountInUsd + ")"));

        sheet.addMergedRegion(new CellRangeAddress(2, 2, 1, 4));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 5, 9));
    }

    private void createProductUrlSection(Product product) {
        Row row = getRow(3);
        getCell(row, 0).setCellValue("Посилання");
        Cell cell = getCell(row, 1);
        cell.setCellValue(product.getUrl());
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 1, 9));
    }

    private void createProductArticleSection(Product product) {
        Row row = getRow(4);
        getCell(row, 0).setCellValue("Артикул");
        Cell cell = getCell(row, 1);
        cell.setCellValue(product.getArticle());
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 1, 9));
    }

    private void createProductPropertiesSection(Product product) {
        getCell(5, 0).setCellValue("Характеристики");
        int rowNum = 5;
        int startNum = rowNum;
        for (Map.Entry<String, String> entry : product.getProperties().entrySet()) {
            Row row = getRow(rowNum);
            getCell(row, 1).setCellValue(entry.getKey());
            getCell(row, 4).setCellValue(entry.getValue());
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 1, 3));
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 4, 9));
            rowNum++;
        }
        sheet.addMergedRegion(new CellRangeAddress(startNum, rowNum - 1, 0, 0));
    }

    private void createProductVariationsSection(Product product) {
        int rowNum = sheet.getLastRowNum() + 1;
        int startNum = rowNum;
        if (product.getVariations().isEmpty()) return;

        getCell(rowNum, 0).setCellValue("Варіації");
        for (Map.Entry<String, List<HashMap<String, String>>> entry : product.getVariations().entrySet()) {
            getCell(rowNum, 1).setCellValue(entry.getKey());
            sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + entry.getValue().size() - 1, 1, 2));
            for (HashMap<String, String> variation : entry.getValue()) {
                getCell(rowNum, 3).setCellValue(variation.get("title"));
                createHyperlink(rowNum, 5, variation.get("url"));
                rowNum++;
            }
        }
        sheet.addMergedRegion(new CellRangeAddress(startNum, rowNum, 0, 0));
    }

    private void createHyperlink(int rowNum, int colNum, String url) {
        Cell cell = getCell(rowNum, colNum);
        cell.setCellValue(url);
        Hyperlink hyperlink = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
        hyperlink.setAddress(url);
        cell.setHyperlink(hyperlink);
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, colNum, 9));
    }

    private byte[] writeToOutputStream() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Sheet createNewSheet(String sheetName) {
        return workbook.createSheet(sheetName);
    }

    private CellStyle createMergedCellStyle() {
        return getBasicCellStyle();
    }
    private CellStyle createBoldMergedCellStyle() {
        CellStyle style = getBasicCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle getBasicCellStyle() {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setShrinkToFit(true);
        return style;
    }

    private Row getRow(int rowNum) {
        return sheet.getRow(rowNum) != null ? sheet.getRow(rowNum) : sheet.createRow(rowNum);
    }

    private Cell getCell(Row row, int cellNum) {
        return row.getCell(cellNum) != null ? row.getCell(cellNum) : row.createCell(cellNum);
    }

    private Cell getCell(int rowNum, int cellNum) {
        return getCell(getRow(rowNum), cellNum);
    }

    private void setStyleForCells(int firstRow, int lastRow, int firstColumn, int lastColumn, CellStyle style) {
        for (int i = firstRow; i <= lastRow; i++) {
            for (int j = firstColumn; j <= lastColumn; j++) {
                getCell(getRow(i), j).setCellStyle(style);
            }
        }
    }
}