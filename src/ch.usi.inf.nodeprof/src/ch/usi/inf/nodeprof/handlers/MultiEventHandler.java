/*******************************************************************************
 * Copyright [2018] [Haiyang Sun, Università della Svizzera Italiana (USI)]
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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public class MultiEventHandler<T extends BaseEventHandlerNode> extends BaseEventHandlerNode {

    @Children final T[] handlers;

    /**
     *
     * @param handlers should be of the same kind T
     */
    protected MultiEventHandler(T[] handlers) {
        super(handlers[0].context);
        this.handlers = handlers;
    }

    public static <T extends BaseEventHandlerNode> MultiEventHandler<T> create(T[] handlers) {
        assert (handlers != null && handlers.length > 0);
        return new MultiEventHandler<>(handlers);
    }

    @Override
    @ExplodeLoop
    public void executePre(VirtualFrame frame, Object[] inputs) {
        for (T handler : handlers) {
            handler.executePre(frame, inputs);
        }
    }

    @Override
    @ExplodeLoop
    public void executePost(VirtualFrame frame, Object result, Object[] inputs) {
        for (T handler : handlers) {
            handler.executePost(frame, result, inputs);
        }
    }

    @Override
    public boolean isLastIndex(int inputCount, int index) {
        return handlers[0].isLastIndex(inputCount, index);
    }

    @ExplodeLoop
    @Override
    public void executeExceptional(VirtualFrame frame) {
        for (T handler : handlers) {
            handler.executeExceptional(frame);
        }
    }

    @ExplodeLoop
    @Override
    public Object onUnwind(VirtualFrame frame, Object info) {
        Object res = null;
        for (T handler : handlers) {
            res = handler.onUnwind(frame, info);
            if (res != null) {
                return res;
            }
        }
        return null;
    }
}
