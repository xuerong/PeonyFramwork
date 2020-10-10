package com.peony.cluster;

import com.peony.cluster.servicerole.ServiceRole;

/**
 * @author z84150192
 * @since 2020/10/9
 */
public class ClusterBeanInfo {
    private Class<?> cls;
    private Object clusterBean;
    private ServiceRole serviceRole;

    public ClusterBeanInfo(Class<?> cls, Object clusterBean, ServiceRole serviceRole) {
        this.cls = cls;
        this.clusterBean = clusterBean;
        this.serviceRole = serviceRole;
    }

    public Class<?> getCls() {
        return cls;
    }

    public void setCls(Class<?> cls) {
        this.cls = cls;
    }

    public Object getClusterBean() {
        return clusterBean;
    }

    public void setClusterBean(Object clusterBean) {
        this.clusterBean = clusterBean;
    }

    public ServiceRole getServiceRole() {
        return serviceRole;
    }

    public void setServiceRole(ServiceRole serviceRole) {
        this.serviceRole = serviceRole;
    }
}
