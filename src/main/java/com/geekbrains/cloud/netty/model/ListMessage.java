package com.geekbrains.cloud.netty.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;

@Data
public class ListMessage implements CloudMessage {

    private final List<String> files;

    public ListMessage(Path path) throws IOException {
        files = Files.list(path)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.LIST;
    }
}
