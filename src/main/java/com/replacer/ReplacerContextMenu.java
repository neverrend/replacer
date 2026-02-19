package com.replacer;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.ContentType;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ReplacerContextMenu implements ContextMenuItemsProvider {

    private final MontoyaApi api;
    private final ReplacerTab replacerTab;

    public ReplacerContextMenu(MontoyaApi api, ReplacerTab replacerTab) {
        this.api = api;
        this.replacerTab = replacerTab;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuItems = new ArrayList<>();

        boolean isCopyContext = event.isFromTool(ToolType.PROXY, ToolType.LOGGER, ToolType.TARGET);
        boolean isRepeater = event.isFromTool(ToolType.REPEATER);

        if (!isCopyContext && !isRepeater) {
            return menuItems;
        }

        // Resolve the request: prefer selected rows, fall back to the message viewer pane
        HttpRequest copyRequest = null;
        if (isCopyContext) {
            List<HttpRequestResponse> selected = event.selectedRequestResponses();
            if (!selected.isEmpty()) {
                copyRequest = selected.get(0).request();
            } else if (event.messageEditorRequestResponse().isPresent()) {
                copyRequest = event.messageEditorRequestResponse().get().requestResponse().request();
            }
        }

        for (ReplacerTab.RulePanel rule : replacerTab.getRulePanels()) {
            String name = rule.getName();
            if (name == null || name.isBlank()) {
                continue;
            }

            if (isCopyContext && copyRequest != null) {
                HttpRequest req = copyRequest;
                JMenuItem item = new JMenuItem("Copy to " + name);
                item.addActionListener(e -> copyToRule(req, rule));
                menuItems.add(item);
            }

            if (isRepeater) {
                event.messageEditorRequestResponse().ifPresent(editor -> {
                    JMenuItem item = new JMenuItem("Use " + name);
                    item.addActionListener(e -> useRule(editor, rule));
                    menuItems.add(item);
                });
            }
        }

        return menuItems;
    }

    private void copyToRule(HttpRequest request, ReplacerTab.RulePanel rule) {
        for (ReplacerTab.TypeRow typeRow : rule.getTypeRows()) {
            String type = typeRow.getType();
            String match = typeRow.getMatch();
            if (match == null || match.isBlank()) {
                continue;
            }

            String extracted = extractValue(request, type, match);
            if (extracted != null) {
                typeRow.setReplace(extracted);
            } else {
                api.logging().logToError("No match found for " + type + ": " + match);
            }
        }
    }

    private void useRule(MessageEditorHttpRequestResponse editor, ReplacerTab.RulePanel rule) {
        HttpRequest request = editor.requestResponse().request();

        for (ReplacerTab.TypeRow typeRow : rule.getTypeRows()) {
            String type = typeRow.getType();
            String match = typeRow.getMatch();
            String replace = typeRow.getReplace();
            if (match == null || match.isBlank()) {
                continue;
            }

            request = applyReplacement(request, type, match, replace);
        }

        editor.setRequest(request);
    }

    private String extractValue(HttpRequest request, String type, String match) {
        switch (type) {
            case "Header":
                return request.headerValue(match);
            case "Cookie":
                String cookieHeader = request.headerValue("Cookie");
                if (cookieHeader != null) {
                    return parseCookieValue(cookieHeader, match);
                }
                return null;
            case "URL Parameter":
                for (HttpParameter param : request.parameters()) {
                    if (param.type() == HttpParameterType.URL && param.name().equals(match)) {
                        return param.value();
                    }
                }
                return null;
            case "POST Body Parameter":
                for (HttpParameter param : request.parameters()) {
                    if (param.type() == HttpParameterType.BODY && param.name().equals(match)) {
                        return param.value();
                    }
                }
                return null;
            default:
                return null;
        }
    }

    private HttpRequest applyReplacement(HttpRequest request, String type, String match, String replace) {
        switch (type) {
            case "Header":
                String safeHeaderValue = sanitizeHeaderValue(replace);
                if (request.hasHeader(match)) {
                    return request.withUpdatedHeader(match, safeHeaderValue);
                }
                return request.withAddedHeader(match, safeHeaderValue);
            case "Cookie":
                String safeCookieValue = sanitizeHeaderValue(replace);
                String cookieHeader = request.headerValue("Cookie");
                if (cookieHeader != null) {
                    String updatedCookie = replaceCookieValue(cookieHeader, match, safeCookieValue);
                    return request.withUpdatedHeader("Cookie", updatedCookie);
                }
                return request.withAddedHeader("Cookie", match + "=" + safeCookieValue);
            case "URL Parameter":
                if (request.hasParameter(match, HttpParameterType.URL)) {
                    return request.withUpdatedParameters(HttpParameter.urlParameter(match, replace));
                }
                return request.withAddedParameters(HttpParameter.urlParameter(match, replace));
            case "POST Body Parameter":
                if (request.contentType() == ContentType.MULTIPART) {
                    return applyMultipartReplacement(request, match, replace);
                }
                if (request.hasParameter(match, HttpParameterType.BODY)) {
                    return request.withUpdatedParameters(HttpParameter.bodyParameter(match, replace));
                }
                return request.withAddedParameters(HttpParameter.bodyParameter(match, replace));
            default:
                return request;
        }
    }

    private HttpRequest applyMultipartReplacement(HttpRequest request, String name, String value) {
        name = sanitizeFieldName(name);
        String contentType = request.headerValue("Content-Type");
        if (contentType == null) {
            return request;
        }

        // Extract boundary from Content-Type header
        String boundary = null;
        for (String part : contentType.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("boundary=")) {
                boundary = trimmed.substring("boundary=".length()).trim();
                // Strip quotes if present
                if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
                    boundary = boundary.substring(1, boundary.length() - 1);
                }
                break;
            }
        }
        if (boundary == null) {
            return request;
        }

        String body = request.bodyToString();
        String partDelimiter = "--" + boundary;
        String closingDelimiter = partDelimiter + "--";

        // Multipart part looks like:
        // --boundary\r\nContent-Disposition: form-data; name="fieldName"\r\n\r\nvalue\r\n
        String partPattern = partDelimiter + "\r\nContent-Disposition: form-data; name=\"" + name + "\"";
        int partStart = body.indexOf(partPattern);

        if (partStart >= 0) {
            // Update existing part: find the blank line after headers, then replace value up to next boundary
            int headersEnd = body.indexOf("\r\n\r\n", partStart);
            if (headersEnd < 0) {
                return request;
            }
            int valueStart = headersEnd + 4; // skip \r\n\r\n
            int nextBoundary = body.indexOf("\r\n" + partDelimiter, valueStart);
            if (nextBoundary < 0) {
                return request;
            }
            String newBody = body.substring(0, valueStart) + value + body.substring(nextBoundary);
            return request.withBody(newBody);
        } else {
            // Add new part before the closing delimiter
            String newPart = partDelimiter + "\r\n"
                    + "Content-Disposition: form-data; name=\"" + name + "\"\r\n"
                    + "\r\n"
                    + value + "\r\n";
            String newBody = body.replace(closingDelimiter, newPart + closingDelimiter);
            return request.withBody(newBody);
        }
    }

    private static String sanitizeHeaderValue(String value) {
        if (value == null) return null;
        return value.replace("\r", "").replace("\n", "");
    }

    private static String sanitizeFieldName(String name) {
        if (name == null) return null;
        return name.replace("\"", "").replace("\r", "").replace("\n", "");
    }

    private String parseCookieValue(String cookieHeader, String name) {
        for (String part : cookieHeader.split(";")) {
            String trimmed = part.trim();
            int eq = trimmed.indexOf('=');
            if (eq > 0 && trimmed.substring(0, eq).trim().equals(name)) {
                return trimmed.substring(eq + 1).trim();
            }
        }
        return null;
    }

    private String replaceCookieValue(String cookieHeader, String name, String newValue) {
        StringBuilder result = new StringBuilder();
        boolean found = false;
        for (String part : cookieHeader.split(";")) {
            String trimmed = part.trim();
            if (result.length() > 0) {
                result.append("; ");
            }
            int eq = trimmed.indexOf('=');
            if (eq > 0 && trimmed.substring(0, eq).trim().equals(name)) {
                result.append(name).append("=").append(newValue);
                found = true;
            } else {
                result.append(trimmed);
            }
        }
        if (!found) {
            if (result.length() > 0) {
                result.append("; ");
            }
            result.append(name).append("=").append(newValue);
        }
        return result.toString();
    }
}
