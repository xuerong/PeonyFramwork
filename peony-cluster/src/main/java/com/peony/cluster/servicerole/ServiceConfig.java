package com.peony.cluster.servicerole;

import java.util.Set;

/**
 * @Author: zhengyuzhen
 * @Date: 2020-10-04 22:19
 */
public class ServiceConfig {
    private int id;
    private Class<?> service;
    private Addresses providers;
    private Addresses consumers;
    private Addresses runSelves;

    class Addresses {
        private boolean include; // true:include,false:exclude
        private Set<String> addresses;

        public boolean contains(String ip) {
            if (addresses == null) {
                return false;
            }
            return include == addresses.contains(ip);
        }

        public boolean isInclude() {
            return include;
        }

        public void setInclude(boolean include) {
            this.include = include;
        }

        public Set<String> getAddresses() {
            return addresses;
        }

        public void setAddresses(Set<String> addresses) {
            this.addresses = addresses;
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Class<?> getService() {
        return service;
    }

    public void setService(Class<?> service) {
        this.service = service;
    }

    public Addresses getProviders() {
        return providers;
    }

    public void setProviders(Addresses providers) {
        this.providers = providers;
    }

    public Addresses getConsumers() {
        return consumers;
    }

    public void setConsumers(Addresses consumers) {
        this.consumers = consumers;
    }

    public Addresses getRunSelves() {
        return runSelves;
    }

    public void setRunSelves(Addresses runSelves) {
        this.runSelves = runSelves;
    }
}
