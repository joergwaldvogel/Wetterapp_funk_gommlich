import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChartTest {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            //Chart chart = new WeatherChart();
            //chart.createAndShowGUI();
        });
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Wetterdaten Stabdiagramm");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(createChartPanel());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createChartPanel() {
        JFreeChart chart = ChartFactory.createBarChart(
                "Durchschnittliche Temperaturen",
                "Jahr",
                "Temperatur (°C)",
                loadData(),
                org.jfree.chart.plot.PlotOrientation.VERTICAL,
                true, true, false);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setItemMargin(0.1);

        return new ChartPanel(chart);
    }

    private DefaultCategoryDataset loadData() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String filePath = "weather_data.txt";  // Datei-Pfad anpassen
        Pattern pattern = Pattern.compile("Jahr: (\\d+)\\nDurchschnittliche Mindesttemperatur: ([0-9NaN.]+)°C\\nDurchschnittliche Höchsttemperatur: ([0-9NaN.]+)°C");

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String content = br.lines().reduce("", (a, b) -> a + "\n" + b);
            Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                String year = matcher.group(1);
                String minTemp = matcher.group(2);
                String maxTemp = matcher.group(3);

                if (!minTemp.equals("NaN")) {
                    dataset.addValue(Double.parseDouble(minTemp), "Min Temperatur", year);
                }
                if (!maxTemp.equals("NaN")) {
                    dataset.addValue(Double.parseDouble(maxTemp), "Max Temperatur", year);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dataset;
    }
}
