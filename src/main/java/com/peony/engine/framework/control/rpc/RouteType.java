package com.peony.engine.framework.control.rpc;


public enum RouteType implements IRoute {
    UID() {
        @Override
        public int getServerId(Object para) {
            //
            return Math.abs(para.hashCode()%10);
        }
    },

    SERVERID() {
        @Override
        public int getServerId(Object para) {
            return Integer.parseInt(para.toString());
        }
    },
    TEST() {
        @Override
        public int getServerId(Object para) {
            return 3;
        }
    },
    ;
}
