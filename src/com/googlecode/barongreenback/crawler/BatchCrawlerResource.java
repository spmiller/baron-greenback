package com.googlecode.barongreenback.crawler;

import com.googlecode.barongreenback.shared.InvocationHandler;
import com.googlecode.barongreenback.shared.ModelRepository;
import com.googlecode.funclate.Model;
import com.googlecode.totallylazy.Callable1;
import com.googlecode.totallylazy.Pair;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.utterlyidle.Application;
import com.googlecode.utterlyidle.MediaType;
import com.googlecode.utterlyidle.Redirector;
import com.googlecode.utterlyidle.Response;
import com.googlecode.utterlyidle.annotations.POST;
import com.googlecode.utterlyidle.annotations.Path;
import com.googlecode.utterlyidle.annotations.Produces;

import java.util.UUID;

import static com.googlecode.barongreenback.crawler.CrawlerDefinitionResource.scheduleAQueuedCrawl;
import static com.googlecode.barongreenback.shared.ModelRepository.MODEL_TYPE;
import static com.googlecode.totallylazy.Callables.first;
import static com.googlecode.totallylazy.Predicates.is;
import static com.googlecode.totallylazy.Predicates.where;
import static com.googlecode.totallylazy.proxy.Call.method;
import static com.googlecode.totallylazy.proxy.Call.on;
import static com.googlecode.utterlyidle.RequestBuilder.post;

@Path("crawler")
@Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON})
public class BatchCrawlerResource {

    private InvocationHandler invocationHandler;
    private final ModelRepository modelRepository;
    private final Redirector redirector;
    private Application application;
    private final CrawlInterval interval;

    public BatchCrawlerResource(final InvocationHandler invocationHandler, final ModelRepository modelRepository, final Redirector redirector, final Application application, final CrawlInterval interval) {
        this.invocationHandler = invocationHandler;
        this.modelRepository = modelRepository;
        this.redirector = redirector;
        this.application = application;
        this.interval = interval;
    }


    @POST
    @Path("crawlAll")
    public Response crawlAll() throws Exception {
        return forAll(ids(), crawl());
    }

    @POST
    @Path("resetAll")
    public Response resetAll() throws Exception {
        return forAll(ids(), reset());
    }

    @POST
    @Path("deleteAll")
    public Response deleteAll() throws Exception {
        return forAll(ids(), delete());
    }

    private Sequence<UUID> ids() {
        return allCrawlerModels().map(first(UUID.class));
    }

    public static <T> Response forAll(final Sequence<T> sequence, final Callable1<T, Response> callable) throws Exception {
        return sequence.map(callable).last();
    }

    public Callable1<UUID, Response> crawl() {
        return new Callable1<UUID, Response>() {
            public Response call(UUID uuid) throws Exception {
                return application.handle(post(scheduleAQueuedCrawl(uuid, uuid, interval.value())).form("id", uuid).build());
            }
        };
    }

    public Callable1<UUID, Response> reset() {
        return new Callable1<UUID, Response>() {
            public Response call(UUID uuid) throws Exception {
                return invocationHandler.handle(method(on(CrawlerDefinitionResource.class).reset(uuid)));
            }
        };
    }

    public Callable1<UUID, Response> delete() {
        return new Callable1<UUID, Response>() {
            public Response call(UUID uuid) throws Exception {
                return invocationHandler.handle(method(on(CrawlerDefinitionResource.class).delete(uuid)));
            }
        };
    }

    private Sequence<Pair<UUID, Model>> allCrawlerModels() {
        return modelRepository.find(where(MODEL_TYPE, is("form")));
    }


}
