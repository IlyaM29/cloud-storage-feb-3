package com.geekbrains.cloud.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;

public class EchoServerNio {

    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private final ByteBuffer buf;
    private Path path = Paths.get("server");

    public EchoServerNio() throws Exception {
        buf = ByteBuffer.allocate(5);
        serverSocketChannel = ServerSocketChannel.open();
        selector = Selector.open();

        serverSocketChannel.bind(new InetSocketAddress(8189));
        serverSocketChannel.configureBlocking(false);

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (serverSocketChannel.isOpen()) {

            selector.select();

            Set<SelectionKey> keys = selector.selectedKeys();

            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey currentKey = iterator.next();
                if (currentKey.isAcceptable()) {
                    handleAccept();
                }
                if (currentKey.isReadable()) {
                    handleRead(currentKey);
                }
                iterator.remove();
            }
        }
    }

    private void handleRead(SelectionKey currentKey) throws IOException {

        SocketChannel channel = (SocketChannel) currentKey.channel();

        StringBuilder reader = new StringBuilder();

        while (true) {
            int count = channel.read(buf);

            if (count == 0) {
                break;
            }

            if (count == -1) {
                channel.close();
                return;
            }

            buf.flip();

            while (buf.hasRemaining()) {
                reader.append((char) buf.get());
            }

            buf.clear();
        }

        String msg = reader.toString();
        System.out.println("Received: " + msg);

        if (msg.startsWith("--")) {
            command(channel, msg);
        } else {
            String msgServer = "From server: " + msg;
            channel.write(ByteBuffer.wrap(msgServer.getBytes(StandardCharsets.UTF_8)));
        }
        channel.write(ByteBuffer.wrap("-> ".getBytes(StandardCharsets.UTF_8)));
    }

    private void command(SocketChannel channel, String msg) throws IOException {
        if (msg.startsWith("--help")) {
            String msgServer = "1. --ls - выводит список файлов на экран\r\n" +
                    "2. --cd path - перемещается из текущей папки в папку из аргумента\r\n" +
                    "3. --cat file - печатает содержание текстового файла на экран\r\n" +
                    "4. --mkdir dir - создает папку в текущей директории\r\n" +
                    "5. --touch file - создает пустой файл в текущей директории\r\n";
            channel.write(ByteBuffer.wrap(msgServer.getBytes(StandardCharsets.UTF_8)));
        }
        if (msg.startsWith("--ls")) {
            String[] files = path.toFile().list();
            if (files != null) {
                for (String file : files) {
                    channel.write(ByteBuffer.wrap((file + "\r\n").getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
        if (msg.startsWith("--cd")) {
            String[] split = msg.split(" ");
            String dir = split[1].substring(0, split[1].length() - 2);
            if (Files.exists(path.resolve(dir))) {
                path = path.resolve(dir);
                channel.write(ByteBuffer.wrap((path + "\r\n").getBytes(StandardCharsets.UTF_8)));
            }
        }
        if (msg.startsWith("--cat")) {
            String[] split = msg.split(" ");
            String file = split[1].substring(0, split[1].length() - 2);
            if (Files.exists(path.resolve(file))) {
                byte[] bytes = Files.readAllBytes(path.resolve(file));
                channel.write(ByteBuffer.wrap((new String(bytes) + "\r\n").getBytes(StandardCharsets.UTF_8)));
            }
        }
        if (msg.startsWith("--mkdir")) {
            String[] split = msg.split(" ");
            String dir = split[1].substring(0, split[1].length() - 2);
            if (Files.exists(path.resolve(dir))) {
                channel.write(ByteBuffer.wrap("Директория существует\r\n".getBytes(StandardCharsets.UTF_8)));
            } else {
                Files.createDirectories(path.resolve(dir));
                channel.write(ByteBuffer.wrap("Директория создана\r\n".getBytes(StandardCharsets.UTF_8)));
            }
        }
        if (msg.startsWith("--touch")) {
            String[] split = msg.split(" ");
            String dir = split[1].substring(0, split[1].length() - 2);
            if (Files.exists(path.resolve(dir))) {
                channel.write(ByteBuffer.wrap("Файл существует\r\n".getBytes(StandardCharsets.UTF_8)));
            } else {
                Files.createFile(path.resolve(dir));
                channel.write(ByteBuffer.wrap("Файл создан\r\n".getBytes(StandardCharsets.UTF_8)));
            }
        }
    }

    private void handleAccept() throws IOException {
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("Client accepted...");

        String welcome = "Welcome to Mike terminal\r\n";
        socketChannel.write(ByteBuffer.wrap(welcome.getBytes(StandardCharsets.UTF_8)));
    }

    public static void main(String[] args) throws Exception {
        new EchoServerNio();
    }

}
