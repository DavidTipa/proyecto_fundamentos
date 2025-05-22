import java.io.*;
import java.util.*;

public class DLVPlanVisualizer {
    public static void main(String[] args) {
        String dlvPath = "C:\\proyecto_fundamentos\\interfaz\\dlv.exe";
        String backgroundFile = "C:\\proyecto_fundamentos\\interfaz\\proyect.dl";
        String planFile = "C:\\proyecto_fundamentos\\interfaz\\lc.plan";

        if (!checkFileExists(dlvPath, "DLV") || 
            !checkFileExists(backgroundFile, "Background") || 
            !checkFileExists(planFile, "Plan")) {
            return;
        }

        try {
            String command = String.format("\"%s\" -FP \"%s\" \"%s\"", dlvPath, backgroundFile, planFile);
            System.out.println("Ejecutando: " + command);

            System.out.println("\n╔══════════════════════════════════════╗");
            System.out.println("║      PLANES GENERADOS POR DLV        ║");
            System.out.println("╚══════════════════════════════════════╝\n");

            Process process = Runtime.getRuntime().exec(command);

            // Leer salida estándar
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                    parseFSMOutput(reader);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            // Leer errores
            Thread errorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println("DLV ERROR: " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            outputThread.start();
            errorThread.start();

            int exitCode = process.waitFor();
            outputThread.join();
            errorThread.join();

            System.out.println("\n🔚 DLV finalizó con código: " + exitCode);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean checkFileExists(String path, String fileType) {
        File file = new File(path);
        if (!file.exists()) {
            System.err.println("❌ No se encuentra el archivo " + fileType + " en: " + path);
            System.err.println("📁 Directorio actual: " + System.getProperty("user.dir"));
            return false;
        }
        return true;
    }

  private static void parseFSMOutput(BufferedReader reader) throws IOException {
    String line;
    int stateNumber = -1;
    List<String> planActions = new ArrayList<>();

    while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty()) continue;

        if (line.startsWith("STATE")) {
            // Muestra el estado completo con sus literales
            stateNumber++;
            String estado = line.substring(line.indexOf(":") + 1).trim();
            System.out.println("════════════════════════════════════════");
            System.out.println("ESTADO: " + stateNumber + ": " + estado);
        } else if (line.startsWith("ACTIONS:")) {
            String actions = line.replace("ACTIONS:", "").trim();
            String[] acts = actions.split(",\\s*");
            System.out.println("ACTIONS:");
            for (String act : acts) {
                System.out.println("  " + act);
            }
        } else if (line.startsWith("PLAN:")) {
            System.out.println("\n  PLAN FINAL:");
            String plan = line.replace("PLAN:", "").trim();
            String[] steps = plan.split(";\\s*|,\\s*");
            int stepNumber = 1;
            for (String step : steps) {
                System.out.println("  " + stepNumber++ + ". " + step.trim());
                planActions.add(step.trim());
            }
        } else {
            // Cualquier otra línea se muestra como información adicional
            System.out.println("" + line);
        }
    }

    if (stateNumber == -1 && planActions.isEmpty()) {
        System.out.println(" No se encontraron estados ni planes.");
    }
}

}
