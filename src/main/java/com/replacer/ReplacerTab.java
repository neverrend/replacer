package com.replacer;

import burp.api.montoya.MontoyaApi;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ReplacerTab {

    private static final String[] RULE_TYPES = {
            "Header", "Cookie", "URL Parameter", "POST Body Parameter"
    };

    private final JPanel mainPanel;
    private final JPanel rulesContainer;
    private final List<RulePanel> rulePanels = new ArrayList<>();

    public ReplacerTab(MontoyaApi api) {
        mainPanel = new JPanel(new BorderLayout());

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("+");
        addButton.addActionListener(e -> addRulePanel());
        topBar.add(addButton);

        rulesContainer = new JPanel();
        rulesContainer.setLayout(new BoxLayout(rulesContainer, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(rulesContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        mainPanel.add(topBar, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        api.userInterface().registerSuiteTab("Replacer", mainPanel);
    }

    public List<RulePanel> getRulePanels() {
        return rulePanels;
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
