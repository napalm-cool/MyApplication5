package com.example.ycn.myapplication;

/**
 * Created by ycn on 2016/12/24.
 */

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;

/**
 * Created by asus on 2016/3/23.
 */
public abstract class TcpClient implements Runnable{

    private String hostname;
    boolean connect = false;
    private SocketTransceiver transceiver;
    private Socket socket;
    public void connect(String host){
    this.hostname=host;
    new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            Log.e("step","2");
            transceiver = new SocketTransceiver() {
                @Override
                public void onReceive(String s) {
                    TcpClient.this.onReceive(s);
                }

                @Override
                public void onConnectBreak() {
                    TcpClient.this.onConnectBreak();
                }

                @Override
                public void onSendSuccess(String s) {
                    TcpClient.this.onSendSuccess(s);
                }


            };
            //域名转换为地址
            URL u = new URL(hostname);
            Log.v  ("hostname=" , u.getHost());
           InetAddress ia = InetAddress.getByName(u.getHost());
            Log.d("IP resolved =" , ia.getHostAddress());
            int port = u.getPort();
            if (port < 0) {
                port = u.getDefaultPort();
            }
            Log.d("port =" , String.valueOf(port));
            socket = new Socket(ia.getHostAddress(), port);
            connect = true;
            Log.d("connect", String.valueOf(connect));
            this.onConnect();
            transceiver.start(socket);
        } catch (IOException e) {
            this.onConnectFalied();
            e.printStackTrace();
        }
    }

    protected  boolean isConnected(){
        return connect;
    }

    public void disConnected(){
        connect = false;
        if(transceiver!=null){
            transceiver.stop();
            transceiver=null;
        }
    }

    public SocketTransceiver getTransceiver(){
        return transceiver;
    }

    public abstract void onConnect();

    public abstract void onConnectBreak();

    public abstract void onReceive(String s);

    public abstract void onConnectFalied();

    public abstract void  onSendSuccess(String s);

}
