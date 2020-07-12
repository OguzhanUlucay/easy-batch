/*
 * The MIT License
 *
 *   Copyright (c) 2020, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 */
package org.jeasy.batch.extensions.msexcel;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jeasy.batch.core.reader.RecordReader;
import org.jeasy.batch.core.record.Header;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Iterator;

/**
 * Reader that reads data from a MS Excel sheet.
 * <strong>Only MS Excel XLSX format is supported</strong>
 *
 * This reader produces {@link MsExcelRecord} instances.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public class MsExcelRecordReader implements RecordReader<Row> {

    private Path path;
    private XSSFSheet sheet;
    private XSSFWorkbook workbook;
    private Iterator<Row> rowIterator;
    private long recordNumber;

    /**
     * Create a new {@link MsExcelRecordReader}.
     *
     * @param path to the input file
     * @throws IOException when an error occurs during file opening
     */
    public MsExcelRecordReader(final Path path) throws IOException {
        this(path, 0);
    }

    /**
     * Create a new {@link MsExcelRecordReader}.
     *
     * @param path to the input file
     * @param sheetIndex the sheet index
     * @throws IOException when an error occurs during file opening
     */
    public MsExcelRecordReader(final Path path, final int sheetIndex) throws IOException {
        this.path = path;
        try {
            workbook = new XSSFWorkbook(path.toFile());
            sheet = workbook.getSheetAt(sheetIndex);
        } catch (InvalidFormatException e) {
            throw new IOException("Invalid MsExcel file format. Only 'xlsx' is supported", e);
        }
    }

    @Override
    public void open() {
        recordNumber = 1;
        rowIterator = sheet.iterator();
    }

    @Override
    public MsExcelRecord readRecord() {
        if (rowIterator.hasNext()) {
            Header header = new Header(recordNumber++, getDataSourceName(), LocalDateTime.now());
            Row payload = rowIterator.next();
            return new MsExcelRecord(header, payload);
        } else {
            return null;
        }
    }

    private String getDataSourceName() {
        return String.format("Sheet '%s' in file %s", sheet.getSheetName(), path.toAbsolutePath().toString());
    }

    @Override
    public void close() throws Exception {
        workbook.close();
    }
}
