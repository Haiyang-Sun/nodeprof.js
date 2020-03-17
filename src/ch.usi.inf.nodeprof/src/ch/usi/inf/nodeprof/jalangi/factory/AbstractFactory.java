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

import ch.usi.inf.nodeprof.utils.Logger;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.nodes.control.YieldException;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSCancelledExecutionException;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSUserObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.analysis.AnalysisFactory;
import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.utils.GlobalObjectCache;

// a factory corresponds to a callback defined in Jalangi-like analysis
public abstract class AbstractFactory implements
                AnalysisFactory<BaseEventHandlerNode> {
    // the jalangi analysis object
    protected final Object jalangiAnalysis;

    protected final DynamicObject pre;
    protected final DynamicObject post;

    protected final String jalangiCallback;

    protected static boolean readBoolean(DynamicObject cb, String name) {
        Object ret = readCBProperty(cb, name);
        if (ret == null) {
            return false;
        } else {
            return ret instanceof Boolean && (Boolean) ret; // unchecked
        }
    }

    @TruffleBoundary
    protected static String readString(DynamicObject cb, String name) {
        Object ret = readCBProperty(cb, name);
        if (ret == null) {
            return null;
        } else {
            return ret.toString();
        }
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
        if (ret == null) {
            return true;
        } else {
            return ret == Undefined.instance;
        }
    }

    protected static Object readCBProperty(DynamicObject cb, String name) {
        if (cb == null) {
            return null;
        }
        try {
            Object val = InteropLibrary.getFactory().getUncached(cb).readMember(cb, name);
            return val == null ? null : val;
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            return null;
        }
    }

    public AbstractFactory(String jalangiCallback, Object jalangiAnalysis, DynamicObject pre,
                    DynamicObject post) {
        this.jalangiCallback = jalangiCallback;
        this.jalangiAnalysis = jalangiAnalysis;
        this.pre = pre;
        this.post = post;
    }

    /**
     * Only interop type can be passed to JS
     *
     * @param result Object to be converted
     * @return the converted Object
     */
    public Object convertResult(Object result) {
        if (result == null) {
            return Null.instance;
        }
        return result;
    }

    @TruffleBoundary
    private static Object parseErrorObject(Throwable exception) {
        return exception instanceof GraalJSException ? ((GraalJSException) exception).getErrorObject() : exception.getMessage();
    }

    @TruffleBoundary
    protected static Object createWrappedException(Throwable exception) {
        if (exception == null) {
            return GlobalObjectCache.getInstance().getEmptyWrappedException();
        } else {
            JSContext ctx = GlobalObjectCache.getInstance().getJSContext();
            DynamicObject wrapped = JSUserObject.create(ctx);
            if (exception instanceof YieldException) {
                JSObject.set(wrapped, "yield", true);
            } else {
                Object errObj = parseErrorObject(exception);
                JSObject.set(wrapped, "exception", errObj == null ? "Unknown Exception" : errObj);

            }
            return wrapped;
        }

    }

    /**
     * nestedControl is a tag to avoid instrumentation of the Jalangi analysis being called
     * recursively
     */
    private static boolean nestedControl = false;

    /**
     * call from Java to Jalangi JavaScript using Interop
     *
     * @param lib interop library object
     * @param receiver the receiver of the call
     * @param arguments the arguments of the call
     */
    public void wrappedDispatchExecution(BaseEventHandlerNode handler, InteropLibrary lib, Object receiver, Object... arguments) throws InteropException {
        if (!nestedControl) {
            nestedControl = true;

            try {
                Object ret = lib.execute(receiver, arguments);
                if (ret != Undefined.instance && JSObject.isJSObject(ret)) {
                    Object prop = JSObject.get((DynamicObject) ret, "deactivate");
                    if (prop.equals(true)) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        handler.deactivate();
                    }
                }
            } catch (JSCancelledExecutionException e) {
                Logger.error(arguments[0], "execution cancelled probably due to timeout");
            } catch (InteropException e) {
                throw e;
            } finally {
                nestedControl = false;
            }
        }
    }

    public InteropLibrary createDispatchNode() {
        return InteropLibrary.getFactory().create(this.jalangiAnalysis);
    }
}
