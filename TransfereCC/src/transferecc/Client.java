package transferecc;

import agenteudp.PDUTypes;
import agenteudp.Receiver;
import agenteudp.Sender;
import agenteudp.management.FileID;

import java.util.InputMismatchException;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        if (args.length < 1)
            System.out.println(Menu.insuficientArguments(args.length));
        else {

            //Configuration config = new Configuration(args[0]);

            //chooseDestinationIP(config);

            Receiver r = new Receiver(5555);
            new Thread(r).start();
            Sender s = new Sender(5555);
            FileID p = new FileID(123,PDUTypes.M_FILE,"~/hello");
            s.sendDatagram(p,"localhost");

        }
    }

    private static void chooseDestinationIP(Configuration config) {
        System.out.println(Menu.welcomeMenu(config.getIPS()));
        int numIps = config.numIps();
        int chosen = getInt(0,numIps+1);
        if (chosen == 0)
            addNewIP(config);
        else {
            config.getIP(chosen-1);
        }

    }

    private static void addNewIP(Configuration config) {

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


    public void nextAction(int acao, Configuration config){
        int nIps= config.numIps();
        Scanner in = new Scanner(System.in);
        int res=in.nextInt();
        if(res>0 && res<nIps){
            //começar ligação com endereço escolhido
        }
        else{
            switch(res){
                case 0:



            }
        }

    }
}
