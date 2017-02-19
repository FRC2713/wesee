package org.iraiders.wesee;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/*
 * I don't actually know how this should work.
 * This is a very rough implementation of an MJPEG-Over-HTTP server
 */
public class MjpegServer implements Runnable {
    private ServerSocket server;
    private List<Connection> connections = new ArrayList<>();
    private BlockingQueue<Connection> toClose = new LinkedBlockingQueue<>();

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
                System.out.println("Accepting new connection...");
                connections.add(connection);

                if (!toClose.isEmpty()) {
                    connections.remove(toClose.take());
                }

                new Thread(connection).start();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void pushFrame(byte[] frame) {
        for (Connection connection : connections) {
            connection.pushFrame(frame);
        }
    }

    private class Connection implements Runnable {
        private Socket socket;
        private BlockingQueue<byte[]> frameQueue;

        public Connection(Socket socket) {
            this.socket = socket;
            frameQueue = new LinkedBlockingQueue<>();
        }

        private void pushFrame(byte[] frame) {
            frameQueue.offer(frame);
        }

        @Override
        public void run() {
            try {
                System.out.println("Accepted new connection.");
                OutputStream out = socket.getOutputStream();
                out.write("HTTP/1.1 200 Success\r\n".getBytes());
                out.write("Cache-Control: no-store, no-cache\r\n".getBytes());
                out.write("Content-Type: multipart/x-mixed-replace;boundary=--frameboundary\r\n\r\n".getBytes());
                out.flush();

                while (true) {
                    byte[] frame = frameQueue.take();

                    out.write("--frameboundary\r\n".getBytes());
                    out.write("Content-Type: image/jpeg\r\n".getBytes());
                    out.write(("Content-Length: " + frame.length + "\r\n\r\n").getBytes());
                    out.write(frame);
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
                System.out.println("Closing connection...");
                toClose.offer(this);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
