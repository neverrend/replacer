package com.replacer;

import burp.api.montoya.MontoyaApi;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ReplacerTab {

    private static final String[] RULE_TYPES = {
            "Header", "Cookie", "URL Parameter", "POST Body Parameter"
    };
    private static final Set<String> VALID_TYPES = Set.of(RULE_TYPES);

    private static final String RULES_CARD = "rules";
    private static final String JSON_CARD = "json";

    private final JPanel cardPanel;
    private final CardLayout cardLayout;
    private final JPanel rulesContainer;
    private final JTextArea jsonTextArea;
    private final List<RulePanel> rulePanels = new ArrayList<>();

    public ReplacerTab(MontoyaApi api) {
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        jsonTextArea = new JTextArea();
        jsonTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        // --- Rules View ---
        JPanel rulesView = new JPanel(new BorderLayout());

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("+");
        addButton.addActionListener(e -> addRulePanel());
        topBar.add(addButton);

        JButton saveLoadButton = new JButton("Save / Load Rules");
        saveLoadButton.addActionListener(e -> {
            jsonTextArea.setText("");
            cardLayout.show(cardPanel, JSON_CARD);
        });
        topBar.add(saveLoadButton);

        rulesContainer = new JPanel();
        rulesContainer.setLayout(new BoxLayout(rulesContainer, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(rulesContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        rulesView.add(topBar, BorderLayout.NORTH);
        rulesView.add(scrollPane, BorderLayout.CENTER);

        // --- JSON View ---
        JPanel jsonView = new JPanel(new BorderLayout());

        JPanel jsonTopBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> cardLayout.show(cardPanel, RULES_CARD));
        jsonTopBar.add(backButton);

        JScrollPane jsonScrollPane = new JScrollPane(jsonTextArea);

        JPanel jsonBottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton exportButton = new JButton("Export Rules");
        exportButton.addActionListener(e -> {
            String json = JsonUtil.exportRules(rulePanels);
            jsonTextArea.setText(json);
        });
        JButton loadButton = new JButton("Load Rules");
        loadButton.addActionListener(e -> loadRulesFromJson());
        jsonBottomBar.add(exportButton);
        jsonBottomBar.add(loadButton);

        jsonView.add(jsonTopBar, BorderLayout.NORTH);
        jsonView.add(jsonScrollPane, BorderLayout.CENTER);
        jsonView.add(jsonBottomBar, BorderLayout.SOUTH);

        // --- Card Panel ---
        cardPanel.add(rulesView, RULES_CARD);
        cardPanel.add(jsonView, JSON_CARD);
        cardLayout.show(cardPanel, RULES_CARD);

        api.userInterface().registerSuiteTab("Replacer", cardPanel);
    }

    private void loadRulesFromJson() {
        String json = jsonTextArea.getText().trim();
        if (json.isEmpty()) return;

        List<JsonUtil.RuleData> parsed;
        try {
            parsed = JsonUtil.parseRules(json);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(cardPanel,
                    "Invalid JSON. Please check the format and try again.",
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Clear existing rules
        rulePanels.clear();
        rulesContainer.removeAll();

        // Create rule panels from parsed data
        for (JsonUtil.RuleData ruleData : parsed) {
            RulePanel rulePanel = new RulePanel();
            rulePanel.nameField.setText(ruleData.name);

            // Remove the default empty type row
            rulePanel.typeRows.clear();
            rulePanel.typesListPanel.removeAll();

            for (JsonUtil.TypeData typeData : ruleData.typeRows) {
                if (typeData.type == null || !VALID_TYPES.contains(typeData.type)) {
                    continue;
                }
                rulePanel.addTypeRow(typeData.type, typeData.match, typeData.replace);
            }

            rulePanels.add(rulePanel);
            rulesContainer.add(rulePanel.getPanel());
        }

        rulesContainer.revalidate();
        rulesContainer.repaint();
        cardLayout.show(cardPanel, RULES_CARD);
    }

    public List<RulePanel> getRulePanels() {
        return Collections.unmodifiableList(rulePanels);
    }

    private void addRulePanel() {
        RulePanel rulePanel = new RulePanel();
        rulePanels.add(rulePanel);

        rulesContainer.add(rulePanel.getPanel());
        rulesContainer.revalidate();
        rulesContainer.repaint();
    }

    private void copyRulePanel(RulePanel source) {
        RulePanel copy = new RulePanel();
        copy.nameField.setText(source.getName() + "-copy");

        // Remove the default empty type row that RulePanel creates
        copy.typeRows.clear();
        copy.typesListPanel.removeAll();

        // Copy each type row from the source
        for (TypeRow srcRow : source.getTypeRows()) {
            copy.addTypeRow(srcRow.getType(), srcRow.getMatch(), srcRow.getReplace());
        }

        rulePanels.add(copy);
        rulesContainer.add(copy.getPanel());
        rulesContainer.revalidate();
        rulesContainer.repaint();
    }

    public static class TypeRow {
        private final JComboBox<String> typeCombo;
        private final JTextField matchField;
        private final JTextField replaceField;
        private final JPanel row;

        TypeRow(JPanel typesListPanel, JPanel rulePanel) {
            row = new JPanel(new GridBagLayout());
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(1, 3, 1, 3);
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0; gbc.weightx = 0;
            row.add(new JLabel("Type:"), gbc);
            gbc.gridx = 1; gbc.weightx = 0;
            typeCombo = new JComboBox<>(RULE_TYPES);
            row.add(typeCombo, gbc);

            gbc.gridx = 2; gbc.weightx = 0;
            row.add(new JLabel("Match:"), gbc);
            gbc.gridx = 3; gbc.weightx = 1;
            matchField = new JTextField();
            row.add(matchField, gbc);

            gbc.gridx = 4; gbc.weightx = 0;
            row.add(new JLabel("Replace:"), gbc);
            gbc.gridx = 5; gbc.weightx = 1;
            replaceField = new JTextField();
            row.add(replaceField, gbc);

            JButton removeTypeButton = new JButton("x");
            removeTypeButton.setMargin(new Insets(0, 3, 0, 3));
            removeTypeButton.addActionListener(e -> {
                if (typesListPanel.getComponentCount() > 1) {
                    typesListPanel.remove(row);
                    typesListPanel.revalidate();
                    typesListPanel.repaint();
                    rulePanel.revalidate();
                    rulePanel.repaint();
                }
            });
            gbc.gridx = 6; gbc.weightx = 0;
            row.add(removeTypeButton, gbc);

            typesListPanel.add(row);
            typesListPanel.revalidate();
        }

        public String getType() {
            return (String) typeCombo.getSelectedItem();
        }

        public String getMatch() {
            return matchField.getText();
        }

        public String getReplace() {
            return replaceField.getText();
        }

        public void setReplace(String value) {
            replaceField.setText(value);
        }

        JPanel getRow() {
            return row;
        }
    }

    public class RulePanel {
        private final JPanel panel;
        private final JTextField nameField;
        private final JPanel typesListPanel;
        private final List<TypeRow> typeRows = new ArrayList<>();

        RulePanel() {
            panel = new JPanel(new BorderLayout(5, 5));
            panel.setBorder(BorderFactory.createEtchedBorder());

            // Top row: Name field and remove button
            JPanel headerRow = new JPanel(new GridBagLayout());
            GridBagConstraints hgbc = new GridBagConstraints();
            hgbc.insets = new Insets(5, 5, 5, 5);
            hgbc.gridy = 0;
            hgbc.fill = GridBagConstraints.HORIZONTAL;

            hgbc.gridx = 0; hgbc.weightx = 0;
            headerRow.add(new JLabel("Name:"), hgbc);
            hgbc.gridx = 1; hgbc.weightx = 1;
            nameField = new JTextField();
            headerRow.add(nameField, hgbc);

            JButton removeButton = new JButton("X");
            removeButton.addActionListener(e -> {
                rulesContainer.remove(panel);
                rulePanels.remove(this);
                rulesContainer.revalidate();
                rulesContainer.repaint();
            });
            hgbc.gridx = 2; hgbc.weightx = 0;
            headerRow.add(removeButton, hgbc);
            panel.add(headerRow, BorderLayout.NORTH);

            // Types section
            JPanel typesWrapper = new JPanel(new BorderLayout(5, 0));

            typesListPanel = new JPanel();
            typesListPanel.setLayout(new BoxLayout(typesListPanel, BoxLayout.Y_AXIS));

            addTypeRow();

            JButton addTypeButton = new JButton("+ add new type");
            addTypeButton.setMargin(new Insets(2, 4, 2, 4));
            addTypeButton.addActionListener(e -> {
                addTypeRow();
                panel.revalidate();
                panel.repaint();
                rulesContainer.revalidate();
            });

            JButton copyRuleButton = new JButton("+ make a copy");
            copyRuleButton.setMargin(new Insets(2, 4, 2, 4));
            copyRuleButton.addActionListener(e -> copyRulePanel(this));

            JPanel buttonsWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 2));
            buttonsWrapper.add(addTypeButton);
            buttonsWrapper.add(copyRuleButton);

            typesWrapper.add(typesListPanel, BorderLayout.CENTER);
            typesWrapper.add(buttonsWrapper, BorderLayout.SOUTH);
            panel.add(typesWrapper, BorderLayout.CENTER);
        }

        private void addTypeRow() {
            TypeRow typeRow = new TypeRow(typesListPanel, panel);
            typeRows.add(typeRow);
        }

        private void addTypeRow(String type, String match, String replace) {
            TypeRow typeRow = new TypeRow(typesListPanel, panel);
            typeRow.typeCombo.setSelectedItem(type);
            typeRow.matchField.setText(match);
            typeRow.replaceField.setText(replace);
            typeRows.add(typeRow);
        }

        public String getName() {
            return nameField.getText();
        }

        public List<TypeRow> getTypeRows() {
            return typeRows;
        }

        JPanel getPanel() {
            return panel;
        }
    }
}
