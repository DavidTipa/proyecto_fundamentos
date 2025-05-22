import java.io.*;
import java.util.*;

public class DLVPlanVisualizer2 {

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

            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                    parseFSMOutput(reader);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

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

            System.out.println("\nDLV finalizó con código: " + exitCode);

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
        boolean hasPlan = false;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("STATE")) {
                stateNumber++;
                String estado = line.substring(line.indexOf(":") + 1).trim();

                System.out.println("\n┌───────────────────────────────────────────────────┐");
                System.out.printf("│ ESTADO %-12d%-30s │\n", stateNumber, "");
                System.out.println("├───────────────────────────────────────────────────┤");

                String[] literales = estado.split(",\\s*");
                for (String literal : literales) {
                    System.out.printf("│ %-47s │\n", literal);
                }
                System.out.println("└───────────────────────────────────────────────────┘");

            } else if (line.startsWith("ACTIONS:")) {
                String actions = line.replace("ACTIONS:", "").trim();

                System.out.println("\n  ┌─────────────────────────────────────────────┐");
                System.out.println("  │                   ACCIONES                   │");
                System.out.println("  ├─────────────────────────────────────────────┤");

                if (actions.equals("(no action)")) {
                    System.out.println("  │ No hay acciones disponibles en este estado │");
                } else {
                    String[] acts = actions.split(",\\s*");
                    for (String act : acts) {
                        System.out.printf("  │  %-40s │\n", act.trim());
                    }
                }
                System.out.println("  └─────────────────────────────────────────────┘");

            } else if (line.startsWith("PLAN:")) {
                hasPlan = true;
                String plan = line.replace("PLAN:", "").trim();

                System.out.println("\n╔═════════════════════════════════════════════════╗");
                System.out.println("║                  PLAN FINAL                     ║");
                System.out.println("╠═════════════════════════════════════════════════╣");

                String[] steps = plan.split(";\\s*|,\\s*");
                int stepNumber = 1;
                for (String step : steps) {
                    if (!step.trim().isEmpty() && !step.trim().equals("(no action)")) {
                        System.out.printf("║ %2d. %-42s ║\n", stepNumber++, step.trim());
                        planActions.add(step.trim());
                    }
                }
                System.out.println("╚═════════════════════════════════════════════════╝");

            } else if (!line.contains("DLV [build") && !line.trim().isEmpty()) {
                System.out.println("  ══> " + line);
            }
        }

        if (stateNumber == -1 && !hasPlan) {
            System.out.println("\n No se encontraron estados ni planes.");
        }
    }
}