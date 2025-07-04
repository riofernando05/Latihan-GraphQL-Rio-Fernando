package com.mycompany.graphqlproductcrud;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ProductForm extends JFrame {
    private JTextField tfId = new JTextField();
    private JTextField tfName = new JTextField();
    private JTextField tfPrice = new JTextField();
    private JTextField tfCategory = new JTextField();

    private JTable table = new JTable();
    private DefaultTableModel tableModel = new DefaultTableModel(new String[]{"ID", "Nama", "Harga", "Kategori"}, 0);

    public ProductForm() {
        setTitle("GraphQL Product Form");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        table.setModel(tableModel);
        JScrollPane tableScroll = new JScrollPane(table);

        JPanel inputPanel = new JPanel(new GridLayout(5, 2));
        inputPanel.add(new JLabel("ID:"));
        inputPanel.add(tfId);
        inputPanel.add(new JLabel("Nama:"));
        inputPanel.add(tfName);
        inputPanel.add(new JLabel("Harga:"));
        inputPanel.add(tfPrice);
        inputPanel.add(new JLabel("Kategori:"));
        inputPanel.add(tfCategory);

        JButton btnAdd = new JButton("Tambah");
        JButton btnUpdate = new JButton("Ubah");
        JButton btnDelete = new JButton("Hapus");
        JButton btnShowAll = new JButton("Tampilkan Semua");

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnShowAll);

        add(inputPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        add(tableScroll, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> tambahProduk());
        btnUpdate.addActionListener(e -> updateProduk());
        btnDelete.addActionListener(e -> hapusProduk());
        btnShowAll.addActionListener(e -> tampilkanSemuaProduk());

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void tambahProduk() {
        if (tfName.getText().isEmpty() || tfPrice.getText().isEmpty() || tfCategory.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Harap isi Nama, Harga, dan Kategori.");
            return;
        }

        try {
            double price = Double.parseDouble(tfPrice.getText());

            String query = String.format(
                "mutation { addProduct(name: \"%s\", price: %s, category: \"%s\") { id name } }",
                tfName.getText(), price, tfCategory.getText()
            );
            kirimPermintaanDanRefresh(query, "Produk berhasil ditambahkan!");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Format harga tidak valid.");
        }
    }

    private void updateProduk() {
        if (tfId.getText().isEmpty() || tfName.getText().isEmpty() || tfPrice.getText().isEmpty() || tfCategory.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Harap isi ID, Nama, Harga, dan Kategori.");
            return;
        }

        try {
            long id = Long.parseLong(tfId.getText());
            double price = Double.parseDouble(tfPrice.getText());

            String query = String.format(
                "mutation { updateProduct(id: %d, name: \"%s\", price: %s, category: \"%s\") { id name price category } }",
                id, tfName.getText(), price, tfCategory.getText()
            );
            kirimPermintaanDanRefresh(query, "Produk berhasil diubah!");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Format ID atau harga tidak valid.");
        }
    }

    private void hapusProduk() {
        if (tfId.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Harap isi ID untuk menghapus produk.");
            return;
        }

        try {
            long id = Long.parseLong(tfId.getText());
            String query = String.format("mutation { deleteProduct(id: %d) }", id);
            String response = kirimPermintaan(prepareJsonQuery(query));

            if (hasErrors(response)) {
                showError(response);
            } else {
                boolean success = JsonParser.parseString(response)
                        .getAsJsonObject()
                        .getAsJsonObject("data")
                        .get("deleteProduct").getAsBoolean();

                if (success) {
                    JOptionPane.showMessageDialog(this, "Produk berhasil dihapus!");
                    tampilkanSemuaProduk();
                } else {
                    JOptionPane.showMessageDialog(this, "Produk tidak ditemukan.");
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Format ID tidak valid.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Terjadi kesalahan: " + e.getMessage());
        }
    }

    private void tampilkanSemuaProduk() {
        String query = "{ allProducts { id name price category } }";
        try {
            String response = kirimPermintaan(prepareJsonQuery(query));
            if (hasErrors(response)) {
                showError(response);
            } else {
                updateTableFromResponse(response);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Terjadi kesalahan: " + e.getMessage());
        }
    }

    private void kirimPermintaanDanRefresh(String query, String successMessage) {
        try {
            String response = kirimPermintaan(prepareJsonQuery(query));
            if (hasErrors(response)) {
                showError(response);
            } else {
                JOptionPane.showMessageDialog(this, successMessage);
                tampilkanSemuaProduk();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Terjadi kesalahan: " + e.getMessage());
        }
    }

    private String prepareJsonQuery(String query) {
        JsonObject json = new JsonObject();
        json.addProperty("query", query);
        return new Gson().toJson(json);
    }

    private boolean hasErrors(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return root.has("errors");
    }

    private void showError(String json) {
        JsonArray errors = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("errors");
        StringBuilder sb = new StringBuilder("Kesalahan GraphQL:\n");
        for (JsonElement e : errors) {
            sb.append("- ").append(e.getAsJsonObject().get("message").getAsString()).append("\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString());
    }

    private String kirimPermintaan(String json) throws Exception {
        URL url = new URL("http://localhost:4567/graphql");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
            os.flush();
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String output;
        while ((output = br.readLine()) != null) {
            sb.append(output);
        }

        conn.disconnect();
        return sb.toString();
    }

    private void updateTableFromResponse(String rawJson) {
        tableModel.setRowCount(0); // clear old rows

        JsonObject data = JsonParser.parseString(rawJson).getAsJsonObject().getAsJsonObject("data");
        if (data == null || !data.has("allProducts")) return;

        JsonArray products = data.getAsJsonArray("allProducts");
        for (JsonElement element : products) {
            JsonObject p = element.getAsJsonObject();
            Object[] row = new Object[]{
                p.get("id").getAsString(),
                p.get("name").getAsString(),
                p.get("price").getAsDouble(),
                p.get("category").getAsString()
            };
            tableModel.addRow(row);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ProductForm::new);
    }
}
