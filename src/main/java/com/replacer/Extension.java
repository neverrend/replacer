package com.replacer;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

public class Extension implements BurpExtension {

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("Replacer");
        ReplacerTab replacerTab = new ReplacerTab(api);
        api.userInterface().registerContextMenuItemsProvider(
                new ReplacerContextMenu(api, replacerTab)
        );
        api.logging().logToOutput("Replacer extension loaded successfully.");
    }
}
