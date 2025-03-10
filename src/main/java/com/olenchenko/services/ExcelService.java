package com.olenchenko.services;

import com.olenchenko.Model.Product;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.formula.functions.Column;
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
    public byte[] generateXLSXFile(Product product) {
        Workbook workbook = new XSSFWorkbook();

        CellStyle mergedCellStyle = workbook.createCellStyle();
        mergedCellStyle.setAlignment(HorizontalAlignment.LEFT);
        mergedCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        mergedCellStyle.setWrapText(true);



        Sheet sheet = workbook.createSheet("Product Info");


        // Header
        Row firstRow = sheet.createRow(0);
        firstRow.setHeight((short) 1000);
        firstRow.createCell(0).setCellValue("Назва товару");

        Cell titleCell = firstRow.createCell(1);
        titleCell.setCellValue(product.getTitle());
        titleCell.setCellStyle(mergedCellStyle);
        //        Merge B1 to G1
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 9));


        Row secondRow = sheet.createRow(1);
        secondRow.createCell(0).setCellValue("Опис");
        secondRow.setHeight((short) 5000);

        Cell descriptionCell = secondRow.createCell(1);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 9));
        descriptionCell.setCellStyle(mergedCellStyle);
        descriptionCell.setCellValue(product.getDescription());

        Row thirdRow = sheet.createRow(2);
        Cell priceWithoutDiscountCell = thirdRow.createCell(1);
        priceWithoutDiscountCell.setCellStyle(mergedCellStyle);
        Cell priceWithDiscountCell = thirdRow.createCell(5);
        priceWithDiscountCell.setCellStyle(mergedCellStyle);
        thirdRow.createCell(0).setCellValue("Ціна");
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 1, 4));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 5, 9));

        priceWithoutDiscountCell.setCellValue(product.getPriceWithoutDiscount());
        priceWithDiscountCell.setCellValue(product.getPriceWithDiscount());

        Row fourthRow = sheet.createRow(3);
        Cell urlCell = fourthRow.createCell(1);
        fourthRow.createCell(0).setCellValue("Посилання");
        fourthRow.setRowStyle(mergedCellStyle);
        urlCell.setCellValue(product.getUrl());
        urlCell.setCellStyle(mergedCellStyle);
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 1, 9));

        Row fifthRow = sheet.createRow(4);
        Cell articulCell = fifthRow.createCell(1);
        fifthRow.createCell(0).setCellValue("Артикул");
        fifthRow.setRowStyle(mergedCellStyle);
        articulCell.setCellValue(product.getArticle());
        articulCell.setCellStyle(mergedCellStyle);
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 1, 9));

        Row sixthRow = sheet.createRow(5);
        sixthRow.createCell(0).setCellValue("Характеристики");
        HashMap<String, String> properties = product.getProperties();
        int rowNum = 5;
        int startRowNum = rowNum;
        Row row;
        Cell cell;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (sheet.getRow(rowNum) == null) {
                row = sheet.createRow(rowNum);
            }
            else {
                row = sheet.getRow(rowNum);
            }
            cell = row.createCell(1);
            cell.setCellValue(key);
            cell.setCellStyle(mergedCellStyle);
            cell = row.createCell(4);
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
        int startSubRowNum = startRowNum;

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
        row = sheet.createRow(startRowNum);
        row.createCell(0).setCellValue("Варіації");
        for (Map.Entry<String, Object> entry : variations.entrySet()) {
            String variationName = entry.getKey(); // Комунікації
            if (sheet.getRow(rowNum) == null) {
                row = sheet.createRow(rowNum);
            }
            else {
                row = sheet.getRow(rowNum);
            }
                cell = row.createCell(1);
                cell.setCellValue(variationName);
                cell.setCellStyle(mergedCellStyle);
                List<HashMap<String, String>> variationsValues = (List<HashMap<String, String>>) entry.getValue();

                sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum + variationsValues.size() - 1, 1, 2));

            for (int i = 0; i < variationsValues.size(); i++) {
                    if (sheet.getRow(rowNum) == null) {
                        row = sheet.createRow(rowNum);
                    }
                    else {
                        row = sheet.getRow(rowNum);
                    }
                    cell = row.createCell(3);
                    cell.setCellValue(variationsValues.get(i).get("title"));
                    cell.setCellStyle(mergedCellStyle);
                    sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 3, 4));
                    cell = row.createCell(5);
                workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);

                Hyperlink cellHyperlink = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                cellHyperlink.setAddress(variationsValues.get(i).get("url"));
                cell.setHyperlink(cellHyperlink);
                cell.setCellValue(variationsValues.get(i).get("url"));

                    cell.setCellStyle(mergedCellStyle);
                    sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 5, 9));
                    rowNum++;
                }
//                sheet.addMergedRegion(new CellRangeAddress(startSubRowNum, rowNum-1, 1, 2));
//            }

        }
        sheet.addMergedRegion(new CellRangeAddress(startRowNum, rowNum-1, 0, 0));

        sheet.autoSizeColumn(0);
        for (int i = 0; i <= rowNum - 1; i++) {
            Row rowFromFirstColumn = sheet.getRow(i);
            if (rowFromFirstColumn != null) {
                Cell cellFromFirstColumn = rowFromFirstColumn.getCell(0);
                if (cellFromFirstColumn != null) {
                    cellFromFirstColumn.setCellStyle(mergedCellStyle);
                }
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            workbook.write(outputStream);
            workbook.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return outputStream.toByteArray();
    }
}
