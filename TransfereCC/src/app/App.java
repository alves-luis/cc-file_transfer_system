package app;

import transferecc.Client;
import transferecc.Server;

import java.time.Instant;
import java.util.InputMismatchException;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        if (args.length < 1)
            System.out.println("Insuficient arguments!");
        else {
            if (args[0].equals("client")) {
                if (args.length < 4) {
                    System.out.println(Menu.insuficientArguments(args.length));
                }
                else {
                    long start = System.nanoTime();
                    Client c = new Client(args[3], 7777);

                    boolean success = c.startConnection();
                    System.out.println("Started connection? " + success);
                    if (success) {
                        if (args[1].equals("GET")) {
                            success = c.requestFile(args[2]);
                            System.out.println("File requested? " + success);
                        }
                        else if (args[1].equals("PUT")) {
                            success = c.sendFile(args[2]);
                            System.out.println("File uploaded? " + success);
                        }
                    }
                    if (success) {
                        long end = System.nanoTime();
                        success = c.endConnection();
                        System.out.println("Ended connection? " + success);
                        System.out.println("Finished download in " + ((end - start) / 1000000) + "ms");
                    }
                }

            }
            if (args[0].equals("server")) {
                Server s = new Server();
                new Thread(s).start();
                boolean serverSuccess = true;
                while(serverSuccess) {
                    serverSuccess = s.receiveConnectionRequest();
                    System.out.println("Transfer with success? " + serverSuccess);
                }
            }

        }
    }

    private static String getString(){
        Scanner in = new Scanner(System.in);
        return in.nextLine();
    }

    /**
     * Gets an int from the stdin.
     * @param defaultValue default value, which after 3 tries is returned
     * @param maxValue max value to be chosen
     * @return value
     */
    private static int getInt(int defaultValue, int maxValue){
        Scanner in = new Scanner(System.in);
        int result = defaultValue;
        int numTries = 3;

        while (numTries > 0)
            try {
                result = in.nextInt();
                in.nextLine();
                if (result < maxValue && result >= 0)
                    numTries = 0;
                else {
                    numTries--;
                    System.err.println("Index out of bounds! Try again!");
                }
            }
            catch(InputMismatchException e) {
                System.err.println("Was expecting an int! Try again!");
                in.nextLine();
                numTries--;
            }
        return result;
    }
}
