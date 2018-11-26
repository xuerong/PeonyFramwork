package com.myFruit.game.share;

import com.alibaba.fastjson.JSONObject;
import com.myFruit.cmd.Cmd;
import com.peony.engine.framework.control.annotation.Request;
import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.data.entity.session.Session;

@Service
public class ShareService {
    @Request(opcode = Cmd.ShareOpen)
    public JSONObject getShareOpen(JSONObject req, Session session){
        JSONObject ret = new JSONObject();
        ret.put("shareOpen",1);
        return ret;
    }
}
