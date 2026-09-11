package deepfake.ai;

public class Main {
    public static void main(String[] args) {
        try {
            // This line tries to find TarsosDSP's main class inside your project.
            // If Maven downloaded everything correctly, this succeeds instantly.
            Class.forName("be.tarsos.dsp.AudioDispatcher");
            System.out.println("TarsosDSP loaded successfully!");
        } catch (ClassNotFoundException e) {
            System.out.println("TarsosDSP did NOT load. Something is missing.");
            e.printStackTrace();
        }
    }
}