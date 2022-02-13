package com.geekbrains.cloud.client;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class MainController implements Initializable {

    private static final int BUFFER_SIZE = 8192;

    public TextField clientPath;
    public TextField serverPath;
    public ChoiceBox<String> disks;
    public ListView<String> clientView;
    public ListView<String> serverView;
    private File currentDirectory;
    private File serverDirectory;

    private DataInputStream is;
    private DataOutputStream os;
    private byte[] buf;

    // Platform.runLater(() -> {})
    private void updateClientView() {
        Platform.runLater(() -> {
//            File[] paths;
//            paths = File.listRoots();
//            for (File path : paths) {
//                disks.getItems().add(String.valueOf(path));
//            }
            clientPath.setText(currentDirectory.getAbsolutePath());
            clientView.getItems().clear();
            clientView.getItems().add("...");
            clientView.getItems()
                    .addAll(currentDirectory.list());
        });
    }

    private void updateServerView() {
        Platform.runLater(() -> {
            serverPath.setText(serverDirectory.getPath());
            serverView.getItems().clear();
            try {
                os.writeUTF("#update_view#");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                String[] split = is.readUTF().split("\\|");
                for (String s : split) {
                    serverView.getItems().add(s);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void download(ActionEvent actionEvent) throws IOException {
        String item = serverView.getSelectionModel().getSelectedItem();
        File selected = serverDirectory.toPath().resolve(item).toFile();
        if (selected.isFile()) {
            os.writeUTF("#file_request#");
            os.writeUTF(item);
            long size = is.readLong();
            File newFile = currentDirectory.toPath()
                    .resolve(item)
                    .toFile();
            try (OutputStream fos = new FileOutputStream(newFile)) {
                for (int i = 0; i < (size + BUFFER_SIZE - 1) / BUFFER_SIZE; i++) {
                    int readCount = is.read(buf);
                    fos.write(buf, 0, readCount);
                }
            }
            System.out.println("File: " + item + " is download");
            updateClientView();
        }
    }

    // upload file to server
    public void upload(ActionEvent actionEvent) throws IOException {
        String item = clientView.getSelectionModel().getSelectedItem();
        File selected = currentDirectory.toPath().resolve(item).toFile();
        if (selected.isFile()) {
            os.writeUTF("#file_message#");
            os.writeUTF(selected.getName());
            os.writeLong(selected.length());
            try (InputStream fis = new FileInputStream(selected)) {
                while (fis.available() > 0) {
                    int readBytes = fis.read(buf);
                    os.write(buf, 0, readBytes);
                }
            }
            os.flush();
            updateServerView();
        }
    }

    private void initNetwork() {
        try {
            buf = new byte[BUFFER_SIZE];
            Socket socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentDirectory = new File(System.getProperty("user.home"));


        // run in FX Thread
        // :: - method reference
        updateClientView();
        initNetwork();
        try {
            serverDirectory = new File(is.readUTF());
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateServerView();
        clientView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String item = clientView.getSelectionModel().getSelectedItem();
                if (item.equals("...")) {
                    currentDirectory = currentDirectory.getParentFile();
                    updateClientView();
                } else {
                    File selected = currentDirectory.toPath().resolve(item).toFile();
                    if (selected.isDirectory()) {
                        currentDirectory = selected;
                        updateClientView();
                    }
                }
            }
        });
    }
}
