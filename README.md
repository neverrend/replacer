# Replacer - Burp Suite Extension

A Burp Suite extension for defining reusable match-and-replace rules that can be applied to HTTP requests via the right-click context menu.

## Features

- **Rule-based replacements** — Create named rules, each containing one or more typed match/replace rows
- **Copy from requests** — Right-click a request in Proxy, Logger, or Target to extract live values into a rule's Replace fields
- **Apply to Repeater** — Right-click in Repeater to apply a rule's replacements to the current request in-place
- **Duplicate rules** — Clone an existing rule with all its type rows using "+ make a copy"

## Supported Types

| Type | Match field | Replace field |
|------|------------|---------------|
| **Header** | Header name (e.g. `Authorization`) | Header value |
| **Cookie** | Cookie name (e.g. `session_id`) | Cookie value |
| **URL Parameter** | Query parameter name | Parameter value |
| **POST Body Parameter** | Body parameter name | Parameter value (supports form-encoded and multipart) |

## Installation

### Build from source

```bash
./gradlew build
```

The extension JAR will be at `build/libs/burp-extension-1.0.0.jar`.

### Load in Burp Suite

1. Open Burp Suite
2. Go to **Extensions** > **Installed**
3. Click **Add**
4. Set Extension type to **Java**
5. Select the built JAR file
6. Click **Next**

## Usage

### Creating rules

1. Go to the **Replacer** tab in Burp Suite
2. Click **+** to add a new rule panel
3. Enter a **Name** for the rule (e.g. "Auth")
4. Each rule starts with one type row — set the **Type**, **Match**, and **Replace** fields
5. Click **+ add new type** to add more type rows to the same rule
6. Click **+ make a copy** to duplicate the entire rule

### Copying values from a request (Proxy / Logger / Target)

1. Right-click on a request in Proxy history, Logger, or Target — either on the table row or inside the request viewer pane
2. Select **Copy to {rule name}**
3. The extension extracts the current value for each type row's Match field from the request and populates the Replace field

For example, if you have a rule "Auth" with a Header type row matching `Authorization`, the extension will find the `Authorization` header in the selected request and fill in its value.

### Applying replacements (Repeater)

1. Right-click inside a request in Repeater
2. Select **Use {rule name}**
3. The extension applies all of the rule's match/replace substitutions to the request in-place

If a matched parameter doesn't exist in the request, it will be added automatically (new header, new query param, new body param, or new multipart form field).

## Requirements

- Burp Suite Professional or Community with Montoya API support
- Java 17+
