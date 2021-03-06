/**
 * Copyright 2012 Nitor Creations Oy
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
 */
package com.nitorcreations.robotframework.eclipseide.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.nitorcreations.robotframework.eclipseide.structure.api.IDynamicParsedString;
import com.nitorcreations.robotframework.eclipseide.structure.api.IKeywordCall;
import com.nitorcreations.robotframework.eclipseide.structure.api.IKeywordSequence;
import com.nitorcreations.robotframework.eclipseide.structure.api.IParsedString;

public abstract class KeywordSequence implements IKeywordSequence {

    private List<IDynamicParsedString> documentationIMM;
    private IDynamicParsedString timeout;
    private IParsedString timeoutMessage;
    private List<IKeywordCall> keywordCalls;

    // immutable versions of above returned by getters
    private List<IKeywordCall> keywordCallsIMM;

    // singles

    public void setTimeout(IDynamicParsedString timeout) {
        this.timeout = timeout;
    }

    public void setTimeoutMessage(IParsedString timeoutMessage) {
        this.timeoutMessage = timeoutMessage;
    }

    // lists

    public void setDocumentation(List<? extends IDynamicParsedString> documentation) {
        this.documentationIMM = Collections.unmodifiableList(documentation);
    }

    public void addKeywordCall(IKeywordCall keywordCall) {
        if (this.keywordCalls == null) {
            this.keywordCalls = new ArrayList<IKeywordCall>();
            this.keywordCallsIMM = Collections.unmodifiableList(this.keywordCalls);
        }
        this.keywordCalls.add(keywordCall);
    }

    // getters

    @Override
    public abstract IParsedString getSequenceName();

    @Override
    public List<IDynamicParsedString> getDocumentation() {
        return documentationIMM;
    }

    @Override
    public IDynamicParsedString getTimeout() {
        return timeout;
    }

    @Override
    public IParsedString getTimeoutMessage() {
        return timeoutMessage;
    }

    @Override
    public List<IKeywordCall> getKeywordCalls() {
        return keywordCallsIMM;
    }

}
