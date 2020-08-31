package com.peony.core.server;

/**
 * Created by a on 2016/8/30.
 */
public final class ServerType {
    private static int serverType = 2;

    public static final int MAIN_SERVER = 1;
    public static final int NODE_SERVER = 2;
    public static final int ASYNC_SERVER = 4;

    public static final String MAIN_SERVER_STR = "mainserver";
    public static final String NODE_SERVER_STR = "nodeserver";
    public static final String ASYNC_SERVER_STR = "asyncserver";

    public static void setServerType(String serverTypeStr){
        if(serverTypeStr == null){
            serverType = MAIN_SERVER|NODE_SERVER|ASYNC_SERVER;
        }else{
            String[] tStrs  = serverTypeStr.split("\\|");
            int type = 0;
            for (String str: tStrs) {
                if(str.equalsIgnoreCase(MAIN_SERVER_STR)){
                    type|=MAIN_SERVER;
                }
                if(str.equalsIgnoreCase(NODE_SERVER_STR)){
                    type|=NODE_SERVER;
                }
                if(str.equalsIgnoreCase(ASYNC_SERVER_STR)){
                    type|=ASYNC_SERVER;
                }
            }
            if(type!=0){
                serverType = type;
            }
        }
    }
    public static String getServerTypeName(){
        return getServerTypeName(serverType);
    }

    public static String getServerTypeName(int serverType){
        StringBuilder result=new StringBuilder();
        if((serverType&MAIN_SERVER)>0){
            result.append(MAIN_SERVER_STR+"|");
        }
        if((serverType&NODE_SERVER)>0){
            result.append(NODE_SERVER_STR+"|");
        }
        if((serverType&ASYNC_SERVER)>0){
            result.append(ASYNC_SERVER_STR+"|");
        }
        if(result.length()>0){
            return result.substring(0,result.length()-1);
        }
        return "";
    }

    public static int getServerType() {
        return serverType;
    }

    public static boolean isMainServer(){
        return (serverType&MAIN_SERVER)>0;
    }
    public static boolean isNodeServer(){
        return (serverType&NODE_SERVER)>0;
    }
    public static boolean isAsyncServer(){
        return (serverType&ASYNC_SERVER)>0;
    }
    public static boolean isMainServer(int serverType){
        return (serverType&MAIN_SERVER)>0;
    }
    public static boolean isNodeServer(int serverType){
        return (serverType&NODE_SERVER)>0;
    }
    public static boolean isAsyncServer(int serverType){
        return (serverType&ASYNC_SERVER)>0;
    }
}
