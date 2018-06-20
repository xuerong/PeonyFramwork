package com.peony.engine.framework.data.sysPara;

import com.peony.engine.framework.data.DataService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by a on 2016/9/28.
 */
public class DefaultSysParaStorage implements SysParaStorage {
    private DataService dataService;
    @Override
    public Map<String, String> getAllSysPara() {
        List<SysPara> sysParaList = dataService.selectList(SysPara.class,"");
        if(sysParaList!=null && sysParaList.size()>0) {
            Map<String, String> result = new HashMap<>();
            for (SysPara sysPara : sysParaList) {
                result.put(sysPara.getId(), sysPara.getValue());
            }
            return result;
        }
        return null;
    }

    @Override
    public void insertSysPara(String key, String value) {
        SysPara sysPara = new SysPara();
        sysPara.setId(key);
        sysPara.setValue(value);
        dataService.insert(sysPara);
    }

    @Override
    public void update(String key, String value) {
        SysPara sysPara = dataService.selectObject(SysPara.class,"id=?",key);
        if(sysPara == null){
            insertSysPara(key,value);
        }else{
            if(value != sysPara.getValue()) {
                dataService.update(sysPara);
            }
        }
    }

    @Override
    public void delete(String key) {
        SysPara sysPara = dataService.selectObject(SysPara.class,"id=?",key);
        if(sysPara != null){
            dataService.delete(sysPara);
        }
    }
}
