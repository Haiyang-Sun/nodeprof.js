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
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSCancelledExecutionException;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.analysis.AnalysisFactory;
import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.utils.Logger;

public abstract class AbstractFactory implements
                AnalysisFactory<BaseEventHandlerNode> {
    protected final Object jalangiAnalysis;

    protected final DynamicObject pre;
    protected final DynamicObject post;

    // for a given callback, the arguments have the same layout
    protected final Object[] preArguments;
    protected final Object[] postArguments;

    protected final String jalangiCallback;

    // used to read the callback object for configuration
    protected static final Node read = Message.READ.createNode();

    protected static boolean readBoolean(DynamicObject cb, String name) {
        Object ret = readCBProperty(cb, name);
        if (ret == null)
            return false;
        else
            return ret instanceof Boolean && (Boolean) ret; // unchecked
    }

    @TruffleBoundary
    protected static String readString(DynamicObject cb, String name) {
        Object ret = readCBProperty(cb, name);
        if (ret == null)
            return null;
        else
            return ret.toString();
    }

    @TruffleBoundary
    protected static Object[] readArray(DynamicObject cb, String name) {
        Object ret = readCBProperty(cb, name);
        if (JSArray.isJSArray(ret)) {
            return JSAbstractArray.toArray((DynamicObject) ret);
        }
        return null;
    }

    @TruffleBoundary
    protected static boolean isPropertyUndefined(DynamicObject cb, String name) {
        Object ret = readCBProperty(cb, name);
        if (ret == null)
            return true;
        else
            return ret == Undefined.instance;
    }

    protected static Object readCBProperty(DynamicObject cb, String name) {
        if (cb == null)
            return null;
        try {
            Object val = ForeignAccess.sendRead(read, cb, name);
            return val == null ? null : val;
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            return null;
        }
    }

    public AbstractFactory(String jalangiCallback, Object jalangiAnalysis, DynamicObject pre,
                    DynamicObject post, int numPreArguments, int numPostArguments) {
        this.jalangiCallback = jalangiCallback;
        this.jalangiAnalysis = jalangiAnalysis;
        this.pre = pre;
        this.post = post;
        if (this.pre != null) {
            this.preArguments = new Object[2 + numPreArguments];
            this.preArguments[0] = this.jalangiAnalysis;
            this.preArguments[1] = this.pre;
        } else {
            this.preArguments = null;
        }
        if (this.post != null) {
            this.postArguments = new Object[2 + numPostArguments];
            this.postArguments[0] = this.jalangiAnalysis;
            this.postArguments[1] = this.post;
        } else {
            this.postArguments = null;
        }
    }

    protected void setPreArguments(int index, Object value) {
        if (this.pre != null) {
            assert this.preArguments.length > index + 2;
            this.preArguments[index + 2] = value;
        }
    }

    protected void setPostArguments(int index, Object value) {
        if (this.post != null) {
            assert this.postArguments.length > index + 2;
            this.postArguments[index + 2] = value;
        }
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
     * Only interop type can be passed to JS
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
     * nestedControl is a tag to avoid instrumentation of the Jalangi analysis being called
     * recursively
     *
     * call from Java to Jalangi JavaScript
     *
     * @param callNode
     * @param isPre pre or post callback
     * @param iid source section id
     */
    protected void directCall(DirectCallNode callNode, boolean isPre, int iid) {
        if (nestedControl)
            return;
        nestedControl = true;
        try {
            callNode.call(isPre ? preArguments : postArguments);
        } catch (GraalJSException e) {
            Logger.error(iid, "error happened in event handler " + this.jalangiCallback + "[" + (isPre ? "Pre" : "Post") + "]");
            Logger.dumpException(e);
        } catch (JSCancelledExecutionException e) {
            Logger.error(iid, "execution cancelled probably due to timeout");
        } catch (Exception e) {
            Logger.error(iid, "unknown exception happened in event handler " + this.jalangiCallback + "[" + (isPre ? "Pre" : "Post") + "]" + " " + e.getClass().getSimpleName());
            throw e;
        } finally {
            nestedControl = false;
        }
    }

    @TruffleBoundary
    protected static Object parseErrorObject(Throwable exception) {
        return exception instanceof GraalJSException ? ((GraalJSException) exception).getErrorObject() : exception.getMessage();
    }
}
