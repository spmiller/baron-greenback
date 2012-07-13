package com.googlecode.barongreenback.crawler;

import com.googlecode.totallylazy.Option;
import com.googlecode.totallylazy.Pair;
import com.googlecode.utterlyidle.Response;

import java.util.*;

public class CrawlerFailures implements StatusMonitor {
    public static final int MAX_FAILURES = 1000;
    private final Map<UUID, Pair<StagedJob, Response>> failures = new LinkedHashMap<UUID, Pair<StagedJob, Response>>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Pair<StagedJob, Response>> eldest) {
            return size() > MAX_FAILURES;
        }
    };

    @Override
    public String name() {
        return "Retry Queue";
    }

    @Override
    public Option<Integer> activeThreads() {
        return Option.none();
    }

    @Override
    public int size() {
        return failures.size();
    }

    public void add(Pair<StagedJob, Response> failure) {
        failures.put(UUID.randomUUID(), failure);
    }

    public Map<UUID, Pair<StagedJob, Response>> values() {
        return failures;
    }

    public void delete(UUID id) {
        failures.remove(id);
    }

    public Option<Pair<StagedJob, Response>> get(UUID id) {
        return Option.option(failures.get(id));
    }

    public boolean isEmpty() {
        return failures.isEmpty();
    }
}