package com.geekbrains.cloud.server;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;

public class CloudFileHandler implements Runnable {

    private static final int BUFFER_SIZE = 8192;
    private final DataInputStream is;
    private final DataOutputStream os;
    private final byte[] buf;
    private File serverDirectory;

    public CloudFileHandler(Socket socket) throws IOException {
        System.out.println("Client connected!");
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        buf = new byte[BUFFER_SIZE];
        serverDirectory = new File("server");
        os.writeUTF(serverDirectory.getName());
    }

    @Override
    public void run() {
        try {
            while (true) {
                String command = is.readUTF();
                if ("#file_message#".equals(command)) {
                    String name = is.readUTF();
                    long size = is.readLong();
                    File newFile = serverDirectory.toPath()
                            .resolve(name)
                            .toFile();
                    try (OutputStream fos = new FileOutputStream(newFile)) {
                        for (int i = 0; i < (size + BUFFER_SIZE - 1) / BUFFER_SIZE; i++) {
                            int readCount = is.read(buf);
                            fos.write(buf, 0, readCount);
                        }
                    }
                    System.out.println("File: " + name + " is uploaded");
                } else if ("#file_request#".equals(command)) {
                    String name = is.readUTF();
                    File selected = serverDirectory.toPath().resolve(name).toFile();
                    os.writeLong(selected.length());
                    try (InputStream fis = new FileInputStream(selected)) {
                        while (fis.available() > 0) {
                            int readBytes = fis.read(buf);
                            os.write(buf, 0, readBytes);
                        }
                    }
                    os.flush();
                } else if ("#update_view#".equals(command)) {
                    String[] listString = serverDirectory.list();
                    StringBuilder list = new StringBuilder();
                    for (String s : listString) {
                        list.append(s + "|");
                    }
                    String s = list.toString();
                    System.out.println(s);
                    os.writeUTF(s);
                } else {
                    System.err.println("Unknown command: " + command);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
