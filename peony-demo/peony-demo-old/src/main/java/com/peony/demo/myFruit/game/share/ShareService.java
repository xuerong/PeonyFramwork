package com.peony.demo.myFruit.game.share;

import com.alibaba.fastjson.JSONObject;
import com.peony.demo.myFruit.cmd.Cmd;
import com.peony.core.control.annotation.Request;
import com.peony.core.control.annotation.Service;
import com.peony.core.data.entity.session.Session;

@Service
public class ShareService {
    @Request(opcode = Cmd.ShareOpen)
    public JSONObject getShareOpen(JSONObject req, Session session){
        JSONObject ret = new JSONObject();
        ret.put("shareOpen",1);
        return ret;
    }
}
