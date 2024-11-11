package com.monstermonitor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * A search bar component for filtering NPCs in Monster Monitor.
 * Displays a text field with a search icon, triggering real-time filtering through the `SearchListener`.
 */
public class MonsterMonitorSearchBar extends JPanel {
    private final JTextField searchField;

    public interface SearchListener {
        void onSearch(String searchText);
    }

    public MonsterMonitorSearchBar(SearchListener listener) {
        setLayout(new BorderLayout());
        setBackground(new Color(45, 45, 45));
        setBorder(new EmptyBorder(5, 5, 5, 5));

        // Create a panel to hold the icon and search field together
        JPanel searchContainer = new JPanel(new BorderLayout());
        searchContainer.setBackground(new Color(30, 30, 30)); // Match search field background
        searchContainer.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5)); // Padding

        // Search icon
        JLabel searchIcon = new JLabel("🔍"); //Magnifying glass
        searchIcon.setForeground(new Color(200, 200, 200));
        searchIcon.setBorder(new EmptyBorder(0, 5, 0, 5));

        // Search field
        searchField = new JTextField();
        searchField.setBackground(new Color(30, 30, 30)); // Dark background for visibility
        searchField.setForeground(Color.LIGHT_GRAY);
        searchField.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5)); // Inner padding
        searchField.setCaretColor(Color.LIGHT_GRAY);

        // Listen for key releases to trigger the search
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                SwingUtilities.invokeLater(() -> listener.onSearch(searchField.getText()));
            }
        });

        // Add icon and search field to the container panel
        searchContainer.add(searchIcon, BorderLayout.WEST);
        searchContainer.add(searchField, BorderLayout.CENTER);

        // Add the container panel to this search bar panel
        add(searchContainer, BorderLayout.CENTER);
    }
}
