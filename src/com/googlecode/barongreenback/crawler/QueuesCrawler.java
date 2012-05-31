package com.googlecode.barongreenback.crawler;

import com.googlecode.barongreenback.persistence.BaronGreenbackRecords;
import com.googlecode.barongreenback.shared.ModelRepository;
import com.googlecode.funclate.Model;
import com.googlecode.lazyrecords.Definition;
import com.googlecode.lazyrecords.Record;
import com.googlecode.lazyrecords.mappings.StringMappings;
import com.googlecode.totallylazy.Function1;
import com.googlecode.totallylazy.Pair;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.Uri;
import com.googlecode.utterlyidle.Response;
import com.googlecode.utterlyidle.handlers.ClientHttpHandler;

import java.io.PrintStream;
import java.util.UUID;
import java.util.concurrent.*;

public class QueuesCrawler extends AbstractCrawler {
    private final ExecutorService inputHandlers;
    private final ExecutorService dataMappers;
    private final ExecutorService writers;
    private final DataWriter dataWriter;
    private final CheckPointHandler checkpointHandler;
    private final StringMappings mappings;
    private final BlockingDeque<Pair<HttpDataSource,Response>> retry;

    public QueuesCrawler(final ModelRepository modelRepository, final BaronGreenbackRecords records, CheckPointHandler checkpointHandler, StringMappings mappings) {
        super(modelRepository);
        this.checkpointHandler = checkpointHandler;
        this.mappings = mappings;
        this.inputHandlers = Executors.newFixedThreadPool(10);
        this.dataMappers = Executors.newCachedThreadPool();
        this.writers = Executors.newSingleThreadExecutor();
        this.dataWriter = new DataWriter(records);
        retry = new LinkedBlockingDeque<Pair<HttpDataSource, Response>>();
    }

    @Override
    public Number crawl(UUID id, PrintStream log) throws Exception {
        Model crawler = crawlerFor(id);
        Definition source = sourceDefinition(crawler);
        Definition destination = destinationDefinition(crawler);

        updateView(crawler, keywords(destination));

        HttpDataSource dataSource = HttpDataSource.dataSource(requestFor(crawler), source);
        crawl(PaginatedHttpJob.paginatedHttpJob(dataSource, destination, new ClientHttpHandler(), checkpointHandler.lastCheckPointFor(crawler), more(crawler), mappings, retry), log);
        return -1;
    }

    public static Uri requestFor(Model crawler) {
        return from(crawler);
    }

    public Future<?> crawl(HttpJob job, PrintStream log) {
        return submit(inputHandlers, job.getInput(log).then(
                submit(dataMappers, processJobs(job.process(), log).then(
                        submit(writers, dataWriter.writeUnique(job.destination()))))), log);
    }

    private Future<?> submit(ExecutorService executorService, final Runnable runnable, final PrintStream log) {
        return executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (RuntimeException e) {
                    e.printStackTrace(log);
                    throw e;
                }
            }
        });
    }


    private <T> Function1<T, Future<?>> submit(final ExecutorService executorService, final Function1<T, ?> then) {
        return new Function1<T, Future<?>>() {
            @Override
            public Future<?> call(T result) throws Exception {
                return executorService.submit((Runnable) then.deferApply(result));
            }
        };
    }

    private <T> Function1<T, Sequence<Record>> processJobs(final Function1<T, Pair<Sequence<Record>, Sequence<HttpJob>>> function, final PrintStream log) {
        return new Function1<T, Sequence<Record>>() {
            @Override
            public Sequence<Record> call(T t) throws Exception {
                Pair<Sequence<Record>, Sequence<HttpJob>> pair = function.call(t);
                for (HttpJob job : pair.second()) {
                    crawl(job, log);
                }
                return pair.first();
            }
        };
    }
}