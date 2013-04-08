package com.googlecode.barongreenback.search;

import com.googlecode.barongreenback.shared.pager.Pager;
import com.googlecode.barongreenback.shared.pager.RequestPager;
import com.googlecode.barongreenback.shared.sorter.Sorter;
import com.googlecode.funclate.StringFunclate;
import com.googlecode.lazyrecords.parser.*;
import com.googlecode.totallylazy.*;
import com.googlecode.totallylazy.time.*;
import com.googlecode.utterlyidle.HttpHandler;
import com.googlecode.utterlyidle.MediaType;
import com.googlecode.utterlyidle.Resources;
import com.googlecode.utterlyidle.handlers.ConvertExtensionToAcceptHeader;
import com.googlecode.utterlyidle.modules.ApplicationScopedModule;
import com.googlecode.utterlyidle.modules.ModuleDefiner;
import com.googlecode.utterlyidle.modules.ModuleDefinitions;
import com.googlecode.utterlyidle.modules.RequestScopedModule;
import com.googlecode.utterlyidle.modules.ResourcesModule;
import com.googlecode.yadic.Container;
import com.googlecode.yadic.Containers;

import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import static com.googlecode.totallylazy.time.Hours.functions.add;
import static com.googlecode.totallylazy.time.Hours.functions.subtract;
import static com.googlecode.utterlyidle.annotations.AnnotatedBindings.annotatedClass;
import static com.googlecode.utterlyidle.handlers.ConvertExtensionToAcceptHeader.Replacements.replacements;

public class SearchModule implements ParserFunctionsModule, ResourcesModule, ApplicationScopedModule, RequestScopedModule, ModuleDefiner, ParserParametersModule {
    public Resources addResources(Resources resources) {
        return resources.add(annotatedClass(SearchResource.class));
    }

    public Container addPerRequestObjects(Container container) throws Exception {
        container.add(Pager.class, RequestPager.class).
                add(Sorter.class, Sorter.class).
                add(ParserDateConverter.class).
                addActivator(PredicateParser.class, StandardParserActivator.class).
                decorate(PredicateParser.class, ParametrizedParser.class).
                add(PredicateBuilder.class).
                add(ParserParameters.class).
                add(ParserFunctions.class);
        return Containers.addInstanceIfAbsent(container, ConvertExtensionToAcceptHeader.Replacements.class, replacements(Pair.pair("json", MediaType.APPLICATION_JSON))).
                decorate(HttpHandler.class, ConvertExtensionToAcceptHeader.class);
    }

    public ModuleDefinitions defineModules(ModuleDefinitions moduleDefinitions) throws Exception {
        return moduleDefinitions.addRequestModule(ParserParametersModule.class).addRequestModule(ParserFunctionsModule.class);
    }

    @Override
    public ParserFunctions addFunctions(ParserFunctions parserFunctions, Container container) {
        final Properties properties = container.get(Properties.class);
        final DateConverter dateConverter = container.get(ParserDateConverter.class);

        return parserFunctions.
                add("addHours", Predicates.always(), StringFunclate.functions.both(Functions.<String, String, String>uncurry2(changeHours(add()).apply(dateConverter)))).
                add("subtractHours", Predicates.always(), StringFunclate.functions.both(Functions.<String, String, String>uncurry2(changeHours(subtract()).apply(dateConverter)))).
                add("properties", Predicates.always(), StringFunclate.functions.first(new UnaryFunction<String>() {
                    @Override
                    public String call(String key) throws Exception {
                        return properties.getProperty(key);
                    }
                }));
    }

    private static Function3<DateConverter, String, String, String> changeHours(final Function2<java.util.Date,java.lang.Integer,java.util.Date> function) {
        return new Function3<DateConverter, String, String, String>() {
            @Override
            public String call(DateConverter dateConverter, String dateAsString, String hoursAsString) throws Exception {
                Date parsedDate = dateConverter.parse(dateAsString);
                int parsedHours = Integer.parseInt(hoursAsString);

                return dateConverter.format(function.apply(parsedDate).apply(parsedHours));
            }
        };
    };

    @Override
    public Container addPerApplicationObjects(Container container) throws Exception {
        Containers.addIfAbsent(container, Properties.class);
        return container;
    }

    @Override
    public ParserParameters addParameters(ParserParameters parameters, Container container) {
        return parameters.add("now", container.get(Clock.class).now());
    }
}