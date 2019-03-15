package transferecc;

import java.util.InputMismatchException;
import java.util.Scanner;

/**
 *
 * @author alvesluis
 */
public class Client {
    public static void main(String[] args) {
        if (args.length < 1)
            System.out.println(Menu.insuficientArguments(args.length));
        else {
            Configuration config = new Configuration(args[0]);
        
            System.out.println(Menu.welcomeMenu(config.getIPS()));
            
            
            
        }
    }
    
    private static String getString(){
        Scanner in = new Scanner(System.in); 
        return in.nextLine();
    }
    
    private static int getInt(int defaultValue){
        Scanner in = new Scanner(System.in);
        int result = defaultValue;
        int numTries = 3;
        
        while (numTries > 0)
            try {
                result = in.nextInt();
                in.nextLine();
                numTries = 0;
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
