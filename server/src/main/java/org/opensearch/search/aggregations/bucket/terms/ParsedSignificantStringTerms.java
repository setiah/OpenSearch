/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

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

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.search.aggregations.bucket.terms;

import org.apache.lucene.util.BytesRef;
import org.opensearch.common.xcontent.ObjectParser;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentParser;

import java.io.IOException;
import java.nio.CharBuffer;

public class ParsedSignificantStringTerms extends ParsedSignificantTerms {

    @Override
    public String getType() {
        return SignificantStringTerms.NAME;
    }

    private static final ObjectParser<ParsedSignificantStringTerms, Void> PARSER =
            new ObjectParser<>(ParsedSignificantStringTerms.class.getSimpleName(), true, ParsedSignificantStringTerms::new);
    static {
        declareParsedSignificantTermsFields(PARSER, ParsedBucket::fromXContent);
    }

    public static ParsedSignificantStringTerms fromXContent(XContentParser parser, String name) throws IOException {
        return parseSignificantTermsXContent(() -> PARSER.parse(parser, null), name);
    }

    public static class ParsedBucket extends ParsedSignificantTerms.ParsedBucket {

        private BytesRef key;

        @Override
        public Object getKey() {
            return getKeyAsString();
        }

        @Override
        public String getKeyAsString() {
            String keyAsString = super.getKeyAsString();
            if (keyAsString != null) {
                return keyAsString;
            }
            return key.utf8ToString();
        }

        public Number getKeyAsNumber() {
            return Double.parseDouble(key.utf8ToString());
        }

        @Override
        protected XContentBuilder keyToXContent(XContentBuilder builder) throws IOException {
            return builder.field(CommonFields.KEY.getPreferredName(), getKey());
        }

        static ParsedBucket fromXContent(XContentParser parser) throws IOException {
            return parseSignificantTermsBucketXContent(parser, new ParsedBucket(), (p, bucket) -> {
                CharBuffer cb = p.charBufferOrNull();
                if (cb == null) {
                    bucket.key = null;
                } else {
                    bucket.key = new BytesRef(cb);
                }
            });
        }
    }
}
