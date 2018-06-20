package com.peony.engine.framework.control.job;

import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.security.exception.MMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by a on 2016/9/5.
 * 存储到文件里
 */
public class DefaultJobStorage implements JobStorage{
    private static final Logger log = LoggerFactory.getLogger(DefaultJobStorage.class);
    private DataService dataService;
    @Override
    public List<Job> getJobList() {
        List<JobDb> jobDbList = dataService.selectList(JobDb.class,"");
        if(jobDbList!=null && jobDbList.size()>0){
            List<Job> jobList = new ArrayList<>(jobDbList.size());
            for(JobDb jobDb : jobDbList){
                Job job = new Job();
                job.setId(jobDb.getId());
                job.setDb(jobDb.getDb()>0);
                job.setMethod(jobDb.getMethod());
                Class serviceClass;
                try {
                    serviceClass = Class.forName(jobDb.getServiceClass());
                }catch (Throwable e){
                    log.error("service class is not exist:"+jobDb.getServiceClass());
                    continue;
                }
                job.setServiceClass(serviceClass);
                job.setStartDate(jobDb.getStartDate());
                job.setPara(jobDb.getParams());
                jobList.add(job);
            }
            return jobList;
        }
        return null;
    }

    @Override
    public void saveJob(Job job) {
        // 校验job的参数
        if(job.getPara() != null && job.getPara().length>0){
            for(Object para : job.getPara()){
                if(!(para instanceof Serializable)){
                    throw new MMException("job para error,not implement Serializable");
                }
            }
        }
        JobDb jobDb = new JobDb();
        jobDb.setId(job.getId());
        jobDb.setMethod(job.getMethod());
        jobDb.setParams(job.getPara());
        jobDb.setDb(job.isDb()?1:0);
        jobDb.setServiceClass(job.getServiceClass().getName());
        jobDb.setStartDate(new Timestamp(job.getStartDate().getTime()));
        dataService.insert(jobDb);
    }

    @Override
    public void deleteJob(String id) {
        JobDb jobDb = dataService.selectObject(JobDb.class,"id=?",id);
        if(jobDb != null){
            dataService.delete(jobDb);
        }else{
            log.error("jobDb is not exist while delete job , job id = "+id);
        }
    }
}
