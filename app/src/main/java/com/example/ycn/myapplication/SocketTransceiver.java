package com.example.ycn.myapplication;

/**
 * Created by ycn on 2016/12/24.
 */

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by asus on 2016/3/23.
 */
public abstract class SocketTransceiver implements Runnable {

    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean runFlag = false;

    public void start(Socket socket) {
        this.socket = socket;
        runFlag = true;
        new Thread(this).start();
    }

    public void stop() {
        runFlag = false;
        try {
            socket.shutdownInput();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            runFlag = false;
            e.printStackTrace();
        }
        while (runFlag) {
            //线程死循环  不停的从服务器端读回数据,并通过onReceiver()方法回传显示到UI
            dataInputStream = new DataInputStream(inputStream);
             try {
                 int s=dataInputStream.read();
                 Log.e("3",String.valueOf(s));
                    if (s==-1 | s==72 )
                    {
                        runFlag=false;
                    }else{
                        this.onReceive(String.valueOf(s));
                    }
            } catch (IOException e) {
                runFlag =  true;
                //e.printStackTrace();
            }

        }

        //断开连接
        try {
            inputStream.close();
            outputStream.close();
            socket.close();
            Log.e("5","not");
            dataInputStream.close();
            if (dataOutputStream != null)
                dataOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();

        }

        this.onConnectBreak();//连接被动断开
    }

    public void sendMSG(String s) {
        if (outputStream != null) {
            dataOutputStream = new DataOutputStream(outputStream);
            try {
                 dataOutputStream.write(s.getBytes());
                dataOutputStream.flush();
                this.onSendSuccess(s);
            } catch (IOException e) {
                onConnectBreak();
                runFlag = false;
                e.printStackTrace();
            }
        }
    }

    public abstract void onReceive(String s);

    public abstract void onConnectBreak();

    public abstract void onSendSuccess(String s);

}