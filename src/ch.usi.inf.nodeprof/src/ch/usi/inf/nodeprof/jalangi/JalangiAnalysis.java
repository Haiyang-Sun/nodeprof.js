/* *****************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package ch.usi.inf.nodeprof.jalangi;

import static ch.usi.inf.nodeprof.ProfiledTagEnum.BINARY;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.BUILTIN;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.CF_BRANCH;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.CF_ROOT;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.DECLARE;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.ELEMENT_READ;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.ELEMENT_WRITE;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.EVAL;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.EXPRESSION;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.INVOKE;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.LITERAL;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.NEW;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.PROPERTY_READ;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.PROPERTY_WRITE;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.ROOT;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.STATEMENT;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.UNARY;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.VAR_READ;
import static ch.usi.inf.nodeprof.ProfiledTagEnum.VAR_WRITE;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.object.DynamicObject;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import ch.usi.inf.nodeprof.jalangi.factory.AsyncRootFactory;
import ch.usi.inf.nodeprof.jalangi.factory.AwaitFactory;
import ch.usi.inf.nodeprof.jalangi.factory.BinaryFactory;
import ch.usi.inf.nodeprof.jalangi.factory.BuiltinFactory;
import ch.usi.inf.nodeprof.jalangi.factory.ConditionalFactory;
import ch.usi.inf.nodeprof.jalangi.factory.DeclareFactory;
import ch.usi.inf.nodeprof.jalangi.factory.EvalFactory;
import ch.usi.inf.nodeprof.jalangi.factory.EvalFunctionFactory;
import ch.usi.inf.nodeprof.jalangi.factory.ExpressionFactory;
import ch.usi.inf.nodeprof.jalangi.factory.ForObjectFactory;
import ch.usi.inf.nodeprof.jalangi.factory.GetElementFactory;
import ch.usi.inf.nodeprof.jalangi.factory.GetFieldFactory;
import ch.usi.inf.nodeprof.jalangi.factory.InitialRootFactory;
import ch.usi.inf.nodeprof.jalangi.factory.InvokeFactory;
import ch.usi.inf.nodeprof.jalangi.factory.LiteralFactory;
import ch.usi.inf.nodeprof.jalangi.factory.LoopFactory;
import ch.usi.inf.nodeprof.jalangi.factory.PutElementFactory;
import ch.usi.inf.nodeprof.jalangi.factory.PutFieldFactory;
import ch.usi.inf.nodeprof.jalangi.factory.ReadFactory;
import ch.usi.inf.nodeprof.jalangi.factory.ReturnFactory;
import ch.usi.inf.nodeprof.jalangi.factory.RootFactory;
import ch.usi.inf.nodeprof.jalangi.factory.StatementFactory;
import ch.usi.inf.nodeprof.jalangi.factory.UnaryFactory;
import ch.usi.inf.nodeprof.jalangi.factory.WriteFactory;
import ch.usi.inf.nodeprof.utils.GlobalConfiguration;
import ch.usi.inf.nodeprof.utils.GlobalObjectCache;
import ch.usi.inf.nodeprof.utils.Logger;

/**
 * Java representation of the Jalangi analysis object created in Jalangi ChainedAnalysisNoCheck
 */
public class JalangiAnalysis {
    /**
     * mapping from the callback names to the function
     */
    private final HashMap<String, DynamicObject> callbacks;

    /**
     * The Jalangi analysis object
     *
     * this will be used as the receiver of the callback events
     *
     */
    public final Object jsAnalysis;

    /**
     * The instrument
     */
    final NodeProfJalangi instrument;

