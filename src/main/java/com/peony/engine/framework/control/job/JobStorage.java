package com.peony.engine.framework.control.job;

import java.util.List;

/**
 * Created by a on 2016/9/5.
 * job的存储接口，具体实现在配置文件中配置
 */
public interface JobStorage {
    public List<Job> getJobList();
    public void saveJob(Job job);
    public void deleteJob(String id);
}
