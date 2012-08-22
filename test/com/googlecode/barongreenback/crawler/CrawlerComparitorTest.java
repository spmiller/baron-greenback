package com.googlecode.barongreenback.crawler;

import com.googlecode.barongreenback.ShowAndTell;
import com.googlecode.barongreenback.batch.BatchOperationsPage;
import com.googlecode.barongreenback.batch.BatchResourceTest;
import com.googlecode.barongreenback.search.SearchPage;
import com.googlecode.barongreenback.shared.ApplicationTests;
import com.googlecode.funclate.Model;
import com.googlecode.totallylazy.Option;
import com.googlecode.totallylazy.Pair;
import org.junit.Ignore;
import org.junit.Test;

import java.util.UUID;

import static com.googlecode.totallylazy.Pair.pair;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

public class CrawlerComparitorTest extends ApplicationTests {
    private static final UUID CRAWLER_ID = UUID.fromString("77916239-0dfe-4217-9e2a-ceaa9e5bed42");

    public Pair<String, String> compareSequentialAndQueues(String sequentialDefinition, String queuesDefinition) throws Exception {
        return compare(sequentialDefinition, SequentialCrawler.class, queuesDefinition, QueuesCrawler.class);
    }

    public Pair<String, String> compare(String definitionA, Class<? extends Crawler> crawlerA,
                                        String definitionB, Class<? extends Crawler> crawlerB) throws Exception {
        deleteAll();
        String nameA = extractName(definitionA);
        changeCrawlerTo(crawlerA);
        importAndCrawl(definitionA, nameA);
        String sequentialResult = csvExport(nameA);

        deleteAll();
        String nameB = extractName(definitionA);
        changeCrawlerTo(crawlerB);
        importAndCrawl(definitionB, nameB);
        String queuesResult = csvExport(nameB);

        return pair(sequentialResult, queuesResult);
    }

    private String extractName(String definition) {
        return Model.parse(definition).get("form", Model.class).get("update", String.class);
    }

    public void deleteAll() throws Exception {
        BatchResourceTest.verifySuccess(new BatchOperationsPage(browser).deleteAll());
    }

    public CrawlerListPage changeCrawlerTo(Class<? extends Crawler> crawlerClass) throws Exception {
        return new CrawlerListPage(browser).changeCrawler(crawlerClass);
    }

    public String csvExport(String name) throws Exception {
        return new SearchPage(browser, name, "").exportToCsv(name, "");
    }

    public String importAndCrawl(String definition, String name) throws Exception {
        CrawlerListPage crawlerListPage = new CrawlerImportPage(browser).importCrawler(definition, Option.some(CRAWLER_ID));
        assertThat(crawlerListPage.contains(name), is(true));
        return crawlerListPage.crawlAndWait(CRAWLER_ID);
    }

    @Test
    @Ignore("manual")
    public void compareCrawlers() throws Exception {
        String bbc = ShowAndTell.bbcDefinition();
        Pair<String, String> results = compareSequentialAndQueues(bbc, bbc);

//        Files.write(results.first().getBytes(), new File("/home/dev/Desktop/sequential.csv"));
//        Files.write(results.second().getBytes(), new File("/home/dev/Desktop/queues.csv"));

        assertEquals(results.first(), results.second());
    }
}