    @SuppressWarnings("serial") public static final Map<String, EnumSet<ProfiledTagEnum>> callbackMap = Collections.unmodifiableMap(new HashMap<String, EnumSet<ProfiledTagEnum>>() {
        {
            // function calls
            put("functionEnter", EnumSet.of(ROOT));
            put("functionExit", EnumSet.of(ROOT));
            put("invokeFunPre", EnumSet.of(INVOKE, NEW));
            put("invokeFun", EnumSet.of(INVOKE, NEW));

            // builtin calls
            put("builtinEnter", EnumSet.of(BUILTIN));
            put("builtinExit", EnumSet.of(BUILTIN));

            // literals
            put("literal", EnumSet.of(LITERAL));
            put("declarePre", EnumSet.of(DECLARE));
            put("declare", EnumSet.of(DECLARE));

            // reads and writes
            put("read", EnumSet.of(VAR_READ, PROPERTY_READ));
            put("write", EnumSet.of(VAR_WRITE, PROPERTY_WRITE));

            // property reads
            put("getFieldPre", EnumSet.of(PROPERTY_READ, ELEMENT_READ));
            put("getField", EnumSet.of(PROPERTY_READ, ELEMENT_READ));

            // property writes
            put("putFieldPre", EnumSet.of(PROPERTY_WRITE, ELEMENT_WRITE));
            put("putField", EnumSet.of(PROPERTY_WRITE, ELEMENT_WRITE));

            // operators
            put("unaryPre", EnumSet.of(UNARY));
            put("unary", EnumSet.of(UNARY));
            put("binaryPre", EnumSet.of(BINARY));
            put("binary", EnumSet.of(BINARY));

            // conditions
            put("conditional", EnumSet.of(CF_BRANCH));

            // eval-like
            put("evalPre", EnumSet.of(EVAL));
            put("evalPost", EnumSet.of(EVAL));
            put("evalFunctionPre", EnumSet.of(BUILTIN));
            put("evalFunctionPost", EnumSet.of(BUILTIN));

            put("asyncFunctionEnter", EnumSet.of(CF_ROOT));
            put("asyncFunctionExit", EnumSet.of(CF_ROOT));

            put("forObject", EnumSet.of(CF_ROOT));

            put("_return", EnumSet.of(CF_BRANCH));

            put("awaitPre", EnumSet.of(CF_BRANCH));
            put("awaitPost", EnumSet.of(CF_BRANCH));

            put("startExpression", EnumSet.of(EXPRESSION));
            put("endExpression", EnumSet.of(EXPRESSION));
            put("startStatement", EnumSet.of(STATEMENT));
            put("endStatement", EnumSet.of(STATEMENT));

            put("newSource", EnumSet.of(ROOT));
        }
    });

    public static final Set<String> unimplementedCallbacks = Collections.unmodifiableSet(
                    new HashSet<>(Arrays.asList("forinObject",
                                    "instrumentCodePre", "instrumentCode", // TODO will those be
                                                                           // supported at all?
                                    "onReady", // TODO should this be ignored instead
                                    "runInstrumentedFunctionBody", "scriptEnter", "scriptExit", "_throw", "_with")));

    public static final Set<String> ignoredCallbacks = Collections.unmodifiableSet(
                    // endExecution is a high-level event handled by the jalangi.js script
                    new HashSet<>(Arrays.asList("endExecution")));

    @TruffleBoundary
    public JalangiAnalysis(NodeProfJalangi nodeprofJalangi, Object jsAnalysis) {
        this.instrument = nodeprofJalangi;
        this.jsAnalysis = jsAnalysis;
        this.callbacks = new HashMap<>();
    }

