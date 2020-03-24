/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package ch.usi.inf.nodeprof.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ListIterator;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import ch.usi.inf.nodeprof.ProfiledTagEnum;

public class MultiEventHandler extends BaseSingleTagEventHandler {

    @Children final BaseEventHandlerNode[] handlers;
    @CompilationFinal boolean noChildHandlerUpdate = true;

    /**
     *
     * @param handlers should be of the same kind T
     */
    protected MultiEventHandler(ProfiledTagEnum tag, BaseEventHandlerNode[] handlers) {
        super(handlers[0].context, tag);
        this.handlers = handlers.clone();

        // sort handlers by their priority
        Arrays.sort(this.handlers, Comparator.comparingInt(ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode::getPriority));
    }

    public static MultiEventHandler create(ProfiledTagEnum tag, BaseEventHandlerNode[] handlers) {
        assert (handlers != null && handlers.length > 0);
        return new MultiEventHandler(tag, handlers);
    }

    @Override
    @ExplodeLoop
    public void executePre(VirtualFrame frame, Object[] inputs) throws Exception {
        for (BaseEventHandlerNode handler : handlers) {
            handler.executePre(frame, inputs);
            noChildHandlerUpdate = noChildHandlerUpdate && (handler.wantsToUpdateHandler() == handler);
        }
    }

    @Override
    public BaseEventHandlerNode wantsToUpdateHandler() {
        if (noChildHandlerUpdate) {
            return this;
        }
        return wantsToUpdateHandlerSlow();
    }

    // TODO: could optimize into single loop and annotate with ExplodeLoop?
    @TruffleBoundary
    private BaseEventHandlerNode wantsToUpdateHandlerSlow() {

        ArrayList<BaseEventHandlerNode> newHandlers = new ArrayList<>(Arrays.asList(handlers));

        // 1st iteration: remove deactivated handlers
        boolean modified = newHandlers.removeIf((BaseEventHandlerNode h) -> h.wantsToUpdateHandler() == null);

        // 2nd iteration: replace updated handlers
        ListIterator<BaseEventHandlerNode> iter = newHandlers.listIterator();
        while (iter.hasNext()) {
            BaseEventHandlerNode cur = iter.next();
            BaseEventHandlerNode replacement = cur.wantsToUpdateHandler();
            if (cur != replacement) {
                iter.set(replacement);
                modified = true;
            }
        }

        assert modified : "noChildHandlerUpdate has lied to us";

        if (newHandlers.size() > 1) {
            // return new MultiEventHandler
            CompilerDirectives.transferToInterpreterAndInvalidate();
            return new MultiEventHandler(this.tag, newHandlers.toArray(new BaseEventHandlerNode[0]));
        } else if (newHandlers.size() == 1) {
            // optimize to SingleEventHandler
            return newHandlers.get(0);
        } else {
            // remove ourselves
            return null;
        }
    }

    @Override
    @ExplodeLoop
    public void executePost(VirtualFrame frame, Object result, Object[] inputs) throws Exception {
        for (BaseEventHandlerNode handler : handlers) {
            handler.executePost(frame, result, inputs);
            noChildHandlerUpdate = noChildHandlerUpdate && (handler.wantsToUpdateHandler() == handler);
        }
    }

    @ExplodeLoop
    @Override
    public void executeExceptional(VirtualFrame frame, Throwable exception) throws Exception {
        for (BaseEventHandlerNode handler : handlers) {
            handler.executeExceptional(frame, exception);
        }
    }

    @Override
    @ExplodeLoop
    public void executeExceptionalCtrlFlow(VirtualFrame frame, Throwable exception, Object[] inputs) throws Exception {
        for (BaseEventHandlerNode handler : handlers) {
            handler.executeExceptionalCtrlFlow(frame, exception, inputs);
        }
    }

    @ExplodeLoop
    @Override
    public Object onUnwind(VirtualFrame frame, Object info) {
        Object res = null;
        for (BaseEventHandlerNode handler : handlers) {
            res = handler.onUnwind(frame, info);
            if (res != null) {
                return res;
            }
        }
        return null;
    }
}
