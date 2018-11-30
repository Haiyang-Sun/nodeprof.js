package ch.usi.inf.nodeprof.jalangi;

import java.util.WeakHashMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class JSHashMap implements TruffleObject {
    JSHashMap() {
    }

    private WeakHashMap<Object, Object> map = new WeakHashMap<>();

    @TruffleBoundary
    Object _get(Object key) {
        return map.get(key);
    }

    @TruffleBoundary
    public boolean _containsKey(Object key) {
        return map.containsKey(key);
    }

    @TruffleBoundary
    public Object _put(Object key, Object value) {
        Object result = map.put(key, value);
        if (result == null) {
            result = Undefined.instance;
        }
        return result;
    }

    public ForeignAccess getForeignAccess() {
        return JSHashMapMessageResolutionForeign.ACCESS;
    }

    @TruffleBoundary
    public void _clear() {
        map.clear();
    }

    @TruffleBoundary
    public Object _remove(Object key) {
        return map.remove(key);
    }

    @TruffleBoundary
    public int _size() {
        return map.size();
    }
}
