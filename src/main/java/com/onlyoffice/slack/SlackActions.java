package com.onlyoffice.slack;

public enum SlackActions implements SlackOperations {
    GENERIC_ACTION("generic_button"),
    ONLYOFFICE_FILE_PERMISSIONS("onlyoffice_file_permissions"),
    OPEN_ONLYOFFICE_FILE("open_onlyoffice_files"),
    CLOSE_ONLYOFFICE_FILE_MODAL("close_onlyoffice_files"),
    OPEN_SETTINGS("open_onlyoffice_settings");
    private String actionID;
    SlackActions(String actionID) {
        this.actionID = actionID;
    }
    public String getEntrypoint() {
        return actionID;
    }
}
