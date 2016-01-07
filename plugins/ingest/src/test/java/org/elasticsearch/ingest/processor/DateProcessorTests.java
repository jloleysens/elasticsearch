/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.ingest.processor;

import org.elasticsearch.ingest.core.IngestDocument;
import org.elasticsearch.ingest.RandomDocumentPicks;
import org.elasticsearch.test.ESTestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

public class DateProcessorTests extends ESTestCase {

    public void testJodaPattern() {
        DateProcessor dateProcessor = new DateProcessor(DateTimeZone.forID("Europe/Amsterdam"), Locale.ENGLISH,
                "date_as_string", Collections.singletonList("yyyy dd MM hh:mm:ss"), "date_as_date");
        Map<String, Object> document = new HashMap<>();
        document.put("date_as_string", "2010 12 06 11:05:15");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        dateProcessor.execute(ingestDocument);
        assertThat(ingestDocument.getFieldValue("date_as_date", String.class), equalTo("2010-06-12T11:05:15.000+02:00"));
    }

    public void testJodaPatternMultipleFormats() {
        List<String> matchFormats = new ArrayList<>();
        matchFormats.add("yyyy dd MM");
        matchFormats.add("dd/MM/yyyy");
        matchFormats.add("dd-MM-yyyy");
        DateProcessor dateProcessor = new DateProcessor(DateTimeZone.forID("Europe/Amsterdam"), Locale.ENGLISH,
                "date_as_string", matchFormats, "date_as_date");

        Map<String, Object> document = new HashMap<>();
        document.put("date_as_string", "2010 12 06");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        dateProcessor.execute(ingestDocument);
        assertThat(ingestDocument.getFieldValue("date_as_date", String.class), equalTo("2010-06-12T00:00:00.000+02:00"));

        document = new HashMap<>();
        document.put("date_as_string", "12/06/2010");
        ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        dateProcessor.execute(ingestDocument);
        assertThat(ingestDocument.getFieldValue("date_as_date", String.class), equalTo("2010-06-12T00:00:00.000+02:00"));

        document = new HashMap<>();
        document.put("date_as_string", "12-06-2010");
        ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        dateProcessor.execute(ingestDocument);
        assertThat(ingestDocument.getFieldValue("date_as_date", String.class), equalTo("2010-06-12T00:00:00.000+02:00"));

        document = new HashMap<>();
        document.put("date_as_string", "2010");
        ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        try {
            dateProcessor.execute(ingestDocument);
            fail("processor should have failed due to not supported date format");
        } catch(IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("unable to parse date [2010]"));
        }
    }

    public void testJodaPatternLocale() {
        DateProcessor dateProcessor = new DateProcessor(DateTimeZone.forID("Europe/Amsterdam"), Locale.ITALIAN,
                "date_as_string", Collections.singletonList("yyyy dd MMM"), "date_as_date");
        Map<String, Object> document = new HashMap<>();
        document.put("date_as_string", "2010 12 giugno");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        dateProcessor.execute(ingestDocument);
        assertThat(ingestDocument.getFieldValue("date_as_date", String.class), equalTo("2010-06-12T00:00:00.000+02:00"));
    }

    public void testJodaPatternDefaultYear() {
        DateProcessor dateProcessor = new DateProcessor(DateTimeZone.forID("Europe/Amsterdam"), Locale.ENGLISH,
                "date_as_string", Collections.singletonList("dd/MM"), "date_as_date");
        Map<String, Object> document = new HashMap<>();
        document.put("date_as_string", "12/06");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        dateProcessor.execute(ingestDocument);
        assertThat(ingestDocument.getFieldValue("date_as_date", String.class), equalTo(DateTime.now().getYear() + "-06-12T00:00:00.000+02:00"));
    }

    public void testTAI64N() {
        DateProcessor dateProcessor = new DateProcessor(DateTimeZone.forOffsetHours(2), randomLocale(random()),
                "date_as_string", Collections.singletonList(DateFormat.Tai64n.toString()), "date_as_date");
        Map<String, Object> document = new HashMap<>();
        String dateAsString = (randomBoolean() ? "@" : "") + "4000000050d506482dbdf024";
        document.put("date_as_string", dateAsString);
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        dateProcessor.execute(ingestDocument);
        assertThat(ingestDocument.getFieldValue("date_as_date", String.class), equalTo("2012-12-22T03:00:46.767+02:00"));
    }

    public void testUnixMs() {
        DateProcessor dateProcessor = new DateProcessor(DateTimeZone.UTC, randomLocale(random()),
                "date_as_string", Collections.singletonList(DateFormat.UnixMs.toString()), "date_as_date");
        Map<String, Object> document = new HashMap<>();
        document.put("date_as_string", "1000500");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        dateProcessor.execute(ingestDocument);
        assertThat(ingestDocument.getFieldValue("date_as_date", String.class), equalTo("1970-01-01T00:16:40.500Z"));
    }

    public void testUnix() {
        DateProcessor dateProcessor = new DateProcessor(DateTimeZone.UTC, randomLocale(random()),
                "date_as_string", Collections.singletonList(DateFormat.Unix.toString()), "date_as_date");
        Map<String, Object> document = new HashMap<>();
        document.put("date_as_string", "1000.5");
        IngestDocument ingestDocument = RandomDocumentPicks.randomIngestDocument(random(), document);
        dateProcessor.execute(ingestDocument);
        assertThat(ingestDocument.getFieldValue("date_as_date", String.class), equalTo("1970-01-01T00:16:40.500Z"));
    }
}
