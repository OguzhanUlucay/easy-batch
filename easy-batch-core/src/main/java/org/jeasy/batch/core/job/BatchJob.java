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
package org.jeasy.batch.core.job;

import java.time.LocalDateTime;

import org.jeasy.batch.core.jmx.JobMonitor;
import org.jeasy.batch.core.listener.BatchListener;
import org.jeasy.batch.core.listener.CompositeBatchListener;
import org.jeasy.batch.core.listener.CompositeJobListener;
import org.jeasy.batch.core.listener.CompositePipelineListener;
import org.jeasy.batch.core.listener.CompositeRecordReaderListener;
import org.jeasy.batch.core.listener.CompositeRecordWriterListener;
import org.jeasy.batch.core.listener.JobListener;
import org.jeasy.batch.core.listener.PipelineListener;
import org.jeasy.batch.core.listener.RecordReaderListener;
import org.jeasy.batch.core.listener.RecordWriterListener;
import org.jeasy.batch.core.processor.CompositeRecordProcessor;
import org.jeasy.batch.core.processor.RecordProcessor;
import org.jeasy.batch.core.reader.RecordReader;
import org.jeasy.batch.core.record.Batch;
import org.jeasy.batch.core.record.Record;
import org.jeasy.batch.core.util.Utils;
import org.jeasy.batch.core.writer.RecordWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of read-process-write job pattern.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
class BatchJob<I , O> implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchJob.class);
    private static final String DEFAULT_JOB_NAME = "job";

    private String name;

    private RecordReader<I> recordReader;
    private RecordWriter<O> recordWriter;
    private RecordProcessor<I, O> recordProcessor;
    private RecordTracker recordTracker;

    private JobListener jobListener;
    private BatchListener<O> batchListener;
    private RecordReaderListener<I> recordReaderListener;
    private RecordWriterListener<O> recordWriterListener;
    private PipelineListener pipelineListener;

    private JobParameters parameters;
    private JobMetrics metrics;
    private JobReport report;
    private JobMonitor monitor;

    BatchJob(JobParameters parameters) {
        this.parameters = parameters;
        this.name = DEFAULT_JOB_NAME;
        metrics = new JobMetrics();
        report = new JobReport();
        report.setParameters(parameters);
        report.setMetrics(metrics);
        report.setJobName(name);
        report.setSystemProperties(System.getProperties());
        monitor = new JobMonitor(report);
        recordReader = new NoOpRecordReader<>();
        recordProcessor = new CompositeRecordProcessor<>();
        recordWriter = new NoOpRecordWriter<>();
        recordReaderListener = new CompositeRecordReaderListener<>();
        pipelineListener = new CompositePipelineListener();
        recordWriterListener = new CompositeRecordWriterListener<>();
        batchListener = new CompositeBatchListener<>();
        jobListener = new CompositeJobListener();
        recordTracker = new RecordTracker();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public JobReport call() {
        start();
        try {
            openReader();
            openWriter();
            setStatus(JobStatus.STARTED);
            while (moreRecords() && !isInterrupted()) {
                Batch<O> batch = readAndProcessBatch();
                writeBatch(batch);
            }
            setStatus(JobStatus.STOPPING);
        } catch (Exception exception) {
            fail(exception);
            return report;
        } finally {
            closeReader();
            closeWriter();
        }
        teardown();
        return report;
    }

    /*
     * private methods
     */

    private void start() {
        setStatus(JobStatus.STARTING);
        jobListener.beforeJob(parameters);
        recordTracker = new RecordTracker();
        metrics.setStartTime(LocalDateTime.now());
        LOGGER.debug("Batch size: {}", parameters.getBatchSize());
        LOGGER.debug("Error threshold: {}", Utils.formatErrorThreshold(parameters.getErrorThreshold()));
        LOGGER.debug("Jmx monitoring: {}", parameters.isJmxMonitoring());
        LOGGER.debug("Batch scanning: {}", parameters.isBatchScanningEnabled());
        registerJobMonitor();
    }

    private void registerJobMonitor() {
        if (parameters.isJmxMonitoring()) {
            monitor.registerJmxMBeanFor(this);
        }
    }

    private void openReader() throws Exception {
        try {
            LOGGER.debug("Opening record reader");
            recordReader.open();
        } catch (Exception e) {
            LOGGER.error("Unable to open record reader", e);
            throw e;
        }
    }

    private void openWriter() throws Exception {
        try {
            LOGGER.debug("Opening record writer");
            recordWriter.open();
        } catch (Exception e) {
            LOGGER.error("Unable to open record writer", e);
            throw e;
        }
    }

    private void setStatus(JobStatus status) {
        if(isInterrupted()) {
            LOGGER.info("Job '{}' has been interrupted, aborting execution.", name);
        }
        LOGGER.info("Job '{}' {}", name, status.name().toLowerCase());
        report.setStatus(status);
    }

    private boolean moreRecords() {
        return recordTracker.moreRecords();
    }

    private Batch<O> readAndProcessBatch() throws Exception {
        Batch<O> batch = new Batch<>();
        batchListener.beforeBatchReading();
        for (int i = 0; i < parameters.getBatchSize(); i++) {
            Record<I> record = readRecord();
            if (record == null) {
                LOGGER.debug("No more records");
                recordTracker.noMoreRecords();
                break;
            } else {
                metrics.incrementReadCount();
            }
            processRecord(record, batch);
        }
        batchListener.afterBatchProcessing(batch);
        return batch;
    }

    private Record<I> readRecord() throws Exception {
        Record<I> record;
        try {
            LOGGER.debug("Reading next record");
            recordReaderListener.beforeRecordReading();
            record = recordReader.readRecord();
            recordReaderListener.afterRecordReading(record);
            return record;
        } catch (Exception e) {
            recordReaderListener.onRecordReadingException(e);
            LOGGER.error("Unable to read next record", e);
            throw e;
        }
    }

    @SuppressWarnings(value = "unchecked,rawtypes")
    private void processRecord(Record<I> record, Batch<O> batch) throws ErrorThresholdExceededException {
        Record processedRecord = null;
        try {
            LOGGER.debug("Processing record {}", record);
            notifyJobUpdate();
            Record preProcessedRecord = pipelineListener.beforeRecordProcessing(record);
            if (preProcessedRecord == null) {
                LOGGER.debug("Record {} has been filtered", record);
                metrics.incrementFilterCount();
            } else {
                processedRecord = recordProcessor.processRecord(preProcessedRecord);
                if (processedRecord == null) {
                    LOGGER.debug("Record {} has been filtered", record);
                    metrics.incrementFilterCount();
                } else {
                    batch.addRecord(processedRecord);
                }
            }
            pipelineListener.afterRecordProcessing(record, processedRecord);
        } catch (Exception e) {
            LOGGER.error("Unable to process record {}", record, e);
            pipelineListener.onRecordProcessingException(record, e);
            metrics.incrementErrorCount();
            report.setLastError(e);
            if (metrics.getErrorCount() > parameters.getErrorThreshold()) {
                String errorMessage = "Error threshold exceeded. Aborting execution";
                LOGGER.error(errorMessage, e);
                throw new ErrorThresholdExceededException(errorMessage, e);
            }
        }
    }

    private void writeBatch(Batch<O> batch) throws Exception {
        try {
            if (!batch.isEmpty()) {
                LOGGER.debug("Writing records {}", batch);
                recordWriterListener.beforeRecordWriting(batch);
                recordWriter.writeRecords(batch);
                recordWriterListener.afterRecordWriting(batch);
                batchListener.afterBatchWriting(batch);
                metrics.incrementWriteCount(batch.size());
            }
        } catch (Exception e) {
            recordWriterListener.onRecordWritingException(batch, e);
            batchListener.onBatchWritingException(batch, e);
            report.setLastError(e);
            if (parameters.isBatchScanningEnabled()) {
                scan(batch);
            } else {
                LOGGER.error("Unable to write records", e);
                throw e;
            }
        }
    }

    private void scan(Batch<O> batch) {
        LOGGER.debug("Scanning records {}", batch);
        for (Record<O> record : batch) {
            record.getHeader().setScanned(true);
            Batch<O> scannedBatch = new Batch<>(record);
            try {
                recordWriterListener.beforeRecordWriting(scannedBatch);
                recordWriter.writeRecords(scannedBatch);
                recordWriterListener.afterRecordWriting(scannedBatch);
                metrics.incrementWriteCount(scannedBatch.size());
            } catch (Exception exception) {
                recordWriterListener.onRecordWritingException(scannedBatch, exception);
                metrics.incrementErrorCount();
                report.setLastError(exception);
            }
        }
        LOGGER.debug("End of records scanning");
    }

    private boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    private void teardown() {
        JobStatus jobStatus = isInterrupted() ? JobStatus.ABORTED : JobStatus.COMPLETED;
        teardown(jobStatus);
    }

    private void teardown(JobStatus status) {
        report.setStatus(status);
        metrics.setEndTime(LocalDateTime.now());
        LOGGER.info( "Job '{}' finished with status {} in {}",
                name, report.getStatus(), Utils.formatDuration(report.getMetrics().getDuration()));
        notifyJobUpdate();
        jobListener.afterJob(report);
    }

    private void fail(Exception exception) {
        report.setLastError(exception);
        teardown(JobStatus.FAILED);
    }

    private void closeReader() {
        try {
            LOGGER.debug("Closing record reader");
            recordReader.close();
        } catch (Exception e) {
            LOGGER.error("Unable to close record reader", e);
            report.setLastError(e);
        }
    }

    private void closeWriter() {
        try {
            LOGGER.debug("Closing record writer");
            recordWriter.close();
        } catch (Exception e) {
            LOGGER.error("Unable to close record writer", e);
            report.setLastError(e);
        }
    }

    private void notifyJobUpdate() {
        if (parameters.isJmxMonitoring()) {
            monitor.notifyJobReportUpdate();
        }
    }

    /*
     * Setters for job components
     */

    public void setRecordReader(RecordReader<I> recordReader) {
        this.recordReader = recordReader;
    }

    public void setRecordWriter(RecordWriter<O> recordWriter) {
        this.recordWriter = recordWriter;
    }

    public void addRecordProcessor(RecordProcessor<?, ?> recordProcessor) {
        ((CompositeRecordProcessor) this.recordProcessor).addRecordProcessor(recordProcessor);
    }

    public void addBatchListener(BatchListener<O> batchListener) {
        ((CompositeBatchListener) this.batchListener).addBatchListener(batchListener);
    }

    public void addJobListener(JobListener jobListener) {
        ((CompositeJobListener) this.jobListener).addJobListener(jobListener);
    }

    public void addRecordReaderListener(RecordReaderListener<I> recordReaderListener) {
        ((CompositeRecordReaderListener) this.recordReaderListener).addRecordReaderListener(recordReaderListener);
    }

    public void addRecordWriterListener(RecordWriterListener<O> recordWriterListener) {
        ((CompositeRecordWriterListener) this.recordWriterListener).addRecordWriterListener(recordWriterListener);
    }

    public void addPipelineListener(PipelineListener pipelineListener) {
        ((CompositePipelineListener) this.pipelineListener).addPipelineListener(pipelineListener);
    }

    public void setName(String name) {
        this.name = name;
        this.report.setJobName(name);
    }

    /*
     * Private inner classes that does not make sense to be public in other packages (reader/writer)
     */
    private static class NoOpRecordReader<P> implements RecordReader<P> {

        @Override
        public void open() {

        }

        @Override
        public Record<P> readRecord() {
            return null;
        }

        @Override
        public void close() {

        }
    }

    private static class NoOpRecordWriter<P> implements RecordWriter<P> {

        @Override
        public void open() {

        }

        @Override
        public void writeRecords(Batch<P> batch) {

        }

        @Override
        public void close() {

        }

    }
}
