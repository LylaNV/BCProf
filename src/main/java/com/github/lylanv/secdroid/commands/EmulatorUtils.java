package com.github.lylanv.secdroid.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class EmulatorUtils {

    private static String emulatorPath = "/Users/lylan/UiO/android/emulator";

    public static List<String> getAvailableAVDs() {
        List<String> availableAVDs = new ArrayList<>();

        try {

            ProcessBuilder pb = new ProcessBuilder(emulatorPath, "-list-avds");
            Process pbProcess = pb.start();

            if (pbProcess != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(pbProcess.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    String avdName = line.trim();
                    availableAVDs.add(avdName);
                }

                return availableAVDs;

            } else {
                System.out.println("[EmulatorUtils -> getAvailableAVDs$ Failed to get available AVDs");
                return null;
            }

        } catch (Exception e){
            e.printStackTrace();
            System.out.println("[EmulatorUtils -> getAvailableAVDs$ Exception - Failed to get available AVDs");
            return null;
        }

    }

    public static void startAVD(String avdName) {
        List<String> startedAVDs = new ArrayList<>();

        try {

            ProcessBuilder pb = new ProcessBuilder(emulatorPath, "-avd", avdName);
            Process pbProcess = pb.start();

        } catch (Exception e){
            e.printStackTrace();
            System.out.println("[EmulatorUtils -> startAVD$ Exception - Failed to start the AVD.");
        }

    }
}
