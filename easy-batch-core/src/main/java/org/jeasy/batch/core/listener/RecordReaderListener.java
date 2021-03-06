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
package org.jeasy.batch.core.listener;

import org.jeasy.batch.core.record.Record;

/**
 * Enables the implementing class to get notified before/after reading a record.
 *
 * @author Mario Mueller (mario@xenji.com)
 * @param <P> type of the record's payload
 */
public interface RecordReaderListener<P> {

    /**
     * Called before each record read operation.
     */
    default void beforeRecordReading() {
        // no-op
    }

    /**
     * Called after each record read operation.
     *
     * @param record The record that has been read. May be {@code null} if the reader reached the end of data source
     */
    default void afterRecordReading(final Record<P> record) {
        // no-op
    }

    /**
     * Called when an exception occurs during record reading.
     *
     * @param throwable the throwable that was thrown during record reading
     */
    default void onRecordReadingException(final Throwable throwable) {
        // no-op
    }

}
