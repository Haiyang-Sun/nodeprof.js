package ch.usi.inf.nodeprof.handlers;

import com.oracle.truffle.api.instrumentation.EventContext;

import ch.usi.inf.nodeprof.ProfiledTagEnum;

public class BaseSingleTagEventHandler extends BaseEventHandlerNode {

    public BaseSingleTagEventHandler(EventContext context, ProfiledTagEnum tag) {
        super(context);
        this.tag = tag;
    }

    protected final ProfiledTagEnum tag;

    @Override
    public int expectedNumInputs() {
        return tag.getExpectedNumInputs();
    }

}
