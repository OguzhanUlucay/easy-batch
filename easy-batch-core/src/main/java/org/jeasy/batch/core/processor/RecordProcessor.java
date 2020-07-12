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
package org.jeasy.batch.core.processor;

import org.jeasy.batch.core.record.Record;

/**
 * A record processor performs business logic on input records and produces output records.
 * The output record may be of a different type than the input record and will be piped out to the next processor if any.
 * If a record processor throws an exception during processing, the record will be reported as an error.
 * If a record processor returns {@code null}, the record will be filtered and next processors in the pipeline will be skipped.
 *
 * @param <I> The payload's type of input records
 * @param <O> The payload's type of output records
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public interface RecordProcessor<I, O> {

    /**
     * Process a record.
     *
     * @param record to process.
     * @return the processed record, may be of a different type than the input record, or {@code null} to skip next processors
     * @throws Exception if an error occurs during record processing
     */
    Record<O> processRecord(Record<I> record) throws Exception;

}