    @TruffleBoundary
    public void onReady() {
        if (GlobalConfiguration.DEBUG) {
            Logger.debug("analysis is ready " + callbacks.keySet());
        }
        if (this.callbacks.containsKey("invokeFunPre") || callbacks.containsKey("invokeFun")) {
            InvokeFactory invokeFactory = new InvokeFactory(this.jsAnalysis, ProfiledTagEnum.INVOKE, callbacks.get("invokeFunPre"), callbacks.get("invokeFun"));
            this.instrument.onCallback(
                            ProfiledTagEnum.INVOKE,
                            invokeFactory);
            InvokeFactory newFactory = new InvokeFactory(this.jsAnalysis, ProfiledTagEnum.NEW, callbacks.get("invokeFunPre"), callbacks.get("invokeFun"));
            this.instrument.onCallback(
                            ProfiledTagEnum.NEW,
                            newFactory);
            this.instrument.onCallback(
                            ProfiledTagEnum.EVAL,
                            new EvalFactory(this.jsAnalysis, callbacks.get("invokeFunPre"), callbacks.get("invokeFun"), true));
        }

        if (this.callbacks.containsKey("putFieldPre") || callbacks.containsKey("putField")) {
            this.instrument.onCallback(
                            ProfiledTagEnum.PROPERTY_WRITE,
                            new PutFieldFactory(this.jsAnalysis, callbacks.get("putFieldPre"), callbacks.get("putField")));
            this.instrument.onCallback(
                            ProfiledTagEnum.ELEMENT_WRITE,
                            new PutElementFactory(this.jsAnalysis, callbacks.get("putFieldPre"), callbacks.get("putField")));
        }

        if (this.callbacks.containsKey("getFieldPre") || callbacks.containsKey("getField")) {
            this.instrument.onCallback(
                            ProfiledTagEnum.PROPERTY_READ,
                            new GetFieldFactory(this.jsAnalysis, callbacks.get("getFieldPre"), callbacks.get("getField")));
            this.instrument.onCallback(
                            ProfiledTagEnum.ELEMENT_READ,
                            new GetElementFactory(this.jsAnalysis, callbacks.get("getFieldPre"), callbacks.get("getField")));
        }

        if (this.callbacks.containsKey("read")) {
            this.instrument.onCallback(ProfiledTagEnum.VAR_READ, new ReadFactory(
                            this.jsAnalysis, callbacks.get("read"), false));
            this.instrument.onCallback(ProfiledTagEnum.PROPERTY_READ, new ReadFactory(
                            this.jsAnalysis, callbacks.get("read"), true));
        }

        if (this.callbacks.containsKey("write")) {
            this.instrument.onCallback(ProfiledTagEnum.VAR_WRITE, new WriteFactory(
                            this.jsAnalysis, callbacks.get("write"), false));
            this.instrument.onCallback(ProfiledTagEnum.PROPERTY_WRITE,
                            new WriteFactory(this.jsAnalysis, callbacks.get("write"),
                                            true));
        }

        if (this.callbacks.containsKey("binaryPre") || this.callbacks.containsKey("binary")) {
            this.instrument.onCallback(ProfiledTagEnum.BINARY,
                            new BinaryFactory(this.jsAnalysis,
                                            callbacks.get("binaryPre"),
                                            callbacks.get("binary")));
        }

        if (this.callbacks.containsKey("literal")) {
            this.instrument.onCallback(ProfiledTagEnum.LITERAL, new LiteralFactory(
                            this.jsAnalysis, callbacks.get("literal")));
        }

        if (this.callbacks.containsKey("declarePre") || this.callbacks.containsKey("declare")) {
            this.instrument.onCallback(ProfiledTagEnum.DECLARE, new DeclareFactory(
                            this.jsAnalysis, callbacks.get("declarePre"), callbacks.get("declare")));
        }

        if (this.callbacks.containsKey("unaryPre") || this.callbacks.containsKey("unary")) {
            this.instrument.onCallback(ProfiledTagEnum.UNARY,
                            new UnaryFactory(this.jsAnalysis, callbacks.get("unaryPre"),
                                            callbacks.get("unary")));
        }

        if (this.callbacks.containsKey("conditional")) {
            this.instrument.onCallback(ProfiledTagEnum.CF_BRANCH, new ConditionalFactory(
                            this.jsAnalysis, callbacks.get("conditional"), false));
            this.instrument.onCallback(
                            ProfiledTagEnum.BINARY,
                            new ConditionalFactory(this.jsAnalysis, callbacks.get("conditional"), true));
        }

        /*
         * functionEnter/Exit callback: instruments root nodes of functions
         */
        if (this.callbacks.containsKey("functionEnter") || this.callbacks.containsKey("functionExit")) {
            this.instrument.onCallback(
                            ProfiledTagEnum.ROOT,
                            new RootFactory(this.jsAnalysis,
                                            callbacks.get("functionEnter"),
                                            callbacks.get("functionExit"),
                                            this.instrument.getEnv()));
        }

        if (this.callbacks.containsKey("startExpression") || this.callbacks.containsKey("endExpression")) {
            this.instrument.onCallback(
                            ProfiledTagEnum.EXPRESSION,
                            new ExpressionFactory(this.jsAnalysis,
                                            callbacks.get("startExpression"), callbacks.get("endExpression")));
        }

        if (this.callbacks.containsKey("startStatement") || this.callbacks.containsKey("endStatement")) {
            this.instrument.onCallback(
                            ProfiledTagEnum.STATEMENT,
                            new StatementFactory(this.jsAnalysis,
                                            callbacks.get("startStatement"), callbacks.get("endStatement")));
        }

        if (this.callbacks.containsKey("builtinEnter") || this.callbacks.containsKey("builtinExit")) {
            this.instrument.onCallback(
                            ProfiledTagEnum.BUILTIN,
                            new BuiltinFactory(this.jsAnalysis,
                                            callbacks.get("builtinEnter"), callbacks.get("builtinExit"), null));
        }

        /*
         * TODO
         *
         * Eval not tested
         */
        if (this.callbacks.containsKey("evalPre") || this.callbacks.containsKey("evalPost")) {
            this.instrument.onCallback(
                            ProfiledTagEnum.EVAL,
                            new EvalFactory(this.jsAnalysis, callbacks.get("evalPre"), callbacks.get("evalPost"), false));
        }

        /*
         *
         * new Function("XXX"); not tested
         */
        if (this.callbacks.containsKey("evalFunctionPre") || this.callbacks.containsKey("evalFunctionPost")) {
            this.instrument.onCallback(
                            ProfiledTagEnum.BUILTIN,
                            new EvalFunctionFactory(this.jsAnalysis, callbacks.get("evalFunctionPre"), callbacks.get("evalFunctionPost")));
        }

        if (this.callbacks.containsKey("forObject")) {
            this.instrument.onCallback(
                            ProfiledTagEnum.CF_ROOT,
                            new ForObjectFactory(this.jsAnalysis, callbacks.get("forObject")));
        }

        /*
         * async function
         */
        if (this.callbacks.containsKey("asyncFunctionEnter") || this.callbacks.containsKey("asyncFunctionExit")) {
            this.instrument.onCallback(
                            ProfiledTagEnum.CF_ROOT,
                            new AsyncRootFactory(this.jsAnalysis, callbacks.get("asyncFunctionEnter"), callbacks.get("asyncFunctionExit")));
        }
        /*
         * await callback
         */
        if (this.callbacks.containsKey("awaitPre") || this.callbacks.containsKey("awaitPost")) {
            this.instrument.onCallback(
                            ProfiledTagEnum.CF_BRANCH,
                            new AwaitFactory(this.jsAnalysis, callbacks.get("awaitPre"), callbacks.get("awaitPost")));
        }

        /*
         * TODO
         *
         * Loop not tested
         */
        if (this.callbacks.containsKey("loopEnter") || this.callbacks.containsKey("loopExit")) {
            this.instrument.onCallback(
                            ProfiledTagEnum.CF_ROOT,
                            new LoopFactory(this.jsAnalysis, callbacks.get("loopEnter"), callbacks.get("loopExit")));
        }

        /*
         * _return callback
         */
        if (this.callbacks.containsKey("_return")) {
            this.instrument.onCallback(
                            ProfiledTagEnum.CF_BRANCH,
                            new ReturnFactory(this.jsAnalysis, callbacks.get("_return")));
        }

        /*
         * Initial function root handler: collects information special functions (e.g. modules) and
         * provides newSource callback. This instrumentation is always enabled but deactivates after
         * its first execution.
         */
        this.instrument.onCallback(
                        ProfiledTagEnum.ROOT,
                        new InitialRootFactory(this.jsAnalysis, callbacks.get("newSource")));
    }

    /**
     * register hooks
     *
     * @param name of the hook
     * @throws UnsupportedTypeException
     * @callback function to be called for the specified hook
     */
    @TruffleBoundary
    public void registerCallback(Object name, Object callback) throws UnsupportedTypeException {
        if (callback instanceof DynamicObject) {
            if (GlobalConfiguration.DEBUG) {
                Logger.debug("Jalangi analysis registering callback: " + name);
            }
            if (unimplementedCallbacks.contains(name)) {
                Logger.warning("Jalangi analysis callback not implemented in NodeProf: " + name);
            }
            GlobalObjectCache.getInstance().addDynamicObject((DynamicObject) callback);
            this.callbacks.put(name.toString(), (DynamicObject) callback);
        } else {
            throw UnsupportedTypeException.create(new Object[]{callback});
        }
    }
}
