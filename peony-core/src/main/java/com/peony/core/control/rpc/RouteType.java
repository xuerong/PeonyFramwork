package com.peony.core.control.rpc;


import com.peony.core.server.IdService;
import com.peony.core.control.BeanHelper;

public enum RouteType implements IRoute {
    UID() {
        @Override
        public int getServerId(Object para) {
            IdService idService = BeanHelper.getServiceBean(IdService.class);
            return idService.getServerIdById(Long.parseLong((String)para));
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
