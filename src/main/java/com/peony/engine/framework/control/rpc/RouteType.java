package com.peony.engine.framework.control.rpc;

import com.peony.engine.framework.control.rpc.IRoute;

public enum RouteType implements IRoute {
    UID(String.class) {
        @Override
        public int getServerId(Object para) {
            //
            return Math.abs(para.hashCode()%10);
        }
    },

    SERVERID(int.class) {
        @Override
        public int getServerId(Object para) {
            return Integer.parseInt(para.toString());
        }
    },
    TEST(int.class) {
        @Override
        public int getServerId(Object para) {
            return 3;
        }
    },
    ;

    Class<?> firstArgType;

    public Class<?> getFirstArgType() {
        return firstArgType;
    }

    RouteType(Class<?> firstArgType) {
        this.firstArgType = firstArgType;
    }
}
