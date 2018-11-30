package ch.usi.inf.nodeprof.utils;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;

public class PropertyExistsNode extends Node {
    private final String propertyName;

    public PropertyExistsNode(String propertyName) {
        this.propertyName = propertyName;
    }

    @Child Node readNode = Message.READ.createNode();

    public boolean keyExists(TruffleObject obj) {
        if (obj == null)
            return false;
        try {
            // to be checked
            Object ret = ForeignAccess.sendRead(readNode, obj, propertyName);
            return ret instanceof Boolean && (Boolean) ret;
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            return false;
        }
    }

    public boolean executePropertyExists(TruffleObject obj) {
        return keyExists(obj);
    }

    public static PropertyExistsNode create(String propertyName) {
        return new PropertyExistsNode(propertyName);
    }
}