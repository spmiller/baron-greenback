package com.googlecode.barongreenback.crawler;

import com.googlecode.lazyrecords.Definition;
import com.googlecode.lazyrecords.Record;
import com.googlecode.totallylazy.Function1;
import com.googlecode.totallylazy.Pair;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.utterlyidle.Response;
import com.googlecode.yadic.Container;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.googlecode.barongreenback.crawler.DataTransformer.loadDocument;
import static com.googlecode.barongreenback.crawler.DataTransformer.transformData;
import static java.util.Collections.unmodifiableMap;

public class HttpJob implements StagedJob {
    protected final Map<String, Object> context;

    protected HttpJob(Map<String, Object> context) {
        this.context = unmodifiableMap(context);
    }

    public static HttpJob job(HttpDatasource datasource, Definition destination) {
        ConcurrentMap<String, Object> context = new ConcurrentHashMap<String, Object>();
        context.put("datasource", datasource);
        context.put("destination", destination);
        return new HttpJob(context);
    }

    @Override
    public HttpDatasource datasource() {
        return (HttpDatasource) context.get("datasource");
    }

    @Override
    public Definition destination() {
        return (Definition) context.get("destination");
    }

    @Override
    public Function1<Response, Pair<Sequence<Record>, Sequence<StagedJob>>> process(final Container crawlerScope) {
        return new Function1<Response, Pair<Sequence<Record>, Sequence<StagedJob>>>() {
            @Override
            public Pair<Sequence<Record>, Sequence<StagedJob>> call(Response response) throws Exception {
                return new SubfeedJobCreator(datasource(), destination()).process(transformData(loadDocument(response), datasource().source()).realise());
            }
        };
    }
}