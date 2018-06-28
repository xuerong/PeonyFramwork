package test;

import com.peony.engine.framework.control.annotation.Service;
import com.peony.engine.framework.server.Server;

@Service
public class TestService {
    int a = Server.getServerId();

}
