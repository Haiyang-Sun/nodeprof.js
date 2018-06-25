/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
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
 *******************************************************************************/
package ch.usi.inf.nodeprof.jalangi.factory;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Null;

import ch.usi.inf.nodeprof.analysis.AnalysisFactory;
import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.utils.Logger;

public abstract class AbstractFactory implements
                AnalysisFactory<BaseEventHandlerNode> {
    protected final Object jalangiAnalysis;
    protected final DynamicObject pre;
    protected final DynamicObject post;
    protected final String jalangiCallback;

    public AbstractFactory(String jalangiCallback, Object jalangiAnalysis, DynamicObject pre,
                    DynamicObject post) {
        this.jalangiAnalysis = jalangiAnalysis;
        this.pre = pre;
        this.post = post;
        this.jalangiCallback = jalangiCallback;
    }

    @TruffleBoundary
    public static DirectCallNode createDirectCallNode(DynamicObject func) {
        return func == null ? null
                        : Truffle.getRuntime().createDirectCallNode(
                                        JSFunction.getCallTarget(func));
    }

    protected DirectCallNode createPreCallNode() {
        return createDirectCallNode(pre);
    }

    protected DirectCallNode createPostCallNode() {
        return createDirectCallNode(post);
    }

    /**
     * We cannot pass some objects to
     *
     * @param result Object to be converted
     * @return the converted Object
     */
    public Object convertResult(Object result) {
        if (result == null)
            return Null.instance;
        return result;
    }

    private static boolean nestedControl = false;

    /**
     * TODO: could add a tag here to avoid instrumentation of the Jalangi analysis being called
     * recursively
     *
     * call from Java to Jalangi JavaScript
     *
     * @param callNode
     * @param args
     * @param isPre TODO
     */
    protected void directCall(DirectCallNode callNode, Object[] args, boolean isPre, int iid) {
        if (nestedControl)
            return;
        nestedControl = true;
        try {
            callNode.call(args);
        } catch (GraalJSException e) {
            Logger.error(iid, "error happened in event handler " + this.jalangiCallback + "[" + (isPre ? "Pre" : "Post") + "]");
            Logger.dumpException(e);
        }
        nestedControl = false;
    }

}
