package ch.usi.inf.nodeprof.jalangi;

import com.oracle.truffle.api.interop.CanResolve;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

@MessageResolution(receiverType = JSHashMap.class)
public class JSHashMapMessageResolution {
    @CanResolve
    public abstract static class CanHandleTestMap extends Node {
        public boolean test(TruffleObject o) {
            return o instanceof JSHashMap;
        }
    }

    @Resolve(message = "INVOKE")
    abstract static class RunnableInvokeNode extends Node {
        public Object access(JSHashMap invoker, String identifier, Object[] arguments) {
            if (identifier.equals("containsKey")) {
                return invoker._containsKey(arguments[0]);
            } else if (identifier.equals("get")) {
                return invoker._get(arguments[0]);
            } else if (identifier.equals("size")) {
                return invoker._size();
            } else if (identifier.equals("remove")) {
                return invoker._remove(arguments[0]);
            } else if (identifier.equals("put")) {
                return invoker._put(arguments[0], arguments[1]);
            } else if (identifier.equals("clear")) {
                invoker._clear();
            }
            return 0;
        }
    }
}
