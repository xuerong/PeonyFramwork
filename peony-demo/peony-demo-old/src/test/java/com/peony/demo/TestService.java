package com.peony.demo;

import com.peony.core.control.annotation.Service;
import com.peony.core.server.Server;

@Service
public class TestService {
    int a = Server.getServerId();

}
