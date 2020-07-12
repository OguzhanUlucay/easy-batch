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

import org.jeasy.batch.core.filter.RecordFilter;
import org.jeasy.batch.core.listener.BatchListener;
import org.jeasy.batch.core.listener.JobListener;
import org.jeasy.batch.core.listener.PipelineListener;
import org.jeasy.batch.core.listener.RecordReaderListener;
import org.jeasy.batch.core.listener.RecordWriterListener;
import org.jeasy.batch.core.mapper.RecordMapper;
import org.jeasy.batch.core.marshaller.RecordMarshaller;
import org.jeasy.batch.core.processor.RecordProcessor;
import org.jeasy.batch.core.reader.RecordReader;
import org.jeasy.batch.core.util.Utils;
import org.jeasy.batch.core.validator.RecordValidator;
import org.jeasy.batch.core.writer.RecordWriter;

/**
 * Batch job builder.
 * This is the main entry point to configure batch jobs.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public final class JobBuilder<I, O> {

    private BatchJob<I, O> job;
    private JobParameters parameters;

    /**
     * Create a new {@link JobBuilder}.
     */
    public JobBuilder() {
        parameters = new JobParameters();
        job = new BatchJob<>(parameters);
    }

    /**
     * Create a new {@link JobBuilder}.
     *
     * @return a new job builder.
     */
    public static JobBuilder aNewJob() {
        return new JobBuilder<>();
    }

    /**
     * Set the job name.
     *
     * @param name the job name
     * @return the job builder
     */
    public JobBuilder<I, O> named(final String name) {
        Utils.checkNotNull(name, "job name");
        job.setName(name);
        return this;
    }

    /**
     * Register a record reader.
     *
     * @param recordReader the record reader to register
     * @return the job builder
     */
    public JobBuilder<I, O> reader(final RecordReader<I> recordReader) {
        Utils.checkNotNull(recordReader, "record reader");
        job.setRecordReader(recordReader);
        return this;
    }

    /**
     * Register a record filter.
     *
     * @param recordFilter the record filter to register
     * @return the job builder
     */
    public JobBuilder<I, O> filter(final RecordFilter<?> recordFilter) {
        Utils.checkNotNull(recordFilter, "record filter");
        job.addRecordProcessor(recordFilter);
        return this;
    }

    /**
     * Register a record mapper.
     *
     * @param recordMapper the record mapper to register
     * @return the job builder
     */
    public JobBuilder<I, O> mapper(final RecordMapper<?, ?> recordMapper) {
        Utils.checkNotNull(recordMapper, "record mapper");
        job.addRecordProcessor(recordMapper);
        return this;
    }

    /**
     * Register a record validator.
     *
     * @param recordValidator the record validator to register
     * @return the job builder
     */
    public JobBuilder<I, O> validator(final RecordValidator<?> recordValidator) {
        Utils.checkNotNull(recordValidator, "record validator");
        job.addRecordProcessor(recordValidator);
        return this;
    }

    /**
     * Register a record processor.
     *
     * @param recordProcessor the record processor to register
     * @return the job builder
     */
    public JobBuilder<I, O> processor(final RecordProcessor<?, ?> recordProcessor) {
        Utils.checkNotNull(recordProcessor, "record processor");
        job.addRecordProcessor(recordProcessor);
        return this;
    }

    /**
     * Register a record marshaller.
     *
     * @param recordMarshaller the record marshaller to register
     * @return the job builder
     */
    public JobBuilder<I, O> marshaller(final RecordMarshaller<?, ?> recordMarshaller) {
        Utils.checkNotNull(recordMarshaller, "record marshaller");
        job.addRecordProcessor(recordMarshaller);
        return this;
    }

    /**
     * Register a record writer.
     *
     * @param recordWriter the record writer to register
     * @return the job builder
     */
    public JobBuilder<I, O> writer(final RecordWriter<O> recordWriter) {
        Utils.checkNotNull(recordWriter, "record writer");
        job.setRecordWriter(recordWriter);
        return this;
    }

    /**
     * Set a threshold for errors. The job will be aborted if the threshold is exceeded.
     *
     * @param errorThreshold the error threshold
     * @return the job builder
     */
    public JobBuilder<I, O> errorThreshold(final long errorThreshold) {
        Utils.checkArgument(errorThreshold >= 0, "error threshold must be >= 0");
        parameters.setErrorThreshold(errorThreshold);
        return this;
    }

    /**
     * Activate JMX monitoring.
     *
     * @param jmx true to enable jmx monitoring
     * @return the job builder
     */
    public JobBuilder<I, O> enableJmx(final boolean jmx) {
        parameters.setJmxMonitoring(jmx);
        return this;
    }

    /**
     * Activate batch scanning. When activated, batch scanning will be kicked in
     * when an exception occurs during the batch writing. Records will be attempted
     * to be written one by one as a singleton batch. This allows to skip faulty
     * records and continue the job execution instead of failing the entire job
     * at the first failed batch.
     *
     * <p><strong>This feature works well with transactional writers where a failed write
     * operation can be re-executed without side effects. However, a known limitation
     * is that when used with a non-transactional writer, items might be written twice
     * (like in the case of a file writer where the output stream is flushed before the
     * exception occurs). To prevent this, a manual rollback action should be done in
     * a {@link org.jeasy.batch.core.listener.BatchListener#onBatchWritingException(org.jeasy.batch.core.record.Batch, java.lang.Throwable)} method.</strong></p>
     *
     * @param batchScanning true to enable batch scanning. False by default.
     * @return the job builder
     */
    public JobBuilder<I, O> enableBatchScanning(final boolean batchScanning) {
        parameters.setBatchScanningEnabled(batchScanning);
        return this;
    }

    /**
     * Set the batch size.
     *
     * @param batchSize the batch size
     * @return the job builder
     */
    public JobBuilder<I, O> batchSize(final int batchSize) {
        Utils.checkArgument(batchSize >= 1, "Batch size must be >= 1");
        parameters.setBatchSize(batchSize);
        return this;
    }

    /**
     * Register a job listener.
     * See {@link JobListener} for available callback methods.
     *
     * @param jobListener The job listener to add.
     * @return the job builder
     */
    public JobBuilder<I, O> jobListener(final JobListener jobListener) {
        Utils.checkNotNull(jobListener, "job listener");
        job.addJobListener(jobListener);
        return this;
    }

    /**
     * Register a batch listener.
     * See {@link BatchListener} for available callback methods.
     *
     * @param batchListener The batch listener to add.
     * @return the job builder
     */
    public JobBuilder<I, O> batchListener(final BatchListener<O> batchListener) {
        Utils.checkNotNull(batchListener, "batch listener");
        job.addBatchListener(batchListener);
        return this;
    }

    /**
     * Register a record reader listener.
     * See {@link RecordReaderListener} for available callback methods.
     *
     * @param recordReaderListener The record reader listener to add.
     * @return the job builder
     */
    public JobBuilder<I, O> readerListener(final RecordReaderListener<I> recordReaderListener) {
        Utils.checkNotNull(recordReaderListener, "record reader listener");
        job.addRecordReaderListener(recordReaderListener);
        return this;
    }

    /**
     * Register a pipeline listener.
     * See {@link PipelineListener} for available callback methods.
     *
     * @param pipelineListener The pipeline listener to add.
     * @return the job builder
     */
    public JobBuilder<I, O> pipelineListener(final PipelineListener pipelineListener) {
        Utils.checkNotNull(pipelineListener, "pipeline listener");
        job.addPipelineListener(pipelineListener);
        return this;
    }

    /**
     * Register a record writer listener.
     * See {@link RecordWriterListener} for available callback methods.
     *
     * @param recordWriterListener The record writer listener to register.
     * @return the job builder
     */
    public JobBuilder<I, O> writerListener(final RecordWriterListener<O> recordWriterListener) {
        Utils.checkNotNull(recordWriterListener, "record writer listener");
        job.addRecordWriterListener(recordWriterListener);
        return this;
    }

    /**
     * Build a batch job instance.
     *
     * @return a batch job instance
     */
    public Job build() {
        return job;
    }

}
