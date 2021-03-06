package uk.co.flax.luwak.util;

/*
 *   Copyright (c) 2015 Lemur Consulting Ltd.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanWeight;
import org.apache.lucene.search.spans.Spans;

/**
 * SpanQuery that wraps another SpanQuery, ensuring that offsets are loaded
 * from the postings lists and exposed to SpanCollectors.
 */
public class SpanOffsetReportingQuery extends SpanQuery {

    private final SpanQuery in;

    /**
     * Create a new SpanOffsetReportingQuery
     * @param in the query to wrap
     */
    public SpanOffsetReportingQuery(SpanQuery in) {
        this.in = in;
    }

    /**
     * @return the wrapped query
     */
    public SpanQuery getWrappedQuery() {
        return in;
    }

    @Override
    public String getField() {
        return in.getField();
    }

    @Override
    public String toString(String field) {
        return in.toString();
    }

    @Override
    public Query rewrite(IndexReader reader) throws IOException {
        SpanQuery rewritten = (SpanQuery) in.rewrite(reader);
        if (in == rewritten)
            return this;
        return new SpanOffsetReportingQuery((SpanQuery)in.rewrite(reader));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpanOffsetReportingQuery that = (SpanOffsetReportingQuery) o;
        return Objects.equals(in, that.in);
    }

    @Override
    public int hashCode() {
        return Objects.hash(in);
    }

    @Override
    public SpanWeight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
        return new SpanOffsetWeight(searcher, in.createWeight(searcher, needsScores, boost), boost);
    }

    /**
     * Build a map of terms to termcontexts, for use in constructing SpanWeights
     * @lucene.internal
     */
    private static Map<Term, TermContext> termContexts(SpanWeight... weights) {
        Map<Term, TermContext> terms = new TreeMap<>();
        for (SpanWeight w : weights) {
            w.extractTermContexts(terms);
        }
        return terms;
    }

    private class SpanOffsetWeight extends SpanWeight {

        private final SpanWeight in;

        private SpanOffsetWeight(IndexSearcher searcher, SpanWeight in, float boost) throws IOException {
            super(SpanOffsetReportingQuery.this, searcher, termContexts(in), boost);
            this.in = in;
        }

        @Override
        public void extractTermContexts(Map<Term, TermContext> contexts) {
            in.extractTermContexts(contexts);
        }

        @Override
        public Spans getSpans(LeafReaderContext ctx, Postings requiredPostings) throws IOException {
            return in.getSpans(ctx, Postings.OFFSETS);
        }

        @Override
        public void extractTerms(Set<Term> terms) {
            in.extractTerms(terms);
        }

        @Override
        public boolean isCacheable(LeafReaderContext ctx) {
            // TODO Auto-generated method stub
            return false;
        }
    }
}
