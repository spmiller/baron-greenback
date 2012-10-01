package com.googlecode.barongreenback;

import com.googlecode.barongreenback.actions.ActionsModule;
import com.googlecode.barongreenback.batch.BatchModule;
import com.googlecode.barongreenback.crawler.CrawlerModule;
import com.googlecode.barongreenback.crawler.executor.ExecutorModule;
import com.googlecode.barongreenback.crawler.failures.FailureModule;
import com.googlecode.barongreenback.jobs.JobsModule;
import com.googlecode.barongreenback.less.LessCssModule;
import com.googlecode.barongreenback.persistence.PersistenceModule;
import com.googlecode.barongreenback.queues.QueuesModule;
import com.googlecode.barongreenback.search.SearchModule;
import com.googlecode.barongreenback.shared.SharedModule;
import com.googlecode.barongreenback.views.ViewsModule;
import com.googlecode.utterlyidle.Application;
import com.googlecode.utterlyidle.BasePath;
import com.googlecode.utterlyidle.RestApplication;
import com.googlecode.utterlyidle.ServerConfiguration;
import com.googlecode.utterlyidle.handlers.GZipPolicy;
import com.googlecode.utterlyidle.modules.Module;
import com.googlecode.utterlyidle.modules.Modules;
import com.googlecode.utterlyidle.modules.PerformanceModule;
import com.googlecode.utterlyidle.profiling.ProfilingModule;
import com.googlecode.yadic.Container;

import java.util.Properties;

import static com.googlecode.totallylazy.URLs.packageUrl;
import static com.googlecode.utterlyidle.ApplicationBuilder.application;
import static com.googlecode.utterlyidle.MediaType.TEXT_HTML;
import static com.googlecode.utterlyidle.ServerConfiguration.defaultConfiguration;
import static com.googlecode.utterlyidle.dsl.DslBindings.bindings;
import static com.googlecode.utterlyidle.dsl.StaticBindingBuilder.in;
import static com.googlecode.utterlyidle.modules.Modules.bindingsModule;
import static com.googlecode.utterlyidle.sitemesh.ContentTypePredicate.contentType;
import static com.googlecode.utterlyidle.sitemesh.MetaTagRule.metaTagRule;
import static com.googlecode.utterlyidle.sitemesh.QueryParamRule.queryParamRule;
import static com.googlecode.utterlyidle.sitemesh.StaticDecoratorRule.staticRule;
import static com.googlecode.utterlyidle.sitemesh.StringTemplateDecorators.stringTemplateDecorators;
import static com.googlecode.utterlyidle.sitemesh.TemplateName.templateName;
import static java.lang.System.getProperties;

public class WebApplication extends RestApplication {
    public WebApplication(BasePath basePath, Properties properties) {
        super(basePath);
        add(Modules.applicationInstance(properties));
        addModules(this);
        add(stringTemplateDecorators(packageUrl(SharedModule.class),
                queryParamRule("decorator"),
                metaTagRule("decorator"),
                staticRule(contentType(TEXT_HTML), templateName("decorator"))));
        add(new ProfilingModule());
        // must come after sitemesh
        add(new PerformanceModule() {
            @Override
            public Container addPerRequestObjects(Container container) throws Exception {
                super.addPerRequestObjects(container);
                container.get(GZipPolicy.class).add(contentType(TEXT_HTML));
                return container;
            }
        });
    }

    public static void addModules(Application application) {
        application.add(new SharedModule());
        PersistenceModule.configure(application);
        application.add(new LessCssModule());
        application.add(new CrawlerModule());
        application.add(new FailureModule());
        application.add(new SearchModule());
        application.add(new ActionsModule());
        application.add(new ViewsModule());
        application.add(new JobsModule());
        application.add(new QueuesModule());
        application.add(new BatchModule());
        application.add(new ExecutorModule());
        application.add(bindingsModule(bindings(in(packageUrl(WebApplication.class)).path("baron-greenback"))));
    }

    public static void main(String[] args) throws Exception {
        ServerConfiguration config = defaultConfiguration().port(9000);
        application(new WebApplication(config.basePath(), getProperties())).start(config);
    }
}
