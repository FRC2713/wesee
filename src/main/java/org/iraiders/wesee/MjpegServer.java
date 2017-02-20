package org.iraiders.wesee;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 * I don't actually know how this should work.
 * This is a very rough implementation of an MJPEG-Over-HTTP server
 *
 * This thing lags a lot. Never use it.
 */
public class MjpegServer implements Runnable {
    private ServerSocket server;
    private List<Connection> connections = new CopyOnWriteArrayList<>();
    private final FrameLock frame = new FrameLock();

    public MjpegServer(int port) {
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Connection connection = new Connection(server.accept());
                connections.add(connection);
                new Thread(connection).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void pushFrame(byte[] data) {
        synchronized (frame) {
            frame.setData(data);
            frame.notifyAll();
        }
    }

    private class Connection implements Runnable {
        private Socket socket;

        public Connection(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                OutputStream out = socket.getOutputStream();
                out.write("HTTP/1.1 200 Success\r\n".getBytes());
                out.write("Cache-Control: no-store, no-cache\r\n".getBytes());
                out.write("Content-Type: multipart/x-mixed-replace;boundary=--frameboundary\r\n\r\n".getBytes());
                out.flush();

                while (true) {
                    synchronized (frame) {
                        frame.wait();
                    }

                    out.write("--frameboundary\r\n".getBytes());
                    out.write("Content-Type: image/jpeg\r\n".getBytes());
                    out.write(("Content-Length: " + frame.getData().length + "\r\n\r\n").getBytes());
                    out.write(frame.getData());
                    out.write("\r\n\r\n".getBytes());
                    out.flush();
                }
            } catch (IOException e) {
                closeConnection();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void closeConnection() {
            try {
                connections.remove(this);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class FrameLock {
        private byte[] data;

        public void setData(byte[] data) {
            this.data = data;
        }

        public byte[] getData() {
            return data;
        }
    }
}
