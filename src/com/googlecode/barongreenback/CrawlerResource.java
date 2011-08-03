package com.googlecode.barongreenback;

import com.googlecode.funclate.Model;
import com.googlecode.totallylazy.Callable1;
import com.googlecode.totallylazy.Pair;
import com.googlecode.totallylazy.Quadruple;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.Sequences;
import com.googlecode.totallylazy.Triple;
import com.googlecode.totallylazy.records.ImmutableKeyword;
import com.googlecode.totallylazy.records.Keyword;
import com.googlecode.totallylazy.records.Record;
import com.googlecode.totallylazy.records.Records;
import com.googlecode.totallylazy.records.lucene.Lucene;
import com.googlecode.utterlyidle.FormParameters;
import com.googlecode.utterlyidle.MediaType;
import com.googlecode.utterlyidle.Response;
import com.googlecode.utterlyidle.annotations.FormParam;
import com.googlecode.utterlyidle.annotations.GET;
import com.googlecode.utterlyidle.annotations.POST;
import com.googlecode.utterlyidle.annotations.Path;
import com.googlecode.utterlyidle.annotations.Produces;
import org.apache.lucene.queryParser.ParseException;

import java.net.URL;

import static com.googlecode.totallylazy.Callables.first;
import static com.googlecode.totallylazy.Callables.second;
import static com.googlecode.totallylazy.Predicates.is;
import static com.googlecode.totallylazy.Predicates.not;
import static com.googlecode.totallylazy.Predicates.where;
import static com.googlecode.totallylazy.Strings.empty;
import static com.googlecode.totallylazy.records.Keywords.keyword;
import static com.googlecode.totallylazy.records.RecordMethods.update;
import static com.googlecode.totallylazy.records.Using.using;
import static com.googlecode.utterlyidle.proxy.Resource.redirect;
import static com.googlecode.utterlyidle.proxy.Resource.resource;

@Path("admin/crawl")
@Produces(MediaType.TEXT_HTML)
public class CrawlerResource {
    private final Records records;
    private final Crawler crawler;

    public CrawlerResource(Records records, Crawler crawler) {
        this.records = records;
        this.crawler = crawler;
    }

    @GET
    public Model get(){
        return Model.model();
    }

    @POST
    public Response crawl(@FormParam("url") URL url, @FormParam("recordName") String recordName, @FormParam("elementXPath") String elementXPath,
                          @FormParam("fields") Iterable<String> fields, @FormParam("aliases") Iterable<String> aliases,
                          @FormParam("types") Iterable<String> types, @FormParam("keys") Iterable<String> keys,
                          FormParameters form
    ) throws Exception {
        Sequence<Pair<Boolean, Keyword>> pairs = toKeywords(fields, aliases, types, clean(keys));
        Sequence<Keyword> primaryKeys = pairs.filter(where(first(Boolean.class), is(true))).map(second(Keyword.class));
        Sequence<Keyword> allKeys = pairs.map(second(Keyword.class));
        XmlSource webSource = new XmlSource(url, keyword(elementXPath), allKeys);
        Sequence<Record> extractedValues = crawler.crawl(webSource);
        return put(keyword(recordName), primaryKeys, extractedValues);
    }

    private Iterable<Boolean> clean(Iterable<String> keys) {
        return new HtmlCheckboxFilter<Boolean>(false).filter(Sequences.sequence(keys).map(asBoolean()));
    }

    public static Callable1<? super String, Boolean> asBoolean() {
        return new Callable1<String, Boolean>() {
            public Boolean call(String value) throws Exception {
                return Boolean.parseBoolean(value);
            }
        };
    }

    private Sequence<Pair<Boolean, Keyword>> toKeywords(Iterable<String> fields, Iterable<String> aliases, Iterable<String> types, Iterable<Boolean> keys) {
        return Sequences.zip(fields, aliases, types, keys).filter(where(first(String.class), not(empty()))).map(asKeyword());
    }

    private Callable1<? super Quadruple<String, String, String, Boolean>, Pair<Boolean, Keyword>> asKeyword() {
        return new Callable1<Quadruple<String, String, String, Boolean>, Pair<Boolean, Keyword>>() {
            public Pair<Boolean, Keyword> call(Quadruple<String, String, String, Boolean> quadruple) throws Exception {
                return Pair.pair(quadruple.fourth(), toKeyword(quadruple));

            }
        };
    }

    private Keyword toKeyword(Triple<String, String, String> triple) throws ClassNotFoundException {
        String className = triple.third();
        Class aClass = classOf(className);
        ImmutableKeyword source = keyword(triple.first(), aClass);
        String alias = triple.second();
        if(!alias.isEmpty()){
            return source.as(keyword(alias, aClass));
        }
        return source;
    }

    private Class<?> classOf(String className) throws ClassNotFoundException {
        return Class.forName(className.split("#")[0]);
    }


    private Response put(final Keyword<Object> recordName, Sequence<Keyword> primaryKeys, final Sequence<Record> recordsToAdd) throws ParseException {
        records.put(recordName, update(using(primaryKeys), recordsToAdd));
        return redirect(resource(SearchResource.class).find(String.format("%s:%s", Lucene.RECORD_KEY, recordName)));
    }
}
