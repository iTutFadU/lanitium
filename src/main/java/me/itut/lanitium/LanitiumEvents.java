package me.itut.lanitium;

import carpet.script.CarpetEventServer;

public abstract class LanitiumEvents extends CarpetEventServer.Event {
//    public static final LanitiumEvents CLIENT_CONFIGURATION = new LanitiumEvents("client_configuration", 2, false) {
//
//    };

    public LanitiumEvents(String name, int reqArgs, boolean isGlobalOnly) {
        super(name, reqArgs, isGlobalOnly);
    }

    public void onPacketEvent() {}
}
