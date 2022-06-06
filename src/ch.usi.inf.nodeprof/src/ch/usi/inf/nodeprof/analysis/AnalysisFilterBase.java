/* *****************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * *****************************************************************************/
package ch.usi.inf.nodeprof.analysis;

import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;

import ch.usi.inf.nodeprof.ProfiledTagEnum;

/**
 * Base for NodeProf's analysis filters
 *
 * Filter combines a source predicate with a second-level filter (see testTag()).
 */
public abstract class AnalysisFilterBase implements SourceSectionFilter.SourcePredicate {

    /**
     * Implementation returns true if the instrumentation tag should be added to source, false if
     * the tag should be filtered.
     *
     * @param source the Source object to filter
     * @param tag the type of tag to filter within source
     */
    public abstract boolean testTag(Source source, ProfiledTagEnum tag);

    /**
     * check whether beginning of source file contains DO NOT INSTRUMENT
     *
     * @param source the Source to test
     * @return true if DO NOT INSTRUMENT string found in source
     */
    static boolean containsDoNotInstrument(final Source source) {
        if (source.getLineCount() > 0) {
            // check if the source code has a special filter string at its beginning
            CharSequence sourceChars = source.getCharacters();
            String sourceHead = sourceChars.subSequence(0, Math.min(sourceChars.length() - 1, 1000)).toString().trim();
            // should be enough
            if (sourceHead.contains("DO NOT INSTRUMENT")) {
                return true;
            }
        }
        return false;
    }

    /**
     * check whether source is not JavaScript but another language (e.g. regex)
     *
     * @param source the Source to test
     * @return true if not a JavaScript source
     */
    static boolean isForeignSource(final Source source) {
        return !JavaScriptLanguage.ID.equals(source.getLanguage());
    }

    /**
     * @return a string describing the filter
     */
    public abstract String getDescription();
}
