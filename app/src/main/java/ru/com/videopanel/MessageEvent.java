package ru.com.videopanel;

public class MessageEvent {
    private String command;

    public MessageEvent(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}